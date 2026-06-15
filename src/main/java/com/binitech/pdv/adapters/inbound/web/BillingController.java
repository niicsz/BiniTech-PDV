package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.domain.Invoice;
import com.binitech.pdv.domain.Subscription;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

  private final BillingUseCasePort billingUseCase;
  private final AuthenticatedUserProvider authenticatedUserProvider;

  public BillingController(
      BillingUseCasePort billingUseCase, AuthenticatedUserProvider authenticatedUserProvider) {
    this.billingUseCase = billingUseCase;
    this.authenticatedUserProvider = authenticatedUserProvider;
  }

  @GetMapping("/subscription")
  public ResponseEntity<SubscriptionDTO> getSubscription() {
    return billingUseCase
        .getSubscriptionForTenant(authenticatedUserProvider.getTenantId())
        .map(this::toSubscriptionDto)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/invoices")
  public ResponseEntity<List<InvoiceDTO>> getInvoices() {
    List<InvoiceDTO> invoices =
        billingUseCase.getInvoicesForTenant(authenticatedUserProvider.getTenantId()).stream()
            .map(this::toInvoiceDto)
            .toList();
    return ResponseEntity.ok(invoices);
  }

  @GetMapping("/checkout")
  public ResponseEntity<UrlDTO> getCheckoutUrl() {
    String url = billingUseCase.createCheckoutUrl(authenticatedUserProvider.getTenantId());
    return ResponseEntity.ok(new UrlDTO(url));
  }

  @GetMapping("/portal")
  public ResponseEntity<UrlDTO> getPortalUrl() {
    String url = billingUseCase.createPortalUrl(authenticatedUserProvider.getTenantId());
    return ResponseEntity.ok(new UrlDTO(url));
  }

  private SubscriptionDTO toSubscriptionDto(Subscription subscription) {
    return new SubscriptionDTO(
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getStripeSubscriptionId(),
        subscription.getStripeCustomerId(),
        subscription.getStripePriceId(),
        subscription.getPlanTier(),
        subscription.getStatus() != null ? subscription.getStatus().name() : null,
        subscription.getCurrentPeriodStart(),
        subscription.getCurrentPeriodEnd(),
        subscription.getNextBillingDate(),
        subscription.getLastPaymentDate(),
        subscription.getFailedPaymentCount(),
        subscription.getCancelledAt(),
        subscription.getCreatedAt(),
        subscription.getUpdatedAt());
  }

  private InvoiceDTO toInvoiceDto(Invoice invoice) {
    return new InvoiceDTO(
        invoice.getId(),
        invoice.getTenantId(),
        invoice.getSubscriptionId(),
        invoice.getStripeInvoiceId(),
        invoice.getAmount(),
        invoice.getStatus() != null ? invoice.getStatus().name() : null,
        invoice.getDueDate(),
        invoice.getPaidAt(),
        invoice.getDescription(),
        invoice.getBaseAmount(),
        invoice.getExcessAmount(),
        invoice.getCreatedAt(),
        invoice.getUpdatedAt());
  }

  public record SubscriptionDTO(
      String id,
      String tenantId,
      String stripeSubscriptionId,
      String stripeCustomerId,
      String stripePriceId,
      String planTier,
      String status,
      LocalDate currentPeriodStart,
      LocalDate currentPeriodEnd,
      LocalDate nextBillingDate,
      LocalDate lastPaymentDate,
      int failedPaymentCount,
      LocalDate cancelledAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  public record UrlDTO(String url) {}

  public record InvoiceDTO(
      String id,
      String tenantId,
      String subscriptionId,
      String stripeInvoiceId,
      BigDecimal amount,
      String status,
      LocalDate dueDate,
      LocalDate paidAt,
      String description,
      BigDecimal baseAmount,
      BigDecimal excessAmount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
