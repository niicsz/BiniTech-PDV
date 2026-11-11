package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.OrderLine;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.OrderRequest;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.PlacedOrder;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.DeliveryOrderRepositoryPort;
import com.binitech.pdv.application.ports.outbound.DeliveryStoreRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.DeliveryAddress;
import com.binitech.pdv.domain.DeliveryOrder;
import com.binitech.pdv.domain.DeliveryOrderItem;
import com.binitech.pdv.domain.DeliveryStore;
import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.DeliveryFulfillment;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.PixChargeStatus;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeliveryUseCaseImplTest {

  private static final String TENANT = "tenant-1";
  private static final String SLUG = "mercadinho-bom-preco";

  @Mock private DeliveryStoreRepositoryPort stores;
  @Mock private DeliveryOrderRepositoryPort orders;
  @Mock private TenantRepositoryPort tenants;
  @Mock private ProductRepositoryPort products;
  @Mock private SaleUseCasePort sales;
  @Mock private PixUseCasePort pix;

  private DeliveryUseCaseImpl useCase;
  private DeliveryStore store;

  @BeforeEach
  void setUp() {
    useCase = create(true);
    Tenant tenant = new Tenant();
    tenant.setId(TENANT);
    tenant.setSlug(SLUG);
    tenant.setName("Mercadinho Bom Preço");
    tenant.setStatus(TenantStatus.ACTIVE);
    when(tenants.findBySlug(SLUG)).thenReturn(Optional.of(tenant));
    when(tenants.findById(TENANT)).thenReturn(Optional.of(tenant));
    store = DeliveryStore.defaults(TENANT);
    store.setEnabled(true);
    store.setOpen(true);
    store.setWhatsapp("5511912345678");
    store.setDeliveryFee(5);
    store.setMinimumOrder(15);
    store.setPixKey("pix@loja.com");
    when(stores.findByTenantId(TENANT)).thenReturn(Optional.of(store));
    when(stores.nextOrderNumber(TENANT)).thenReturn(7L);
    when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(products.findById("coca"))
        .thenReturn(Optional.of(product("coca", "Coca-Cola 2L", 12.5, 10)));
    when(products.findById("pao")).thenReturn(Optional.of(product("pao", "Pão francês", 1.0, 3)));
  }

  private DeliveryUseCaseImpl create(boolean enabled) {
    return new DeliveryUseCaseImpl(
        new DeliveryUseCaseImpl.Options(
            enabled,
            "https://www.binitechpdv.com.br/",
            Clock.fixed(Instant.parse("2026-11-03T15:00:00Z"), ZoneOffset.UTC)),
        stores,
        orders,
        tenants,
        products,
        sales,
        pix);
  }

  private static Product product(String id, String description, double price, int stock) {
    Product p = new Product();
    p.setId(id);
    p.setDescription(description);
    p.setPrice(price);
    p.setStockQuantity(stock);
    p.setTenantId(TENANT);
    p.setActive(true);
    p.setDeliveryVisible(true);
    return p;
  }

  private static OrderRequest request(
      DeliveryFulfillment fulfillment,
      DeliveryPaymentMethod method,
      Double changeFor,
      OrderLine... lines) {
    return new OrderRequest(
        "Ana Souza",
        "(11) 98888-7777",
        fulfillment,
        new DeliveryAddress("Rua das Flores", "120", "apto 3", "Centro", "perto da praça"),
        method,
        changeFor,
        null,
        List.of(lines));
  }

  @Test
  @DisplayName("Com a flag desligada, tudo responde como inexistente")
  void featureFlagOff() {
    DeliveryUseCaseImpl off = create(false);
    assertThrows(ResourceNotFoundException.class, () -> off.getPublicStore(SLUG));
    assertThrows(ResourceNotFoundException.class, () -> off.getSettings(TENANT));
    OrderRequest request1 =
        request(
            DeliveryFulfillment.PICKUP,
            DeliveryPaymentMethod.PIX,
            null,
            new OrderLine("coca", 2, null));
    assertThrows(ResourceNotFoundException.class, () -> off.placeOrder(SLUG, request1));
  }

  @Test
  @DisplayName(
      "Calcula o pedido com o preço do cadastro, soma a taxa e monta a mensagem do WhatsApp")
  void placesDeliveryOrder() {
    PlacedOrder placed =
        useCase.placeOrder(
            SLUG,
            request(
                DeliveryFulfillment.DELIVERY,
                DeliveryPaymentMethod.CASH,
                50.0,
                new OrderLine("coca", 2, "bem gelada"),
                new OrderLine("pao", 3, null)));

    DeliveryOrder order = placed.order();
    assertEquals(7L, order.getNumber());
    assertEquals(DeliveryOrderStatus.RECEIVED, order.getStatus());
    assertEquals("5511988887777", order.getCustomerPhone());
    assertEquals(28.0, order.getSubtotal());
    assertEquals(5.0, order.getDeliveryFee());
    assertEquals(33.0, order.getTotal());
    assertEquals(50.0, order.getChangeFor());
    assertTrue(placed.whatsappUrl().startsWith("https://wa.me/5511912345678?text="));
    String text = URLDecoder.decode(placed.whatsappUrl().split("text=")[1], StandardCharsets.UTF_8);
    assertTrue(text.contains("*Novo pedido #7* - Mercadinho Bom Preço"));
    assertTrue(text.contains("2x Coca-Cola 2L - R$ 25,00"));
    assertTrue(text.contains("_Obs.: bem gelada_"));
    assertTrue(text.contains("*Total: R$ 33,00*"));
    assertTrue(text.contains("Dinheiro (troco para R$ 50,00)"));
    assertTrue(text.contains("Rua das Flores, 120 - apto 3 - Centro"));
    assertTrue(text.contains("(11) 98888-7777"));
    assertTrue(
        text.contains(
            "https://www.binitechpdv.com.br/pedir/mercadinho-bom-preco/pedido/" + order.getId()));
  }

  @Test
  @DisplayName("Com o Pix integrado o pedido aguarda o pagamento e só devolve o QR code")
  void pixOrderAwaitsPayment() {
    when(pix.isActive(TENANT)).thenReturn(true);
    PixCharge charge = new PixCharge();
    charge.setId("charge-1");
    charge.setStatus(PixChargeStatus.PENDING);
    charge.setQrCode("000201");
    when(pix.createOrderCharge(any())).thenReturn(charge);

    PlacedOrder placed =
        useCase.placeOrder(
            SLUG,
            request(
                DeliveryFulfillment.DELIVERY,
                DeliveryPaymentMethod.PIX,
                null,
                new OrderLine("coca", 2, null)));

    assertEquals(DeliveryOrderStatus.AWAITING_PAYMENT, placed.order().getStatus());
    assertNull(placed.whatsappUrl());
    assertEquals("000201", placed.pix().getQrCode());
    ArgumentCaptor<PixUseCasePort.OrderCharge> captor =
        ArgumentCaptor.forClass(PixUseCasePort.OrderCharge.class);
    verify(pix).createOrderCharge(captor.capture());
    assertEquals(30.0, captor.getValue().amount());
    assertEquals(placed.order().getId(), captor.getValue().orderId());
    assertEquals("Pedido #7 - Mercadinho Bom Preço", captor.getValue().description());
    verify(orders).attachPixCharge(placed.order().getId(), TENANT, "charge-1");
  }

  @Test
  @DisplayName("Sem Pix integrado o pedido com Pix entra direto na fila, como antes")
  void pixOrderWithoutIntegrationGoesToQueue() {
    PlacedOrder placed =
        useCase.placeOrder(
            SLUG,
            request(
                DeliveryFulfillment.DELIVERY,
                DeliveryPaymentMethod.PIX,
                null,
                new OrderLine("coca", 2, null)));

    assertEquals(DeliveryOrderStatus.RECEIVED, placed.order().getStatus());
    assertNull(placed.pix());
    assertTrue(placed.whatsappUrl().startsWith("https://wa.me/"));
    verify(pix, never()).createOrderCharge(any());
  }

  @Test
  @DisplayName("Se o Pix não puder ser gerado o pedido é cancelado e o cliente é avisado")
  void cancelsOrderWhenPixFails() {
    when(pix.isActive(TENANT)).thenReturn(true);
    when(pix.createOrderCharge(any())).thenThrow(new IllegalStateException("fora do ar"));
    OrderRequest failing =
        request(
            DeliveryFulfillment.DELIVERY,
            DeliveryPaymentMethod.PIX,
            null,
            new OrderLine("coca", 2, null));

    assertThrows(BusinessException.class, () -> useCase.placeOrder(SLUG, failing));

    verify(orders)
        .changeStatus(
            anyString(),
            eq(TENANT),
            eq(DeliveryOrderStatus.AWAITING_PAYMENT),
            eq(DeliveryOrderStatus.CANCELLED));
  }

  @Test
  @DisplayName("Acompanhar o pedido consulta o Pix e libera o WhatsApp depois de pago")
  void trackingReleasesAfterPayment() {
    DeliveryOrder awaiting = storedOrder(DeliveryOrderStatus.AWAITING_PAYMENT);
    awaiting.setPixChargeId("charge-1");
    DeliveryOrder received = storedOrder(DeliveryOrderStatus.RECEIVED);
    received.setPaidAt(Instant.parse("2026-11-03T15:01:00Z"));
    received.setAddress(new DeliveryAddress("Rua das Flores", "120", null, "Centro", null));
    when(orders.findByIdAndTenantId("o1", TENANT))
        .thenReturn(Optional.of(awaiting), Optional.of(received));
    when(pix.checkOrderCharge(TENANT, "charge-1")).thenReturn(Optional.empty());

    PlacedOrder placed = useCase.trackOrder(SLUG, "o1");

    assertEquals(DeliveryOrderStatus.RECEIVED, placed.order().getStatus());
    assertNull(placed.pix());
    String text = URLDecoder.decode(placed.whatsappUrl().split("text=")[1], StandardCharsets.UTF_8);
    assertTrue(text.contains("Pix (já pago)"));
    assertTrue(!text.contains("Chave Pix"));
  }

  @Test
  @DisplayName("Pedido cancelado porque o Pix venceu não oferece o WhatsApp da loja")
  void expiredPixOrderHasNoWhatsapp() {
    DeliveryOrder expired = storedOrder(DeliveryOrderStatus.CANCELLED);
    expired.setPixChargeId("charge-1");
    when(orders.findByIdAndTenantId("o1", TENANT)).thenReturn(Optional.of(expired));

    PlacedOrder placed = useCase.trackOrder(SLUG, "o1");

    assertEquals(DeliveryOrderStatus.CANCELLED, placed.order().getStatus());
    assertNull(placed.whatsappUrl());
    assertNull(placed.pix());
  }

  @Test
  @DisplayName("Pedido pago pelo Pix e cancelado pela loja continua com o WhatsApp")
  void paidPixOrderCancelledByStoreKeepsWhatsapp() {
    DeliveryOrder cancelled = storedOrder(DeliveryOrderStatus.CANCELLED);
    cancelled.setPixChargeId("charge-1");
    cancelled.setPaidAt(Instant.parse("2026-11-03T15:01:00Z"));
    cancelled.setAddress(new DeliveryAddress("Rua das Flores", "120", null, "Centro", null));
    when(orders.findByIdAndTenantId("o1", TENANT)).thenReturn(Optional.of(cancelled));

    PlacedOrder placed = useCase.trackOrder(SLUG, "o1");

    assertTrue(placed.whatsappUrl().startsWith("https://wa.me/"));
  }

  @Test
  @DisplayName("Enquanto o Pix está pendente o acompanhamento devolve o QR e nenhum WhatsApp")
  void trackingKeepsQrWhilePending() {
    DeliveryOrder awaiting = storedOrder(DeliveryOrderStatus.AWAITING_PAYMENT);
    awaiting.setPixChargeId("charge-1");
    when(orders.findByIdAndTenantId("o1", TENANT)).thenReturn(Optional.of(awaiting));
    PixCharge charge = new PixCharge();
    charge.setId("charge-1");
    charge.setStatus(PixChargeStatus.PENDING);
    charge.setQrCode("000201");
    when(pix.checkOrderCharge(TENANT, "charge-1")).thenReturn(Optional.of(charge));

    PlacedOrder placed = useCase.trackOrder(SLUG, "o1");

    assertEquals("000201", placed.pix().getQrCode());
    assertNull(placed.whatsappUrl());
  }

  @Test
  @DisplayName("Pedido pago pelo Pix é concluído como Pix sem informar a forma de pagamento")
  void completesPaidOrderAsPix() {
    DeliveryOrder order = storedOrder(DeliveryOrderStatus.ACCEPTED);
    order.setPaidAt(Instant.parse("2026-11-03T15:01:00Z"));
    when(orders.findByIdAndTenantId("o1", TENANT)).thenReturn(Optional.of(order));
    when(orders.changeStatus(
            "o1", TENANT, DeliveryOrderStatus.ACCEPTED, DeliveryOrderStatus.COMPLETED))
        .thenReturn(true);
    Sale sale = new Sale();
    sale.setId("sale-9");
    when(sales.createSale(any(), eq("user-1"), eq(TENANT))).thenReturn(sale);

    useCase.complete("o1", null, "user-1", TENANT);

    ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
    verify(sales).createSale(captor.capture(), eq("user-1"), eq(TENANT));
    assertEquals(PaymentMethod.PIX, captor.getValue().getPayments().get(0).getMethod());
  }

  @Test
  @DisplayName("Retirada não cobra taxa nem pede endereço")
  void pickupHasNoFee() {
    PlacedOrder placed =
        useCase.placeOrder(
            SLUG,
            request(
                DeliveryFulfillment.PICKUP,
                DeliveryPaymentMethod.PIX,
                100.0,
                new OrderLine("coca", 2, null)));
    assertEquals(0.0, placed.order().getDeliveryFee());
    assertEquals(25.0, placed.order().getTotal());
    assertNull(placed.order().getAddress());
    assertNull(placed.order().getChangeFor());
  }

  @Test
  @DisplayName("Recusa pedido abaixo do mínimo, acima do estoque ou com produto fora do cardápio")
  void rejectsInvalidOrders() {
    OrderRequest request2 =
        request(
            DeliveryFulfillment.PICKUP,
            DeliveryPaymentMethod.PIX,
            null,
            new OrderLine("pao", 2, null));
    assertThrows(BusinessException.class, () -> useCase.placeOrder(SLUG, request2));
    OrderRequest request3 =
        request(
            DeliveryFulfillment.PICKUP,
            DeliveryPaymentMethod.PIX,
            null,
            new OrderLine("coca", 2, null),
            new OrderLine("pao", 2, null),
            new OrderLine("pao", 2, null));
    assertThrows(BusinessException.class, () -> useCase.placeOrder(SLUG, request3));
    Product hidden = product("cerveja", "Cerveja", 10, 50);
    hidden.setDeliveryVisible(false);
    when(products.findById("cerveja")).thenReturn(Optional.of(hidden));
    OrderRequest request4 =
        request(
            DeliveryFulfillment.PICKUP,
            DeliveryPaymentMethod.PIX,
            null,
            new OrderLine("cerveja", 2, null));
    assertThrows(BusinessException.class, () -> useCase.placeOrder(SLUG, request4));
    verify(orders, never()).save(any());
  }

  @Test
  @DisplayName("Loja fechada, troco menor que o total ou pagamento não aceito são recusados")
  void rejectsByStoreRules() {
    store.setAcceptedPayments(EnumSet.of(DeliveryPaymentMethod.PIX, DeliveryPaymentMethod.CASH));
    OrderRequest request5 =
        request(
            DeliveryFulfillment.PICKUP,
            DeliveryPaymentMethod.CARD,
            null,
            new OrderLine("coca", 2, null));
    assertThrows(BusinessException.class, () -> useCase.placeOrder(SLUG, request5));
    OrderRequest request6 =
        request(
            DeliveryFulfillment.DELIVERY,
            DeliveryPaymentMethod.CASH,
            20.0,
            new OrderLine("coca", 2, null));
    assertThrows(BusinessException.class, () -> useCase.placeOrder(SLUG, request6));
    store.setOpen(false);
    OrderRequest request7 =
        request(
            DeliveryFulfillment.PICKUP,
            DeliveryPaymentMethod.PIX,
            null,
            new OrderLine("coca", 2, null));
    assertThrows(BusinessException.class, () -> useCase.placeOrder(SLUG, request7));
  }

  @Test
  @DisplayName("Concluir lança a venda no caixa com o subtotal e guarda o id da venda")
  void completesOrderIntoSale() {
    DeliveryOrder order = storedOrder(DeliveryOrderStatus.DISPATCHED);
    when(orders.findByIdAndTenantId("o1", TENANT)).thenReturn(Optional.of(order));
    when(orders.changeStatus(
            "o1", TENANT, DeliveryOrderStatus.DISPATCHED, DeliveryOrderStatus.COMPLETED))
        .thenReturn(true);
    Sale sale = new Sale();
    sale.setId("sale-9");
    when(sales.createSale(any(), eq("user-1"), eq(TENANT))).thenReturn(sale);

    useCase.complete("o1", PaymentMethod.DEBIT_CARD, "user-1", TENANT);

    ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
    verify(sales).createSale(captor.capture(), eq("user-1"), eq(TENANT));
    Sale created = captor.getValue();
    assertEquals(1, created.getItems().size());
    assertEquals(2, created.getItems().get(0).getQuantity());
    assertEquals(PaymentMethod.DEBIT_CARD, created.getPayments().get(0).getMethod());
    assertEquals(25.0, created.getPayments().get(0).getAmount());
    assertEquals("Ana Souza", created.getCustomerName());
    verify(orders).attachSale("o1", TENANT, "sale-9");
  }

  @Test
  @DisplayName("Se a venda falhar, o pedido volta para a situação anterior")
  void revertsWhenSaleFails() {
    DeliveryOrder order = storedOrder(DeliveryOrderStatus.ACCEPTED);
    when(orders.findByIdAndTenantId("o1", TENANT)).thenReturn(Optional.of(order));
    when(orders.changeStatus(
            "o1", TENANT, DeliveryOrderStatus.ACCEPTED, DeliveryOrderStatus.COMPLETED))
        .thenReturn(true);
    when(sales.createSale(any(), anyString(), anyString()))
        .thenThrow(new BusinessException("Estoque insuficiente"));

    assertThrows(
        BusinessException.class, () -> useCase.complete("o1", PaymentMethod.PIX, "u", TENANT));
    verify(orders)
        .changeStatus("o1", TENANT, DeliveryOrderStatus.COMPLETED, DeliveryOrderStatus.ACCEPTED);
    verify(orders, never()).attachSale(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Pedido recebido precisa ser aceito antes de concluir, e não volta de cancelado")
  void enforcesTransitions() {
    when(orders.findByIdAndTenantId("o1", TENANT))
        .thenReturn(Optional.of(storedOrder(DeliveryOrderStatus.RECEIVED)));
    assertThrows(
        BusinessException.class, () -> useCase.complete("o1", PaymentMethod.PIX, "u", TENANT));
    when(orders.findByIdAndTenantId("o2", TENANT))
        .thenReturn(Optional.of(storedOrder(DeliveryOrderStatus.CANCELLED)));
    assertThrows(
        BusinessException.class,
        () -> useCase.updateStatus("o2", TENANT, DeliveryOrderStatus.ACCEPTED));
  }

  @Test
  @DisplayName("Só administrador altera o delivery e o WhatsApp vira formato internacional")
  void updatesSettings() {
    DeliveryStore changes = DeliveryStore.defaults(TENANT);
    changes.setEnabled(true);
    changes.setWhatsapp("(11) 91234-5678");
    assertThrows(
        AccessDeniedException.class, () -> useCase.updateSettings(TENANT, changes, Role.OPERATOR));
    when(stores.save(any())).thenAnswer(inv -> inv.getArgument(0));
    DeliveryStore saved = useCase.updateSettings(TENANT, changes, Role.TENANT_ADMIN).store();
    assertEquals("5511912345678", saved.getWhatsapp());
    changes.setWhatsapp("");
    assertThrows(
        BusinessException.class, () -> useCase.updateSettings(TENANT, changes, Role.TENANT_ADMIN));
  }

  private static DeliveryOrder storedOrder(DeliveryOrderStatus status) {
    DeliveryOrder order = new DeliveryOrder();
    order.setId("o1");
    order.setTenantId(TENANT);
    order.setNumber(3);
    order.setStatus(status);
    order.setFulfillment(DeliveryFulfillment.DELIVERY);
    order.setCustomerName("Ana Souza");
    order.setCustomerPhone("5511988887777");
    order.setPaymentMethod(DeliveryPaymentMethod.PIX);
    order.setItems(List.of(new DeliveryOrderItem("coca", "Coca-Cola 2L", 12.5, 2, null)));
    order.recalculate(5);
    return order;
  }
}
