package com.binitech.pdv.application.usecases;

import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataUserRepository;
import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.outbound.CheckoutSessionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.InvoiceRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.AsaasException;
import com.binitech.pdv.config.AsaasGateway;
import com.binitech.pdv.config.AsaasGateway.CheckoutSession;
import com.binitech.pdv.config.BillingAsaasConfig;
import com.binitech.pdv.config.PlanConfig;
import com.binitech.pdv.config.PlanConfig.PlanLimits;
import com.binitech.pdv.domain.Invoice;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.InvoiceStatus;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
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
  private static final String PAYMENT_SUPPORT_MESSAGE =
      "Entre em contato com o suporte para realizar o pagamento.";
  private static final String BILLING_TYPE_PIX = "PIX";
  private static final String BILLING_TYPE_CREDIT_CARD = "CREDIT_CARD";
  private static final int BILLING_PERIOD_DAYS = 30;
  private static final List<String> MONTH_NAMES =
      List.of("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez");

  private final SubscriptionRepositoryPort subscriptionRepository;
  private final InvoiceRepositoryPort invoiceRepository;
  private final TenantRepositoryPort tenantRepository;
  private final SpringDataProductRepository productRepository;
  private final SpringDataUserRepository userRepository;
  private final String frontendUrl;
  private final AsaasGateway asaasGateway;
  private final CheckoutSessionRepositoryPort checkoutSessionRepository;

  public BillingUseCaseImpl(
      SubscriptionRepositoryPort subscriptionRepository,
      InvoiceRepositoryPort invoiceRepository,
      TenantRepositoryPort tenantRepository,
      SpringDataProductRepository productRepository,
      SpringDataUserRepository userRepository,
      BillingAsaasConfig asaasConfig) {
    this.subscriptionRepository = subscriptionRepository;
    this.invoiceRepository = invoiceRepository;
    this.tenantRepository = tenantRepository;
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.frontendUrl = asaasConfig.frontendUrl();
    this.asaasGateway = asaasConfig.asaasGateway();
    this.checkoutSessionRepository = asaasConfig.checkoutSessionRepository();
  }

  @Override
  public Subscription createSubscription(String tenantId, String planTier) {
    PlanLimits limits = PlanConfig.getLimits(planTier);
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    Subscription subscription =
        subscriptionRepository.findByTenantId(tenantId).orElseGet(Subscription::new);

    subscription.setTenantId(tenantId);
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
        operators >= 0
            ? operators
            : (int) userRepository.countByTenantIdAndRole(tenantId, "OPERATOR");
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
  public Invoice markInvoicePaid(String asaasPaymentId, String asaasSubscriptionId) {
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

    Optional<Subscription> subscriptionOptional =
        hasText(asaasSubscriptionId)
            ? subscriptionRepository.findByAsaasSubscriptionId(asaasSubscriptionId)
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
            asaasPaymentId, subscriptionOptional.map(Subscription::getTenantId).orElse(null));
    if (invoiceOptional.isPresent()) {
      Invoice invoice = invoiceOptional.get();
      if (hasText(asaasPaymentId)) {
        invoice.setAsaasPaymentId(asaasPaymentId);
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
          LogSanitizer.maskId(asaasPaymentId),
          LogSanitizer.maskId(asaasSubscriptionId));
    }
    return savedInvoice;
  }

  @Override
  public void recordPaymentFailure(String asaasSubscriptionId) {
    Subscription subscription =
        subscriptionRepository
            .findByAsaasSubscriptionId(asaasSubscriptionId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Subscription", "asaasSubscriptionId", asaasSubscriptionId));
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
        LogSanitizer.maskId(asaasSubscriptionId),
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
    Optional<Subscription> existing = subscriptionRepository.findByTenantId(tenantId);
    if (existing.isPresent()) {
      return existing;
    }
    return tenantRepository
        .findById(tenantId)
        .filter(tenant -> hasText(tenant.getPlanId()))
        .map(this::pendingSubscriptionFor);
  }

  private Subscription pendingSubscriptionFor(Tenant tenant) {
    Subscription pending = new Subscription();
    pending.setTenantId(tenant.getId());
    pending.setPlanTier(tenant.getPlanId().toLowerCase(Locale.ROOT));
    pending.setStatus(
        PlanConfig.isFree(tenant.getPlanId())
            ? SubscriptionStatus.ACTIVE
            : SubscriptionStatus.PENDING);
    pending.setCreatedAt(tenant.getCreatedAt());
    pending.setUpdatedAt(tenant.getUpdatedAt());
    return pending;
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
    if (!asaasGateway.isConfigured()) {
      throw new BusinessException(
          "Pagamento online indisponível no momento. " + PAYMENT_SUPPORT_MESSAGE);
    }
    PlanLimits limits = PlanConfig.getLimits(tenant.getPlanId());
    String baseUrl = stripTrailingSlash(frontendUrl);
    String successUrl = baseUrl + "/billing?status=success";
    String cancelUrl = baseUrl + "/billing";
    try {
      CheckoutSession checkout =
          asaasGateway.createSubscriptionCheckout(
              tenant,
              capitalize(tenant.getPlanId()),
              limits.baseMonthlyFee(),
              successUrl,
              cancelUrl);
      checkoutSessionRepository.save(checkout.id(), tenant.getId());
      return checkout.url();
    } catch (AsaasException e) {
      log.error(
          "Falha ao criar checkout no Asaas: tenantId={} error={}",
          LogSanitizer.maskId(tenantId),
          e.getMessage());
      throw new BusinessException(
          "Não foi possível iniciar o pagamento. " + PAYMENT_SUPPORT_MESSAGE);
    }
  }

  @Override
  public String createPixCheckoutUrl(String tenantId) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    if (PlanConfig.isFree(tenant.getPlanId())) {
      throw new BusinessException("Plano cortesia não requer pagamento.");
    }
    if (!asaasGateway.isConfigured()) {
      throw new BusinessException(
          "Pagamento via Pix indisponível no momento. " + PAYMENT_SUPPORT_MESSAGE);
    }
    BigDecimal amount =
        findLatestInvoiceByTenantAndStatuses(tenantId, InvoiceStatus.OVERDUE, InvoiceStatus.PENDING)
            .map(Invoice::getAmount)
            .orElseGet(() -> PlanConfig.getLimits(tenant.getPlanId()).baseMonthlyFee());
    String baseUrl = stripTrailingSlash(frontendUrl);
    try {
      CheckoutSession checkout =
          asaasGateway.createPixCheckout(
              tenant,
              capitalize(tenant.getPlanId()),
              amount,
              baseUrl + "/billing?status=success",
              baseUrl + "/billing");
      checkoutSessionRepository.save(checkout.id(), tenant.getId(), BILLING_TYPE_PIX);
      return checkout.url();
    } catch (AsaasException e) {
      log.error(
          "Falha ao criar checkout Pix no Asaas: tenantId={} error={}",
          LogSanitizer.maskId(tenantId),
          e.getMessage());
      throw new BusinessException("Não foi possível gerar o Pix. " + PAYMENT_SUPPORT_MESSAGE);
    }
  }

  @Override
  public void blockExpiredPixSubscriptions(int graceDays) {
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDate cutoffDate = today.minusDays(graceDays);
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    for (Subscription subscription :
        subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE)) {
      if (!BILLING_TYPE_PIX.equals(subscription.getBillingType())
          || subscription.getCurrentPeriodEnd() == null
          || !subscription.getCurrentPeriodEnd().isBefore(cutoffDate)) {
        continue;
      }
      subscription.setStatus(SubscriptionStatus.PAST_DUE);
      subscription.setUpdatedAt(now);
      subscriptionRepository.save(subscription);
      tenantRepository
          .findById(subscription.getTenantId())
          .filter(tenant -> tenant.getStatus() == TenantStatus.ACTIVE)
          .ifPresent(
              tenant -> {
                tenant.setStatus(TenantStatus.BLOCKED);
                tenant.setBlockedAt(today);
                tenant.setBlockReason("Mensalidade via Pix vencida");
                tenant.setUpdatedAt(now);
                tenantRepository.save(tenant);
                log.warn(
                    "Tenant bloqueado por mensalidade Pix vencida: tenantId={}",
                    LogSanitizer.maskId(tenant.getId()));
              });
    }
  }

  @Override
  public String createPortalUrl(String tenantId) {
    Subscription subscription =
        subscriptionRepository
            .findByTenantId(tenantId)
            .orElseThrow(
                () -> new BusinessException("Nenhuma assinatura encontrada para gerenciar."));
    if (!hasText(subscription.getAsaasSubscriptionId())) {
      throw new BusinessException(
          "Assinatura ainda não vinculada ao pagamento online. " + PAYMENT_SUPPORT_MESSAGE);
    }
    if (!asaasGateway.isConfigured()) {
      throw new BusinessException(
          "Gestão de assinatura indisponível no momento. " + PAYMENT_SUPPORT_MESSAGE);
    }
    Optional<String> invoiceUrl;
    try {
      invoiceUrl = asaasGateway.findOpenPaymentUrl(subscription.getAsaasSubscriptionId());
    } catch (AsaasException e) {
      log.error(
          "Falha ao consultar faturas no Asaas: tenantId={} error={}",
          LogSanitizer.maskId(tenantId),
          e.getMessage());
      throw new BusinessException(
          "Não foi possível abrir a fatura da assinatura. " + PAYMENT_SUPPORT_MESSAGE);
    }
    return invoiceUrl.orElseThrow(
        () -> new BusinessException("Nenhuma fatura em aberto no momento."));
  }

  @Override
  public void confirmSubscriptionPayment(
      String asaasPaymentId,
      String asaasSubscriptionId,
      String asaasCustomerId,
      String checkoutSessionId) {
    boolean knownSubscription =
        hasText(asaasSubscriptionId)
            && subscriptionRepository.findByAsaasSubscriptionId(asaasSubscriptionId).isPresent();
    if (!knownSubscription && hasText(checkoutSessionId)) {
      activateFromCheckoutSession(checkoutSessionId, asaasSubscriptionId, asaasCustomerId, null);
      return;
    }
    markInvoicePaid(asaasPaymentId, asaasSubscriptionId);
  }

  @Override
  public Optional<Subscription> activateFromCheckoutSession(
      String checkoutSessionId,
      String asaasSubscriptionId,
      String asaasCustomerId,
      String externalReference) {
    if (isPixCheckout(checkoutSessionId)) {
      return activateFromPixCheckout(checkoutSessionId);
    }
    if (hasText(asaasSubscriptionId)) {
      Optional<Subscription> existing =
          subscriptionRepository.findByAsaasSubscriptionId(asaasSubscriptionId);
      if (existing.isPresent()) {
        return existing;
      }
    }
    String tenantId =
        hasText(externalReference)
            ? externalReference
            : Optional.ofNullable(checkoutSessionId)
                .flatMap(checkoutSessionRepository::findTenantId)
                .orElse(null);
    return activateFromCheckout(asaasSubscriptionId, asaasCustomerId, tenantId);
  }

  private boolean isPixCheckout(String checkoutSessionId) {
    return hasText(checkoutSessionId)
        && checkoutSessionRepository
            .findBillingType(checkoutSessionId)
            .filter(BILLING_TYPE_PIX::equals)
            .isPresent();
  }

  private Optional<Subscription> activateFromPixCheckout(String checkoutSessionId) {
    Optional<Tenant> tenantOptional =
        checkoutSessionRepository
            .findTenantId(checkoutSessionId)
            .flatMap(tenantRepository::findById);
    if (tenantOptional.isEmpty()) {
      log.warn("Checkout Pix pago sem tenant identificável; ativação ignorada.");
      return Optional.empty();
    }
    Tenant tenant = tenantOptional.get();
    if (!checkoutSessionRepository.markPaid(checkoutSessionId)) {
      return subscriptionRepository.findByTenantId(tenant.getId());
    }

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    Subscription subscription =
        subscriptionRepository.findByTenantId(tenant.getId()).orElseGet(Subscription::new);
    LocalDate periodEnd = subscription.getCurrentPeriodEnd();
    LocalDate periodStart =
        subscription.getStatus() == SubscriptionStatus.ACTIVE
                && periodEnd != null
                && periodEnd.isAfter(today)
            ? periodEnd
            : today;

    subscription.setTenantId(tenant.getId());
    subscription.setPlanTier(tenant.getPlanId().toLowerCase(Locale.ROOT));
    subscription.setBillingType(BILLING_TYPE_PIX);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setCurrentPeriodStart(periodStart);
    subscription.setCurrentPeriodEnd(periodStart.plusDays(BILLING_PERIOD_DAYS));
    subscription.setNextBillingDate(periodStart.plusDays(BILLING_PERIOD_DAYS));
    subscription.setLastPaymentDate(today);
    subscription.setFailedPaymentCount(0);
    subscription.setCancelledAt(null);
    if (subscription.getCreatedAt() == null) {
      subscription.setCreatedAt(now);
    }
    subscription.setUpdatedAt(now);
    Subscription saved = subscriptionRepository.save(subscription);

    settleOpenInvoices(tenant.getId(), today, now);
    activateTenant(tenant, now);

    log.info(
        "Mensalidade paga via Pix: tenantId={} validaAte={}",
        LogSanitizer.maskId(tenant.getId()),
        saved.getCurrentPeriodEnd());
    return Optional.of(saved);
  }

  @Override
  public Optional<Subscription> activateFromCheckout(
      String asaasSubscriptionId, String asaasCustomerId, String externalReference) {
    if (!hasText(externalReference)) {
      log.warn("Checkout pago sem tenant identificável; ativação ignorada.");
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
    String planTier = tenant.getPlanId();

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    Subscription subscription = createSubscription(tenant.getId(), planTier);
    if (hasText(asaasSubscriptionId)) {
      subscription.setAsaasSubscriptionId(asaasSubscriptionId);
    }
    if (hasText(asaasCustomerId)) {
      subscription.setAsaasCustomerId(asaasCustomerId);
    }
    subscription.setBillingType(BILLING_TYPE_CREDIT_CARD);
    subscription.setLastPaymentDate(today);
    subscription.setUpdatedAt(now);
    Subscription saved = subscriptionRepository.save(subscription);

    activateTenant(tenant, now);

    log.info(
        "Assinatura ativada via checkout: tenantId={} subscriptionId={}",
        LogSanitizer.maskId(tenant.getId()),
        LogSanitizer.maskId(asaasSubscriptionId));
    return Optional.of(saved);
  }

  @Override
  public Subscription manuallyActivate(String tenantId, PaymentMethod paymentMethod) {
    if (paymentMethod != PaymentMethod.CASH && paymentMethod != PaymentMethod.PIX) {
      throw new BusinessException(
          "A liberação manual aceita somente pagamento em dinheiro ou Pix.");
    }
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    Subscription subscription = createSubscription(tenantId, tenant.getPlanId());
    subscription.setBillingType(null);
    subscription.setLastPaymentDate(today);
    subscription.setUpdatedAt(now);
    Subscription saved = subscriptionRepository.save(subscription);

    settleOpenInvoices(tenantId, today, now);
    activateTenant(tenant, now);

    log.info(
        "Assinatura ativada manualmente: tenantId={} paymentMethod={}",
        LogSanitizer.maskId(tenantId),
        paymentMethod);
    return saved;
  }

  private void settleOpenInvoices(String tenantId, LocalDate today, LocalDateTime now) {
    invoiceRepository.findAllByTenantIdOrderByDueDateDesc(tenantId).stream()
        .filter(
            invoice ->
                invoice.getStatus() == InvoiceStatus.PENDING
                    || invoice.getStatus() == InvoiceStatus.OVERDUE)
        .forEach(
            invoice -> {
              invoice.setStatus(InvoiceStatus.PAID);
              invoice.setPaidAt(today);
              invoice.setUpdatedAt(now);
              invoiceRepository.save(invoice);
            });
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

  private Optional<Invoice> findInvoiceForPaidWebhook(String asaasPaymentId, String tenantId) {
    if (hasText(asaasPaymentId)) {
      Optional<Invoice> byPaymentId = invoiceRepository.findByAsaasPaymentId(asaasPaymentId);
      if (byPaymentId.isPresent()) {
        return byPaymentId;
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
