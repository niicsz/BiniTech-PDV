package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.AsaasGateway;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/asaas")
public class AsaasWebhookController {

  private static final Logger log = LoggerFactory.getLogger(AsaasWebhookController.class);

  private final BillingUseCasePort billingUseCase;
  private final AsaasGateway asaasGateway;
  private final SubscriptionRepositoryPort subscriptionRepository;
  private final TenantRepositoryPort tenantRepository;

  public AsaasWebhookController(
      BillingUseCasePort billingUseCase,
      AsaasGateway asaasGateway,
      SubscriptionRepositoryPort subscriptionRepository,
      TenantRepositoryPort tenantRepository) {
    this.billingUseCase = billingUseCase;
    this.asaasGateway = asaasGateway;
    this.subscriptionRepository = subscriptionRepository;
    this.tenantRepository = tenantRepository;
  }

  @PostMapping
  public ResponseEntity<Void> handleWebhook(
      @RequestBody AsaasEvent event,
      @RequestHeader(value = "asaas-access-token", required = false) String accessToken) {
    if (!asaasGateway.isValidWebhookToken(accessToken)) {
      log.warn("Webhook Asaas rejeitado (token inválido)");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (event == null || event.event() == null) {
      return ResponseEntity.badRequest().build();
    }

    log.info(
        "Webhook Asaas recebido: event={} id={}",
        LogSanitizer.neutralize(event.event()),
        LogSanitizer.maskId(event.id()));

    try {
      switch (event.event()) {
        case "CHECKOUT_PAID" -> handleCheckoutPaid(event.checkout());
        case "PAYMENT_CONFIRMED" -> handlePaymentPaid(event.payment());
        case "PAYMENT_RECEIVED" -> handlePaymentReceived(event.payment());
        case "PAYMENT_OVERDUE", "PAYMENT_CREDIT_CARD_CAPTURE_REFUSED" ->
            handlePaymentFailed(event.payment());
        case "SUBSCRIPTION_DELETED", "SUBSCRIPTION_INACTIVATED" ->
            handleSubscriptionEnded(event.subscription());
        default ->
            log.info("Evento Asaas ignorado: event={}", LogSanitizer.neutralize(event.event()));
      }
    } catch (Exception e) {
      log.error(
          "Erro ao processar webhook Asaas: event={} error={}",
          LogSanitizer.neutralize(event.event()),
          e.getMessage(),
          e);
    }

    return ResponseEntity.ok().build();
  }

  private void handleCheckoutPaid(AsaasCheckout checkout) {
    if (checkout == null) {
      log.warn("CHECKOUT_PAID sem objeto checkout");
      return;
    }
    String customerId = checkout.customer();
    String subscriptionId =
        hasText(customerId) ? asaasGateway.findActiveSubscriptionId(customerId) : null;
    billingUseCase.activateFromCheckoutSession(
        checkout.id(), subscriptionId, customerId, checkout.externalReference());
  }

  private void handlePaymentReceived(AsaasPayment payment) {
    if (payment != null && "CREDIT_CARD".equals(payment.billingType())) {
      return;
    }
    handlePaymentPaid(payment);
  }

  private void handlePaymentPaid(AsaasPayment payment) {
    if (payment == null || !hasText(payment.subscription())) {
      return;
    }
    billingUseCase.confirmSubscriptionPayment(
        payment.id(), payment.subscription(), payment.customer(), payment.checkoutSession());
  }

  private void handlePaymentFailed(AsaasPayment payment) {
    if (payment == null || !hasText(payment.subscription())) {
      return;
    }
    billingUseCase.recordPaymentFailure(payment.subscription());
  }

  private void handleSubscriptionEnded(AsaasSubscription asaasSubscription) {
    if (asaasSubscription == null || !hasText(asaasSubscription.id())) {
      return;
    }
    cancelSubscription(asaasSubscription.id());
  }

  private void cancelSubscription(String asaasSubscriptionId) {
    Optional<Subscription> subscriptionOptional =
        subscriptionRepository.findByAsaasSubscriptionId(asaasSubscriptionId);
    if (subscriptionOptional.isEmpty()) {
      log.warn(
          "Subscription não encontrada para cancelamento: subscriptionId={}",
          LogSanitizer.maskId(asaasSubscriptionId));
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
        LogSanitizer.maskId(asaasSubscriptionId),
        LogSanitizer.maskId(subscription.getTenantId()));
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public record AsaasEvent(
      String id,
      String event,
      AsaasCheckout checkout,
      AsaasPayment payment,
      AsaasSubscription subscription) {}

  public record AsaasCheckout(String id, String customer, String externalReference) {}

  public record AsaasPayment(
      String id,
      String customer,
      String subscription,
      String billingType,
      String checkoutSession) {}

  public record AsaasSubscription(String id, String customer) {}
}
