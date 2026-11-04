package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.inbound.BillingUseCasePort.PlanOverview;
import com.binitech.pdv.domain.Invoice;
import com.binitech.pdv.domain.Subscription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @GetMapping("/checkout/pix")
  public ResponseEntity<UrlDTO> getPixCheckoutUrl() {
    String url = billingUseCase.createPixCheckoutUrl(authenticatedUserProvider.getTenantId());
    return ResponseEntity.ok(new UrlDTO(url));
  }

  @GetMapping("/plan")
  public ResponseEntity<PlanOverviewDTO> getPlan() {
    return ResponseEntity.ok(
        toPlanDto(billingUseCase.getPlanOverview(authenticatedUserProvider.getTenantId())));
  }

  @PutMapping("/plan")
  public ResponseEntity<PlanOverviewDTO> changePlan(@Valid @RequestBody ChangePlanRequest request) {
    return ResponseEntity.ok(
        toPlanDto(
            billingUseCase.changePlan(
                authenticatedUserProvider.getTenantId(),
                request.planId(),
                authenticatedUserProvider.getUserRole())));
  }

  @PutMapping("/extra-users")
  public ResponseEntity<PlanOverviewDTO> setExtraUsers(
      @Valid @RequestBody ExtraUsersRequest request) {
    return ResponseEntity.ok(
        toPlanDto(
            billingUseCase.setExtraUsers(
                authenticatedUserProvider.getTenantId(),
                request.quantity(),
                authenticatedUserProvider.getUserRole())));
  }

  @GetMapping("/portal")
  public ResponseEntity<UrlDTO> getPortalUrl() {
    String url = billingUseCase.createPortalUrl(authenticatedUserProvider.getTenantId());
    return ResponseEntity.ok(new UrlDTO(url));
  }

  private PlanOverviewDTO toPlanDto(PlanOverview overview) {
    return new PlanOverviewDTO(
        overview.currentPlan(),
        overview.extraUsers(),
        overview.operatorsInUse(),
        overview.operatorLimit() == Integer.MAX_VALUE ? null : overview.operatorLimit(),
        overview.monthlyAmount(),
        overview.extraUserPrice(),
        overview.recurringCard(),
        overview.plans().stream()
            .map(
                p ->
                    new PlanOptionDTO(
                        p.id(), p.maxProducts(), p.maxSalesPerMonth(), p.maxOperators(), p.price()))
            .toList());
  }

  private SubscriptionDTO toSubscriptionDto(Subscription subscription) {
    return new SubscriptionDTO(
        subscription.getId(),
        subscription.getTenantId(),
        subscription.getAsaasSubscriptionId(),
        subscription.getAsaasCustomerId(),
        subscription.getPlanTier(),
        subscription.getBillingType(),
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
        invoice.getAsaasPaymentId(),
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
      String asaasSubscriptionId,
      String asaasCustomerId,
      String planTier,
      String billingType,
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

  public record ChangePlanRequest(@NotBlank String planId) {}

  public record ExtraUsersRequest(@NotNull @Min(0) @Max(50) Integer quantity) {}

  public record PlanOptionDTO(
      String id, int maxProducts, int maxSalesPerMonth, int maxOperators, BigDecimal price) {}

  public record PlanOverviewDTO(
      String currentPlan,
      int extraUsers,
      long operatorsInUse,
      Integer operatorLimit,
      BigDecimal monthlyAmount,
      BigDecimal extraUserPrice,
      boolean recurringCard,
      List<PlanOptionDTO> plans) {}

  public record InvoiceDTO(
      String id,
      String tenantId,
      String subscriptionId,
      String asaasPaymentId,
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
