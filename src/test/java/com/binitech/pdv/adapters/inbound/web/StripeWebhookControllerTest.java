package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.StripeGateway;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

  @Mock private BillingUseCasePort billingUseCase;
  @Mock private StripeGateway stripeGateway;
  @Mock private SubscriptionRepositoryPort subscriptionRepository;
  @Mock private TenantRepositoryPort tenantRepository;

  private StripeWebhookController controller() {
    return new StripeWebhookController(
        billingUseCase, stripeGateway, subscriptionRepository, tenantRepository);
  }

  @Test
  @DisplayName("Assinatura inválida resulta em 400 e nenhum processamento")
  void invalidSignature_shouldReturn400() throws Exception {
    when(stripeGateway.constructEvent(anyString(), anyString()))
        .thenThrow(new SignatureVerificationException("assinatura inválida", "sig"));

    ResponseEntity<Void> response = controller().handleWebhook("{}", "sig");

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    verify(billingUseCase, never()).activateFromCheckout(any(), any(), any(), any());
  }

  @Test
  @DisplayName("checkout.session.completed ativa a assinatura via use case")
  void checkoutCompleted_shouldActivate() throws Exception {
    Event event = mock(Event.class);
    EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
    com.stripe.model.checkout.Session session = mock(com.stripe.model.checkout.Session.class);

    when(stripeGateway.constructEvent(anyString(), anyString())).thenReturn(event);
    when(event.getType()).thenReturn("checkout.session.completed");
    when(event.getId()).thenReturn("evt_1");
    when(event.getDataObjectDeserializer()).thenReturn(deserializer);
    when(deserializer.getObject()).thenReturn(Optional.of((StripeObject) session));
    when(session.getClientReferenceId()).thenReturn("t1");
    when(session.getSubscription()).thenReturn("sub_1");
    when(session.getCustomer()).thenReturn("cus_1");
    when(stripeGateway.getPriceIdForSubscription("sub_1")).thenReturn("price_pro");

    ResponseEntity<Void> response = controller().handleWebhook("{}", "sig");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(billingUseCase).activateFromCheckout("sub_1", "cus_1", "t1", "price_pro");
  }
}
