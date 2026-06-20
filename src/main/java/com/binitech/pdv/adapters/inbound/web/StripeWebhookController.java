package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.StripeGateway;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe os webhooks do Stripe. A assinatura é verificada pelo próprio SDK ({@link
 * StripeGateway#constructEvent}); eventos com assinatura inválida tomam 400. Eventos tratados:
 * {@code checkout.session.completed} (ativa), {@code invoice.paid} (renovação), {@code
 * invoice.payment_failed} (falha) e {@code customer.subscription.deleted} (cancelamento).
 */
@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

  private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

  private final BillingUseCasePort billingUseCase;
  private final StripeGateway stripeGateway;
  private final SubscriptionRepositoryPort subscriptionRepository;
  private final TenantRepositoryPort tenantRepository;

  public StripeWebhookController(
      BillingUseCasePort billingUseCase,
      StripeGateway stripeGateway,
      SubscriptionRepositoryPort subscriptionRepository,
      TenantRepositoryPort tenantRepository) {
    this.billingUseCase = billingUseCase;
    this.stripeGateway = stripeGateway;
    this.subscriptionRepository = subscriptionRepository;
    this.tenantRepository = tenantRepository;
  }

  @PostMapping
  public ResponseEntity<Void> handleWebhook(
      @RequestBody String payload,
      @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
    Event event;
    try {
      event = stripeGateway.constructEvent(payload, signature);
    } catch (StripeException e) {
      log.warn("Webhook Stripe rejeitado (assinatura inválida): {}", e.getMessage());
      return ResponseEntity.badRequest().build();
    }

    log.info(
        "Webhook Stripe recebido: type={} id={}",
        event.getType(),
        LogSanitizer.maskId(event.getId()));

    try {
      switch (event.getType()) {
        case "checkout.session.completed" -> handleCheckoutCompleted(event);
        case "invoice.paid" -> handleInvoicePaid(event);
        case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
        case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
        default -> log.info("Evento Stripe ignorado: type={}", event.getType());
      }
    } catch (Exception e) {
      log.error(
          "Erro ao processar webhook Stripe: type={} error={}", event.getType(), e.getMessage(), e);
    }

    return ResponseEntity.ok().build();
  }

  private void handleCheckoutCompleted(Event event) throws StripeException {
    com.stripe.model.checkout.Session session =
        (com.stripe.model.checkout.Session) deserialize(event);
    if (session == null) {
      log.warn("checkout.session.completed sem objeto desserializável");
      return;
    }
    String tenantId = session.getClientReferenceId();
    String stripeSubscriptionId = session.getSubscription();
    String stripeCustomerId = session.getCustomer();
    String priceId =
        hasText(stripeSubscriptionId)
            ? stripeGateway.getPriceIdForSubscription(stripeSubscriptionId)
            : null;
    billingUseCase.activateFromCheckout(stripeSubscriptionId, stripeCustomerId, tenantId, priceId);
  }

  private void handleInvoicePaid(Event event) {
    com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) deserialize(event);
    if (invoice == null) {
      return;
    }
    billingUseCase.markInvoicePaid(invoice.getId(), subscriptionIdOf(invoice));
  }

  private void handleInvoicePaymentFailed(Event event) {
    com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) deserialize(event);
    if (invoice == null) {
      return;
    }
    String subscriptionId = subscriptionIdOf(invoice);
    if (!hasText(subscriptionId)) {
      return;
    }
    billingUseCase.recordPaymentFailure(subscriptionId);
  }

  /** Extrai o id da subscription de uma invoice do Stripe (modelo novo: invoice.parent). */
  private String subscriptionIdOf(com.stripe.model.Invoice invoice) {
    com.stripe.model.Invoice.Parent parent = invoice.getParent();
    if (parent == null || parent.getSubscriptionDetails() == null) {
      return null;
    }
    return parent.getSubscriptionDetails().getSubscription();
  }

  private void handleSubscriptionDeleted(Event event) {
    com.stripe.model.Subscription stripeSubscription =
        (com.stripe.model.Subscription) deserialize(event);
    if (stripeSubscription == null) {
      return;
    }
    cancelSubscription(stripeSubscription.getId());
  }

  private void cancelSubscription(String stripeSubscriptionId) {
    Optional<Subscription> subscriptionOptional =
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);
    if (subscriptionOptional.isEmpty()) {
      log.warn(
          "Subscription não encontrada para cancelamento: subscriptionId={}",
          LogSanitizer.maskId(stripeSubscriptionId));
      return;
    }

    Subscription subscription = subscriptionOptional.get();
    subscription.setStatus(SubscriptionStatus.CANCELLED);
    subscription.setCancelledAt(LocalDate.now(ZoneId.systemDefault()));
    subscription.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    subscriptionRepository.save(subscription);

    tenantRepository
        .findById(subscription.getTenantId())
        .ifPresent(
            tenant -> {
              tenant.setStatus(TenantStatus.CANCELLED);
              tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
              tenantRepository.save(tenant);
            });

    log.warn(
        "Subscription cancelada via webhook: subscriptionId={} tenantId={}",
        LogSanitizer.maskId(stripeSubscriptionId),
        LogSanitizer.maskId(subscription.getTenantId()));
  }

  /**
   * Desserializa o objeto do evento, com fallback "unsafe" quando a versão de API do evento difere
   * da versão do SDK (cenário comum em integrações reais).
   */
  private StripeObject deserialize(Event event) {
    EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
    if (deserializer.getObject().isPresent()) {
      return deserializer.getObject().get();
    }
    try {
      return deserializer.deserializeUnsafe();
    } catch (EventDataObjectDeserializationException e) {
      log.error(
          "Falha ao desserializar evento Stripe: type={} error={}",
          event.getType(),
          e.getMessage());
      return null;
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
