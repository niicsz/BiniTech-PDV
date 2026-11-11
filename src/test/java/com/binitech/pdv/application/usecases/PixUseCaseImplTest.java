package com.binitech.pdv.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.inbound.PixUseCasePort.Notification;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort.Operator;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort.OrderCharge;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort.SettingsUpdate;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.DeliveryOrderRepositoryPort;
import com.binitech.pdv.application.ports.outbound.PixChargeRepositoryPort;
import com.binitech.pdv.application.ports.outbound.PixGatewayPort;
import com.binitech.pdv.application.ports.outbound.PixGatewayPort.ChargeState;
import com.binitech.pdv.application.ports.outbound.PixSettingsRepositoryPort;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.PixSettings;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.PixChargeStatus;
import com.binitech.pdv.utils.enums.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PixUseCaseImplTest {

  private static final Operator OPERATOR = new Operator("u1", "t1", Role.OPERATOR);
  private static final Operator ADMIN = new Operator("u9", "t1", Role.TENANT_ADMIN);
  private static final Instant FIXED = Instant.parse("2026-11-08T15:00:00Z");
  private static final LocalDateTime NOW = LocalDateTime.ofInstant(FIXED, ZoneId.systemDefault());
  private static final PixSettings ACTIVE =
      new PixSettings("t1", true, "APP_USR-token-1234", null, "Loja", LocalDateTime.now());

  @Mock private PixSettingsRepositoryPort settings;
  @Mock private PixChargeRepositoryPort charges;
  @Mock private PixGatewayPort gateway;
  @Mock private SaleUseCasePort sales;
  @Mock private DeliveryOrderRepositoryPort deliveryOrders;

  private PixUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = build(true, "https://api.example.com");
  }

  private PixUseCaseImpl build(boolean enabled, String baseUrl) {
    return new PixUseCaseImpl(
        new PixUseCaseImpl.Options(
            enabled,
            Duration.ofMinutes(15),
            Duration.ofSeconds(3),
            baseUrl,
            Clock.fixed(FIXED, ZoneOffset.UTC)),
        settings,
        charges,
        gateway,
        sales,
        deliveryOrders);
  }

  private static Sale request(double pix, double cash) {
    Sale sale = new Sale();
    SaleItem item = new SaleItem();
    item.setProductId("p1");
    item.setQuantity(2);
    sale.setItems(new ArrayList<>(List.of(item)));
    List<Payment> payments = new ArrayList<>();
    if (pix > 0) payments.add(new Payment(PaymentMethod.PIX, pix));
    if (cash > 0) payments.add(new Payment(PaymentMethod.CASH, cash));
    sale.setPayments(payments);
    sale.setDiscount(1.5);
    return sale;
  }

  private static PixCharge pending() {
    PixCharge charge = new PixCharge();
    charge.setId("c1");
    charge.setTenantId("t1");
    charge.setUserId("u1");
    charge.setProviderPaymentId("998877");
    charge.setAmount(18.5);
    charge.setStatus(PixChargeStatus.PENDING);
    charge.setCreatedAt(NOW.minusMinutes(1));
    charge.setExpiresAt(NOW.plusMinutes(14));
    charge.setSaleRequest(request(18.5, 0));
    return charge;
  }

  @Test
  @DisplayName("Com o Pix integrado desligado no servidor tudo responde 404")
  void disabledFlagHidesEverything() {
    PixUseCaseImpl off = build(false, null);
    assertThatThrownBy(() -> off.getSettings(OPERATOR))
        .isInstanceOf(ResourceNotFoundException.class);
    Sale sale = request(10, 0);
    assertThatThrownBy(() -> off.createCharge(OPERATOR, sale))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("Operador não altera a configuração do Pix")
  void operatorCannotChangeSettings() {
    SettingsUpdate update = new SettingsUpdate(true, "tok", null);
    assertThatThrownBy(() -> useCase.updateSettings(OPERATOR, update))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("Salvar um token novo valida a conta no Mercado Pago e guarda o nome")
  void savingTokenVerifiesAccount() {
    when(settings.findByTenantId("t1")).thenReturn(Optional.empty());
    when(gateway.verifyAccount("APP_USR-novo")).thenReturn("MERCADINHO");
    when(settings.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PixSettings saved =
        useCase.updateSettings(ADMIN, new SettingsUpdate(true, "  APP_USR-novo ", "segredo"));

    assertThat(saved.enabled()).isTrue();
    assertThat(saved.accessToken()).isEqualTo("APP_USR-novo");
    assertThat(saved.accountName()).isEqualTo("MERCADINHO");
    assertThat(saved.webhookSecret()).isEqualTo("segredo");
    assertThat(saved.accessTokenHint()).isEqualTo("••••novo");
  }

  @Test
  @DisplayName("Não liga o Pix sem Access Token")
  void enablingRequiresToken() {
    when(settings.findByTenantId("t1")).thenReturn(Optional.empty());

    SettingsUpdate update = new SettingsUpdate(true, null, null);
    assertThatThrownBy(() -> useCase.updateSettings(ADMIN, update))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Access Token");
  }

  @Test
  @DisplayName("Gera o QR só do valor em Pix, valida a venda antes e não grava venda nenhuma")
  void createsChargeForPixAmountOnly() {
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.createCharge(eq("APP_USR-token-1234"), any()))
        .thenReturn(new PixGatewayPort.CreatedCharge("998877", "000201pix", "iVBOR"));
    when(charges.insert(any())).thenAnswer(inv -> inv.getArgument(0));

    PixCharge charge = useCase.createCharge(OPERATOR, request(12.0, 6.5));

    ArgumentCaptor<PixGatewayPort.ChargeRequest> sent =
        ArgumentCaptor.forClass(PixGatewayPort.ChargeRequest.class);
    verify(gateway).createCharge(anyString(), sent.capture());
    assertThat(sent.getValue().amount()).isEqualByComparingTo("12.0");
    assertThat(sent.getValue().notificationUrl())
        .isEqualTo("https://api.example.com/webhooks/mercadopago/pix?charge=" + charge.getId());
    assertThat(sent.getValue().idempotencyKey()).isEqualTo(charge.getId());
    assertThat(charge.getStatus()).isEqualTo(PixChargeStatus.PENDING);
    assertThat(charge.getQrCode()).isEqualTo("000201pix");
    assertThat(charge.getSaleRequest().getPayments()).hasSize(2);
    assertThat(charge.getSaleRequest().getDiscount()).isEqualTo(1.5);
    verify(sales).previewSale(any(), eq("t1"));
    verify(sales, never()).createSale(any(), anyString(), anyString());
  }

  @Test
  @DisplayName("Sem URL pública https o QR sai sem notificação e a baixa fica pela consulta")
  void noNotificationUrlWithoutHttpsBase() {
    PixUseCaseImpl local = build(true, "http://localhost:8080");
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.createCharge(anyString(), any()))
        .thenReturn(new PixGatewayPort.CreatedCharge("1", "qr", "b64"));
    when(charges.insert(any())).thenAnswer(inv -> inv.getArgument(0));

    local.createCharge(OPERATOR, request(5, 0));

    ArgumentCaptor<PixGatewayPort.ChargeRequest> sent =
        ArgumentCaptor.forClass(PixGatewayPort.ChargeRequest.class);
    verify(gateway).createCharge(anyString(), sent.capture());
    assertThat(sent.getValue().notificationUrl()).isNull();
  }

  @Test
  @DisplayName("Venda sem pagamento em Pix ou com Pix desligado na loja é recusada")
  void rejectsWithoutPixAmountOrInactiveStore() {
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    Sale cashOnly = request(0, 10);
    assertThatThrownBy(() -> useCase.createCharge(OPERATOR, cashOnly))
        .isInstanceOf(BusinessException.class);

    when(settings.findByTenantId("t1"))
        .thenReturn(Optional.of(new PixSettings("t1", false, "tok", null, null, null)));
    Sale pixSale = request(10, 0);
    assertThatThrownBy(() -> useCase.createCharge(OPERATOR, pixSale))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não está ligado");
    verify(gateway, never()).createCharge(anyString(), any());
  }

  @Test
  @DisplayName(
      "Pagamento aprovado registra a venda uma vez, mesmo sem estoque, no nome do operador")
  void approvedPaymentCreatesSale() {
    PixCharge charge = pending();
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(charge));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState("APP_USR-token-1234", "998877")).thenReturn(ChargeState.APPROVED);
    when(charges.transition("c1", PixChargeStatus.PENDING, PixChargeStatus.PROCESSING))
        .thenReturn(true);
    Sale created = new Sale();
    created.setId("v1");
    when(sales.createSale(any(), eq("u1"), eq("t1"))).thenReturn(created);
    when(charges.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PixCharge result = useCase.getCharge(OPERATOR, "c1");

    ArgumentCaptor<Sale> sale = ArgumentCaptor.forClass(Sale.class);
    verify(sales).createSale(sale.capture(), eq("u1"), eq("t1"));
    assertThat(sale.getValue().isSkipStockValidation()).isTrue();
    assertThat(result.getStatus()).isEqualTo(PixChargeStatus.PAID);
    assertThat(result.getSaleId()).isEqualTo("v1");
  }

  @Test
  @DisplayName("Se outra consulta já confirmou o Pix, não registra a venda de novo")
  void concurrentConfirmationCreatesNoDuplicate() {
    PixCharge charge = pending();
    PixCharge done = pending();
    done.setStatus(PixChargeStatus.PAID);
    done.setSaleId("v1");
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(charge));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState(anyString(), anyString())).thenReturn(ChargeState.APPROVED);
    when(charges.transition("c1", PixChargeStatus.PENDING, PixChargeStatus.PROCESSING))
        .thenReturn(false);
    when(charges.findById("c1")).thenReturn(Optional.of(done));

    PixCharge result = useCase.getCharge(OPERATOR, "c1");

    assertThat(result.getSaleId()).isEqualTo("v1");
    verify(sales, never()).createSale(any(), anyString(), anyString());
  }

  @Test
  @DisplayName("Se a venda falhar depois do Pix pago, a cobrança fica paga com o motivo")
  void paidButSaleFailedKeepsReason() {
    PixCharge charge = pending();
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(charge));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState(anyString(), anyString())).thenReturn(ChargeState.APPROVED);
    when(charges.transition(anyString(), any(), any())).thenReturn(true);
    when(sales.createSale(any(), anyString(), anyString()))
        .thenThrow(new BusinessException("Produto inativo: Coca"));
    when(charges.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PixCharge result = useCase.getCharge(OPERATOR, "c1");

    assertThat(result.getStatus()).isEqualTo(PixChargeStatus.PAID);
    assertThat(result.getSaleId()).isNull();
    assertThat(result.getFailureReason()).contains("Pix foi recebido").contains("Produto inativo");
  }

  @Test
  @DisplayName("Consulta recente não chama o Mercado Pago de novo")
  void throttlesStatusChecks() {
    PixCharge charge = pending();
    charge.setLastCheckedAt(NOW.minusSeconds(1));
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(charge));

    useCase.getCharge(OPERATOR, "c1");

    verify(gateway, never()).fetchState(anyString(), anyString());
  }

  @Test
  @DisplayName("QR vencido e ainda pendente é cancelado no provedor e expira")
  void expiresLateCharge() {
    PixCharge charge = pending();
    charge.setExpiresAt(LocalDateTime.of(2020, 1, 1, 0, 0));
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(charge));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState(anyString(), anyString())).thenReturn(ChargeState.PENDING);
    when(charges.transition("c1", PixChargeStatus.PENDING, PixChargeStatus.EXPIRED))
        .thenReturn(true);

    PixCharge result = useCase.getCharge(OPERATOR, "c1");

    assertThat(result.getStatus()).isEqualTo(PixChargeStatus.EXPIRED);
    verify(gateway).cancel("APP_USR-token-1234", "998877");
  }

  @Test
  @DisplayName("Cancelar confere antes se o cliente já pagou")
  void cancelChecksPaymentFirst() {
    PixCharge charge = pending();
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(charge));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState(anyString(), anyString())).thenReturn(ChargeState.PENDING);
    when(charges.transition("c1", PixChargeStatus.PENDING, PixChargeStatus.CANCELLED))
        .thenReturn(true);

    PixCharge result = useCase.cancelCharge(OPERATOR, "c1");

    assertThat(result.getStatus()).isEqualTo(PixChargeStatus.CANCELLED);
    verify(gateway).cancel(anyString(), eq("998877"));
  }

  @Test
  @DisplayName("Notificação com assinatura inválida é ignorada")
  void ignoresBadSignature() {
    PixCharge charge = pending();
    when(charges.findById("c1")).thenReturn(Optional.of(charge));
    when(settings.findByTenantId("t1"))
        .thenReturn(Optional.of(new PixSettings("t1", true, "tok", "segredo", null, null)));
    when(gateway.isValidSignature("segredo", "998877", "req", "ts=1,v1=00")).thenReturn(false);

    useCase.handleNotification(new Notification("c1", "998877", "req", "ts=1,v1=00"));

    verify(gateway, never()).fetchState(anyString(), anyString());
  }

  @Test
  @DisplayName("Notificação de outro pagamento não mexe na cobrança")
  void ignoresOtherPayment() {
    when(charges.findById("c1")).thenReturn(Optional.of(pending()));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));

    useCase.handleNotification(new Notification("c1", "111", null, null));

    verify(gateway, never()).fetchState(anyString(), anyString());
  }

  @Test
  @DisplayName("Notificação válida consulta o Mercado Pago e confirma a venda")
  void validNotificationConfirms() {
    when(charges.findById("c1")).thenReturn(Optional.of(pending()));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState(anyString(), eq("998877"))).thenReturn(ChargeState.APPROVED);
    when(charges.transition(anyString(), any(), any())).thenReturn(true);
    Sale created = new Sale();
    created.setId("v9");
    when(sales.createSale(any(), anyString(), anyString())).thenReturn(created);
    when(charges.save(any())).thenAnswer(inv -> inv.getArgument(0));

    useCase.handleNotification(new Notification("c1", "998877", null, null));

    verify(sales, times(1)).createSale(any(), eq("u1"), eq("t1"));
  }

  private static PixCharge orderCharge() {
    PixCharge charge = pending();
    charge.setUserId(null);
    charge.setSaleRequest(null);
    charge.setDeliveryOrderId("order-1");
    return charge;
  }

  @Test
  @DisplayName("Cobrança de pedido de delivery usa o total do pedido e não envolve venda")
  void createsOrderCharge() {
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.createCharge(anyString(), any()))
        .thenReturn(new PixGatewayPort.CreatedCharge("998877", "000201", "base64"));
    when(charges.insert(any())).thenAnswer(inv -> inv.getArgument(0));

    PixCharge charge =
        useCase.createOrderCharge(new OrderCharge("t1", "order-1", 33.0, "Pedido #7 - Loja"));

    assertThat(charge.getDeliveryOrderId()).isEqualTo("order-1");
    assertThat(charge.getAmount()).isEqualTo(33.0);
    assertThat(charge.getSaleRequest()).isNull();
    assertThat(charge.getQrCode()).isEqualTo("000201");
    verify(sales, never()).previewSale(any(), anyString());
  }

  @Test
  @DisplayName("Pix aprovado de pedido de delivery libera o pedido e não cria venda")
  void approvedOrderPaymentReleasesOrder() {
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(orderCharge()));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState(anyString(), anyString())).thenReturn(ChargeState.APPROVED);
    when(charges.transition(anyString(), any(), any())).thenReturn(true);
    when(deliveryOrders.confirmPayment(eq("order-1"), eq("t1"), any())).thenReturn(true);
    when(charges.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<PixCharge> result = useCase.checkOrderCharge("t1", "c1");

    assertThat(result).isPresent();
    assertThat(result.get().getStatus()).isEqualTo(PixChargeStatus.PAID);
    assertThat(result.get().getFailureReason()).isNull();
    verify(sales, never()).createSale(any(), any(), any());
  }

  @Test
  @DisplayName("Pix pago de pedido que não aguardava mais pagamento guarda o motivo")
  void paidOrderNoLongerAwaitingKeepsReason() {
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(orderCharge()));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState(anyString(), anyString())).thenReturn(ChargeState.APPROVED);
    when(charges.transition(anyString(), any(), any())).thenReturn(true);
    when(deliveryOrders.confirmPayment(eq("order-1"), eq("t1"), any())).thenReturn(false);
    when(charges.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<PixCharge> result = useCase.checkOrderCharge("t1", "c1");

    assertThat(result.get().getFailureReason()).contains("não estava mais aguardando");
  }

  @Test
  @DisplayName("Pix vencido de pedido de delivery cancela o pedido")
  void expiredOrderChargeCancelsOrder() {
    PixCharge charge = orderCharge();
    charge.setExpiresAt(LocalDateTime.of(2020, 1, 1, 0, 0));
    when(charges.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(charge));
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(gateway.fetchState(anyString(), anyString())).thenReturn(ChargeState.PENDING);
    when(charges.transition("c1", PixChargeStatus.PENDING, PixChargeStatus.EXPIRED))
        .thenReturn(true);

    Optional<PixCharge> result = useCase.checkOrderCharge("t1", "c1");

    assertThat(result.get().getStatus()).isEqualTo(PixChargeStatus.EXPIRED);
    verify(deliveryOrders)
        .changeStatus(
            "order-1", "t1", DeliveryOrderStatus.AWAITING_PAYMENT, DeliveryOrderStatus.CANCELLED);
  }

  @Test
  @DisplayName("Loja sem Pix integrado ativo não gera cobrança de pedido")
  void isActiveFollowsStoreSettings() {
    when(settings.findByTenantId("t1")).thenReturn(Optional.of(ACTIVE));
    when(settings.findByTenantId("t2")).thenReturn(Optional.empty());

    assertThat(useCase.isActive("t1")).isTrue();
    assertThat(useCase.isActive("t2")).isFalse();
    assertThat(build(false, null).isActive("t1")).isFalse();
  }
}
