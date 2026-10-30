package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.inbound.web.AsaasWebhookController.AsaasCheckout;
import com.binitech.pdv.adapters.inbound.web.AsaasWebhookController.AsaasEvent;
import com.binitech.pdv.adapters.inbound.web.AsaasWebhookController.AsaasPayment;
import com.binitech.pdv.adapters.inbound.web.AsaasWebhookController.AsaasSubscription;
import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.AsaasGateway;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AsaasWebhookControllerTest {

  private static final String TOKEN = "token-seguro-com-mais-de-32-caracteres";

  @Mock private BillingUseCasePort billingUseCase;
  @Mock private AsaasGateway asaasGateway;
  @Mock private SubscriptionRepositoryPort subscriptionRepository;
  @Mock private TenantRepositoryPort tenantRepository;

  private AsaasWebhookController controller() {
    return new AsaasWebhookController(
        billingUseCase, asaasGateway, subscriptionRepository, tenantRepository);
  }

  private AsaasEvent event(
      String type, AsaasCheckout checkout, AsaasPayment payment, AsaasSubscription subscription) {
    return new AsaasEvent("evt_1", type, checkout, payment, subscription);
  }

  @Test
  @DisplayName("Token inválido resulta em 401 e nenhum processamento")
  void invalidToken_shouldReturn401() {
    when(asaasGateway.isValidWebhookToken("errado")).thenReturn(false);

    ResponseEntity<Void> response =
        controller()
            .handleWebhook(
                event("CHECKOUT_PAID", new AsaasCheckout("chk", "cus_1", "t1"), null, null),
                "errado");

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(billingUseCase);
  }

  @Test
  @DisplayName("CHECKOUT_PAID ativa a assinatura com o id da assinatura do cliente")
  void checkoutPaid_shouldActivate() {
    when(asaasGateway.isValidWebhookToken(TOKEN)).thenReturn(true);
    when(asaasGateway.findActiveSubscriptionId("cus_1")).thenReturn("sub_1");

    ResponseEntity<Void> response =
        controller()
            .handleWebhook(
                event("CHECKOUT_PAID", new AsaasCheckout("chk", "cus_1", "t1"), null, null), TOKEN);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(billingUseCase).activateFromCheckoutSession("chk", "sub_1", "cus_1", "t1");
  }

  @Test
  @DisplayName("PAYMENT_CONFIRMED confirma o pagamento da assinatura")
  void paymentConfirmed_shouldMarkPaid() {
    when(asaasGateway.isValidWebhookToken(TOKEN)).thenReturn(true);

    controller()
        .handleWebhook(
            event(
                "PAYMENT_CONFIRMED",
                null,
                new AsaasPayment("pay_1", "cus_1", "sub_1", "CREDIT_CARD", "chk"),
                null),
            TOKEN);

    verify(billingUseCase).confirmSubscriptionPayment("pay_1", "sub_1", "cus_1", "chk");
  }

  @Test
  @DisplayName("PAYMENT_RECEIVED de cartão é ignorado (já tratado no PAYMENT_CONFIRMED)")
  void paymentReceivedCreditCard_shouldBeIgnored() {
    when(asaasGateway.isValidWebhookToken(TOKEN)).thenReturn(true);

    controller()
        .handleWebhook(
            event(
                "PAYMENT_RECEIVED",
                null,
                new AsaasPayment("pay_1", "cus_1", "sub_1", "CREDIT_CARD", "chk"),
                null),
            TOKEN);

    verify(billingUseCase, never()).confirmSubscriptionPayment(any(), any(), any(), any());
  }

  @Test
  @DisplayName("PAYMENT_RECEIVED via Pix confirma o pagamento")
  void paymentReceivedPix_shouldMarkPaid() {
    when(asaasGateway.isValidWebhookToken(TOKEN)).thenReturn(true);

    controller()
        .handleWebhook(
            event(
                "PAYMENT_RECEIVED",
                null,
                new AsaasPayment("pay_2", "cus_1", "sub_1", "PIX", null),
                null),
            TOKEN);

    verify(billingUseCase).confirmSubscriptionPayment("pay_2", "sub_1", "cus_1", null);
  }

  @Test
  @DisplayName("PAYMENT_OVERDUE registra falha de pagamento")
  void paymentOverdue_shouldRecordFailure() {
    when(asaasGateway.isValidWebhookToken(TOKEN)).thenReturn(true);

    controller()
        .handleWebhook(
            event(
                "PAYMENT_OVERDUE",
                null,
                new AsaasPayment("pay_3", "cus_1", "sub_1", "CREDIT_CARD", null),
                null),
            TOKEN);

    verify(billingUseCase).recordPaymentFailure("sub_1");
  }

  @Test
  @DisplayName("SUBSCRIPTION_DELETED cancela assinatura e tenant")
  void subscriptionDeleted_shouldCancel() {
    when(asaasGateway.isValidWebhookToken(TOKEN)).thenReturn(true);
    Subscription subscription = new Subscription();
    subscription.setTenantId("t1");
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    Tenant tenant = new Tenant();
    tenant.setId("t1");
    tenant.setStatus(TenantStatus.ACTIVE);
    when(subscriptionRepository.findByAsaasSubscriptionId("sub_1"))
        .thenReturn(Optional.of(subscription));
    when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));

    controller()
        .handleWebhook(
            event("SUBSCRIPTION_DELETED", null, null, new AsaasSubscription("sub_1", "cus_1")),
            TOKEN);

    assertEquals(SubscriptionStatus.CANCELLED, subscription.getStatus());
    assertEquals(TenantStatus.CANCELLED, tenant.getStatus());
    verify(subscriptionRepository).save(subscription);
    verify(tenantRepository).save(tenant);
  }
}
