package com.binitech.pdv.application.usecases;

import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataUserRepository;
import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.outbound.InvoiceRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.BillingStripeConfig;
import com.binitech.pdv.config.PlanConfig;
import com.binitech.pdv.config.PlanConfig.PlanLimits;
import com.binitech.pdv.config.StripeGateway;
import com.binitech.pdv.config.StripeProperties;
import com.binitech.pdv.domain.Invoice;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.InvoiceStatus;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import com.stripe.exception.StripeException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BillingUseCaseImpl implements BillingUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(BillingUseCaseImpl.class);
  private static final List<String> MONTH_NAMES =
      List.of("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez");

  private final SubscriptionRepositoryPort subscriptionRepository;
  private final InvoiceRepositoryPort invoiceRepository;
  private final TenantRepositoryPort tenantRepository;
  private final SpringDataProductRepository productRepository;
  private final SpringDataUserRepository userRepository;
  private final String frontendUrl;
  private final StripeGateway stripeGateway;
  private final StripeProperties stripeProperties;

  public BillingUseCaseImpl(
      SubscriptionRepositoryPort subscriptionRepository,
      InvoiceRepositoryPort invoiceRepository,
      TenantRepositoryPort tenantRepository,
      SpringDataProductRepository productRepository,
      SpringDataUserRepository userRepository,
      BillingStripeConfig stripeConfig) {
    this.subscriptionRepository = subscriptionRepository;
    this.invoiceRepository = invoiceRepository;
    this.tenantRepository = tenantRepository;
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.frontendUrl = stripeConfig.frontendUrl();
    this.stripeGateway = stripeConfig.stripeGateway();
    this.stripeProperties = stripeConfig.stripeProperties();
  }

  @Override
  public Subscription createSubscription(String tenantId, String planTier) {
    PlanLimits limits = PlanConfig.getLimits(planTier);
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    Subscription subscription =
        subscriptionRepository.findByTenantId(tenantId).orElseGet(Subscription::new);

    subscription.setTenantId(tenantId);
    subscription.setStripePriceId(stripeProperties.priceForTier(planTier));
    subscription.setPlanTier(planTier.toLowerCase(Locale.ROOT));
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setCurrentPeriodStart(today);
    subscription.setCurrentPeriodEnd(today.plusDays(30));
    subscription.setNextBillingDate(today.plusDays(30));
    subscription.setLastPaymentDate(null);
    subscription.setFailedPaymentCount(0);
    subscription.setCancelledAt(null);
    if (subscription.getCreatedAt() == null) {
      subscription.setCreatedAt(now);
    }
    subscription.setUpdatedAt(now);

    Subscription saved = subscriptionRepository.save(subscription);
    if (log.isInfoEnabled()) {
      log.info(
          "Subscription criada/atualizada: tenantId={} tier={} baseFee={}",
          LogSanitizer.maskId(saved.getTenantId()),
          LogSanitizer.neutralize(saved.getPlanTier()),
          limits.baseMonthlyFee());
    }
    return saved;
  }

  @Override
  public Invoice generateMonthlyInvoice(
      String tenantId, int activeProducts, int monthlySales, int operators) {
    Subscription subscription =
        subscriptionRepository
            .findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription", "tenantId", tenantId));
    PlanLimits limits = PlanConfig.getLimits(subscription.getPlanTier());
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

    int effectiveProducts =
        activeProducts >= 0
            ? activeProducts
            : (int) productRepository.countByTenantIdAndActiveTrue(tenantId);
    int effectiveOperators =
        operators >= 0 ? operators : (int) userRepository.countByTenantId(tenantId);
    int extraProducts = Math.max(effectiveProducts - limits.maxProducts(), 0);
    int extraSales = Math.max(monthlySales - limits.maxSalesPerMonth(), 0);
    int extraOperators = Math.max(effectiveOperators - limits.maxOperators(), 0);

    BigDecimal baseAmount = limits.baseMonthlyFee();
    BigDecimal excessAmount =
        PlanConfig.EXCESS_PER_PRODUCT
            .multiply(BigDecimal.valueOf(extraProducts))
            .add(PlanConfig.EXCESS_PER_SALE.multiply(BigDecimal.valueOf(extraSales)))
            .add(PlanConfig.EXCESS_PER_OPERATOR.multiply(BigDecimal.valueOf(extraOperators)));

    Invoice invoice = new Invoice();
    invoice.setTenantId(tenantId);
    invoice.setSubscriptionId(subscription.getId());
    invoice.setAmount(baseAmount.add(excessAmount));
    invoice.setStatus(InvoiceStatus.PENDING);
    invoice.setDueDate(today.plusDays(5));
    invoice.setPaidAt(null);
    invoice.setDescription(
        buildDescription(
            subscription.getPlanTier(), today, extraProducts, extraSales, extraOperators));
    invoice.setBaseAmount(baseAmount);
    invoice.setExcessAmount(excessAmount);
    invoice.setCreatedAt(now);
    invoice.setUpdatedAt(now);

    Invoice saved = invoiceRepository.save(invoice);

    subscription.setCurrentPeriodStart(today);
    subscription.setCurrentPeriodEnd(today.plusDays(30));
    subscription.setNextBillingDate(today.plusDays(30));
    subscription.setUpdatedAt(now);
    subscriptionRepository.save(subscription);

    if (log.isInfoEnabled()) {
      log.info(
          "Fatura mensal gerada: tenantId={} invoiceId={} total={}",
          LogSanitizer.maskId(tenantId),
          LogSanitizer.maskId(saved.getId()),
          saved.getAmount());
    }
    return saved;
  }

  @Override
  public Invoice markInvoicePaid(String stripeInvoiceId, String stripeSubscriptionId) {
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

    Optional<Subscription> subscriptionOptional =
        hasText(stripeSubscriptionId)
            ? subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
            : Optional.empty();
    subscriptionOptional.ifPresent(
        subscription -> {
          subscription.setLastPaymentDate(today);
          subscription.setFailedPaymentCount(0);
          subscription.setStatus(SubscriptionStatus.ACTIVE);
          subscription.setUpdatedAt(now);
          subscriptionRepository.save(subscription);
        });

    Invoice savedInvoice = null;
    Optional<Invoice> invoiceOptional =
        findInvoiceForPaidWebhook(
            stripeInvoiceId, subscriptionOptional.map(Subscription::getTenantId).orElse(null));
    if (invoiceOptional.isPresent()) {
      Invoice invoice = invoiceOptional.get();
      if (hasText(stripeInvoiceId)) {
        invoice.setStripeInvoiceId(stripeInvoiceId);
      }
      invoice.setStatus(InvoiceStatus.PAID);
      invoice.setPaidAt(today);
      invoice.setUpdatedAt(now);
      savedInvoice = invoiceRepository.save(invoice);
    }

    String tenantId = subscriptionOptional.map(Subscription::getTenantId).orElse(null);
    if (tenantId == null && savedInvoice != null) {
      tenantId = savedInvoice.getTenantId();
    }
    if (tenantId != null) {
      unblockTenant(tenantId, now);
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Pagamento de fatura processado: invoiceId={} subscriptionId={}",
          LogSanitizer.maskId(stripeInvoiceId),
          LogSanitizer.maskId(stripeSubscriptionId));
    }
    return savedInvoice;
  }

  @Override
  public void recordPaymentFailure(String stripeSubscriptionId) {
    Subscription subscription =
        subscriptionRepository
            .findByStripeSubscriptionId(stripeSubscriptionId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Subscription", "stripeSubscriptionId", stripeSubscriptionId));
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

    subscription.setFailedPaymentCount(subscription.getFailedPaymentCount() + 1);
    subscription.setStatus(SubscriptionStatus.PAST_DUE);
    subscription.setUpdatedAt(now);
    subscriptionRepository.save(subscription);

    findLatestInvoiceByTenantAndStatuses(subscription.getTenantId(), InvoiceStatus.PENDING)
        .ifPresent(
            invoice -> {
              invoice.setStatus(InvoiceStatus.OVERDUE);
              invoice.setUpdatedAt(now);
              invoiceRepository.save(invoice);
            });

    log.warn(
        "Falha de pagamento registrada: subscriptionId={} tentativas={}",
        LogSanitizer.maskId(stripeSubscriptionId),
        subscription.getFailedPaymentCount());
  }

  @Override
  public void blockOverdueTenants(int graceDays) {
    LocalDate cutoffDate = LocalDate.now(ZoneId.systemDefault()).minusDays(graceDays);
    Set<String> processedTenantIds = new HashSet<>();

    for (Invoice invoice : invoiceRepository.findOverdueInvoicesBefore(cutoffDate)) {
      if (!processedTenantIds.add(invoice.getTenantId())) {
        continue;
      }
      tenantRepository
          .findById(invoice.getTenantId())
          .ifPresent(
              tenant -> {
                if (tenant.getStatus() == TenantStatus.ACTIVE) {
                  tenant.setStatus(TenantStatus.BLOCKED);
                  tenant.setBlockedAt(LocalDate.now(ZoneId.systemDefault()));
                  tenant.setBlockReason("Fatura vencida");
                  tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
                  tenantRepository.save(tenant);
                  log.warn(
                      "Tenant bloqueado por inadimplência: tenantId={} invoiceId={}",
                      LogSanitizer.maskId(tenant.getId()),
                      LogSanitizer.maskId(invoice.getId()));
                }
              });
    }
  }

  @Override
  public List<Invoice> getInvoicesForTenant(String tenantId) {
    return invoiceRepository.findAllByTenantIdOrderByDueDateDesc(tenantId);
  }

  @Override
  public Optional<Subscription> getSubscriptionForTenant(String tenantId) {
    return subscriptionRepository.findByTenantId(tenantId);
  }

  @Override
  public String createCheckoutUrl(String tenantId) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    if (PlanConfig.isFree(tenant.getPlanId())) {
      throw new BusinessException("Plano cortesia não requer pagamento.");
    }
    if (!stripeGateway.isConfigured()) {
      throw new BusinessException(
          "Pagamento online indisponível no momento. Tente novamente mais tarde.");
    }
    String priceId = stripeProperties.priceForTier(tenant.getPlanId());
    if (!hasText(priceId)) {
      throw new BusinessException("Plano sem price configurado no Stripe: " + tenant.getPlanId());
    }
    String baseUrl = stripTrailingSlash(frontendUrl);
    String successUrl = baseUrl + "/billing?status=success";
    String cancelUrl = baseUrl + "/billing";
    try {
      return stripeGateway.createSubscriptionCheckoutSession(
          tenant, priceId, successUrl, cancelUrl);
    } catch (StripeException e) {
      log.error(
          "Falha ao criar checkout no Stripe: tenantId={} error={}",
          LogSanitizer.maskId(tenantId),
          e.getMessage());
      throw new BusinessException("Não foi possível iniciar o pagamento. Tente novamente.");
    }
  }

  @Override
  public String createPortalUrl(String tenantId) {
    Subscription subscription =
        subscriptionRepository
            .findByTenantId(tenantId)
            .orElseThrow(
                () -> new BusinessException("Nenhuma assinatura encontrada para gerenciar."));
    if (!hasText(subscription.getStripeCustomerId())) {
      throw new BusinessException(
          "Assinatura ainda não vinculada ao Stripe. Conclua um pagamento primeiro.");
    }
    if (!stripeGateway.isConfigured()) {
      throw new BusinessException("Gestão de assinatura indisponível no momento.");
    }
    String returnUrl = stripTrailingSlash(frontendUrl) + "/billing";
    try {
      return stripeGateway.createBillingPortalSession(
          subscription.getStripeCustomerId(), returnUrl);
    } catch (StripeException e) {
      log.error(
          "Falha ao criar portal do Stripe: tenantId={} error={}",
          LogSanitizer.maskId(tenantId),
          e.getMessage());
      throw new BusinessException("Não foi possível abrir a gestão de assinatura.");
    }
  }

  @Override
  public Optional<Subscription> activateFromCheckout(
      String stripeSubscriptionId,
      String stripeCustomerId,
      String externalReference,
      String stripePriceId) {
    if (!hasText(externalReference)) {
      log.warn(
          "checkout.session.completed sem client_reference_id; impossível identificar o tenant.");
      return Optional.empty();
    }
    Optional<Tenant> tenantOptional = tenantRepository.findById(externalReference);
    if (tenantOptional.isEmpty()) {
      log.warn(
          "Tenant do checkout não encontrado: externalReference={}",
          LogSanitizer.maskId(externalReference));
      return Optional.empty();
    }

    Tenant tenant = tenantOptional.get();
    String planTier =
        Optional.ofNullable(stripeProperties.tierForPrice(stripePriceId))
            .orElse(tenant.getPlanId());

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    Subscription subscription = createSubscription(tenant.getId(), planTier);
    if (hasText(stripeSubscriptionId)) {
      subscription.setStripeSubscriptionId(stripeSubscriptionId);
    }
    if (hasText(stripeCustomerId)) {
      subscription.setStripeCustomerId(stripeCustomerId);
    }
    subscription.setLastPaymentDate(today);
    subscription.setUpdatedAt(now);
    Subscription saved = subscriptionRepository.save(subscription);

    activateTenant(tenant, now);

    log.info(
        "Assinatura ativada via checkout: tenantId={} subscriptionId={}",
        LogSanitizer.maskId(tenant.getId()),
        LogSanitizer.maskId(stripeSubscriptionId));
    return Optional.of(saved);
  }

  @Override
  public Subscription manuallyActivate(String tenantId) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

    Subscription subscription = createSubscription(tenantId, tenant.getPlanId());
    subscription.setLastPaymentDate(LocalDate.now(ZoneId.systemDefault()));
    subscription.setUpdatedAt(now);
    Subscription saved = subscriptionRepository.save(subscription);

    activateTenant(tenant, now);

    log.info("Assinatura ativada manualmente: tenantId={}", LogSanitizer.maskId(tenantId));
    return saved;
  }

  private void unblockTenant(String tenantId, LocalDateTime now) {
    tenantRepository
        .findById(tenantId)
        .ifPresent(
            tenant -> {
              if (tenant.getStatus() == TenantStatus.BLOCKED) {
                tenant.setStatus(TenantStatus.ACTIVE);
                tenant.setBlockedAt(null);
                tenant.setBlockReason(null);
                tenant.setUpdatedAt(now);
                tenantRepository.save(tenant);
              }
            });
  }

  private void activateTenant(Tenant tenant, LocalDateTime now) {
    if (tenant.getStatus() == TenantStatus.ACTIVE) {
      return;
    }
    tenant.setStatus(TenantStatus.ACTIVE);
    tenant.setBlockedAt(null);
    tenant.setBlockReason(null);
    tenant.setUpdatedAt(now);
    tenantRepository.save(tenant);
  }

  private String stripTrailingSlash(String value) {
    if (value == null) {
      return "";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private Optional<Invoice> findInvoiceForPaidWebhook(String stripeInvoiceId, String tenantId) {
    if (hasText(stripeInvoiceId)) {
      Optional<Invoice> byStripeId = invoiceRepository.findByStripeInvoiceId(stripeInvoiceId);
      if (byStripeId.isPresent()) {
        return byStripeId;
      }
    }
    if (hasText(tenantId)) {
      return findLatestInvoiceByTenantAndStatuses(
          tenantId, InvoiceStatus.PENDING, InvoiceStatus.OVERDUE);
    }
    return Optional.empty();
  }

  private Optional<Invoice> findLatestInvoiceByTenantAndStatuses(
      String tenantId, InvoiceStatus... statuses) {
    Set<InvoiceStatus> acceptedStatuses = Set.of(statuses);
    return invoiceRepository.findAllByTenantIdOrderByDueDateDesc(tenantId).stream()
        .filter(invoice -> acceptedStatuses.contains(invoice.getStatus()))
        .findFirst();
  }

  private String buildDescription(
      String planTier,
      LocalDate referenceDate,
      int extraProducts,
      int extraSales,
      int extraOperators) {
    StringBuilder description =
        new StringBuilder(
            "Assinatura "
                + capitalize(planTier)
                + " "
                + MONTH_NAMES.get(referenceDate.getMonthValue() - 1)
                + "/"
                + referenceDate.getYear());

    StringBuilder extras = new StringBuilder();
    appendExtra(extras, extraProducts, "produto");
    appendExtra(extras, extraSales, "venda");
    appendExtra(extras, extraOperators, "operador");
    if (!extras.isEmpty()) {
      description.append(" + ").append(extras);
    }
    return description.toString();
  }

  private void appendExtra(StringBuilder extras, int quantity, String label) {
    if (quantity <= 0) {
      return;
    }
    if (!extras.isEmpty()) {
      extras.append(", ");
    }
    extras.append(quantity).append(' ').append(label).append(quantity > 1 ? "s extras" : " extra");
  }

  private String capitalize(String value) {
    if (!hasText(value)) {
      return value;
    }
    return value.substring(0, 1).toUpperCase(Locale.ROOT)
        + value.substring(1).toLowerCase(Locale.ROOT);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
