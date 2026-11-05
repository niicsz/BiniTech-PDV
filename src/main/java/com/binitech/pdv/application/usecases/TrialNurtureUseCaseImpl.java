package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.TrialNurtureUseCasePort;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.PlanConfig;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.TrialEmail;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrialNurtureUseCaseImpl implements TrialNurtureUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(TrialNurtureUseCaseImpl.class);
  private static final int FIRST_SALE_DAY = 1;
  private static final int FEATURES_DAY = 3;
  private static final int ENDED_WINDOW_DAYS = 7;

  private final TenantRepositoryPort tenantRepository;
  private final SubscriptionRepositoryPort subscriptionRepository;
  private final SaleRepositoryPort saleRepository;
  private final EmailServicePort emailService;

  public TrialNurtureUseCaseImpl(
      TenantRepositoryPort tenantRepository,
      SubscriptionRepositoryPort subscriptionRepository,
      SaleRepositoryPort saleRepository,
      EmailServicePort emailService) {
    this.tenantRepository = tenantRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.saleRepository = saleRepository;
    this.emailService = emailService;
  }

  @Override
  public int sendDueEmails(LocalDate today) {
    int sent = 0;
    for (Tenant tenant : tenantRepository.findAllByStatus(TenantStatus.ACTIVE)) {
      try {
        if (process(tenant, today)) {
          sent++;
        }
      } catch (RuntimeException e) {
        log.error(
            "Falha no e-mail do teste grátis: tenantId={} error={}",
            LogSanitizer.maskId(tenant.getId()),
            e.getMessage());
      }
    }
    return sent;
  }

  private boolean process(Tenant tenant, LocalDate today) {
    if (!eligible(tenant)) {
      return false;
    }
    Optional<TrialEmail> due = nextDue(tenant, today);
    if (due.isEmpty()) {
      return false;
    }
    TrialEmail step = due.get();
    tenant.getTrialEmailsSent().add(step.name());
    tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    tenantRepository.save(tenant);
    emailService.sendTrialEmail(tenant.getBillingEmail(), tenant.getName(), step);
    if (log.isInfoEnabled()) {
      log.info(
          "E-mail do teste grátis enviado: tenantId={} etapa={}",
          LogSanitizer.maskId(tenant.getId()),
          step);
    }
    return true;
  }

  private boolean eligible(Tenant tenant) {
    return tenant.getTrialEndsAt() != null
        && tenant.getCreatedAt() != null
        && tenant.getBillingEmail() != null
        && !tenant.getBillingEmail().isBlank()
        && !PlanConfig.isFree(tenant.getPlanId())
        && !paid(tenant.getId());
  }

  private Optional<TrialEmail> nextDue(Tenant tenant, LocalDate today) {
    LocalDate created = tenant.getCreatedAt().toLocalDate();
    LocalDate trialEnd = tenant.getTrialEndsAt();
    if (today.isAfter(trialEnd)) {
      boolean recent = !today.isAfter(trialEnd.plusDays(ENDED_WINDOW_DAYS));
      return recent && !sent(tenant, TrialEmail.ENDED)
          ? Optional.of(TrialEmail.ENDED)
          : Optional.empty();
    }
    if (!today.isBefore(trialEnd.minusDays(1))) {
      return sent(tenant, TrialEmail.ENDING) ? Optional.empty() : Optional.of(TrialEmail.ENDING);
    }
    if (!sent(tenant, TrialEmail.FIRST_SALE)
        && !today.isBefore(created.plusDays(FIRST_SALE_DAY))
        && !hasSales(tenant)) {
      return Optional.of(TrialEmail.FIRST_SALE);
    }
    if (!sent(tenant, TrialEmail.FEATURES) && !today.isBefore(created.plusDays(FEATURES_DAY))) {
      return Optional.of(TrialEmail.FEATURES);
    }
    return Optional.empty();
  }

  private static boolean sent(Tenant tenant, TrialEmail step) {
    return tenant.getTrialEmailsSent().contains(step.name());
  }

  private boolean hasSales(Tenant tenant) {
    return saleRepository.countByTenantIdAndTimestampBetween(
            tenant.getId(), tenant.getCreatedAt(), LocalDateTime.now(ZoneId.systemDefault()))
        > 0;
  }

  private boolean paid(String tenantId) {
    return subscriptionRepository
        .findByTenantId(tenantId)
        .map(
            s ->
                s.getStatus() == SubscriptionStatus.ACTIVE
                    || s.getStatus() == SubscriptionStatus.PAST_DUE)
        .orElse(false);
  }
}
