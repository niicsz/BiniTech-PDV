package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.SubscriptionDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataSubscriptionRepository;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {

  private final SpringDataSubscriptionRepository repository;

  public SubscriptionRepositoryAdapter(SpringDataSubscriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Subscription save(Subscription subscription) {
    return toDomain(repository.save(toDocument(subscription)));
  }

  @Override
  public Optional<Subscription> findById(String id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Subscription> findByTenantId(String tenantId) {
    return repository.findByTenantId(tenantId).map(this::toDomain);
  }

  @Override
  public Optional<Subscription> findByAsaasSubscriptionId(String asaasSubscriptionId) {
    return repository.findByAsaasSubscriptionId(asaasSubscriptionId).map(this::toDomain);
  }

  @Override
  public Optional<Subscription> findByAsaasCustomerId(String asaasCustomerId) {
    return repository.findByAsaasCustomerId(asaasCustomerId).map(this::toDomain);
  }

  @Override
  public List<Subscription> findAllByStatus(SubscriptionStatus status) {
    return repository.findAllByStatus(status.name()).stream().map(this::toDomain).toList();
  }

  private SubscriptionDocument toDocument(Subscription s) {
    return SubscriptionDocument.builder()
        .id(s.getId())
        .tenantId(s.getTenantId())
        .asaasSubscriptionId(s.getAsaasSubscriptionId())
        .asaasCustomerId(s.getAsaasCustomerId())
        .planTier(s.getPlanTier())
        .billingType(s.getBillingType())
        .status(s.getStatus() != null ? s.getStatus().name() : null)
        .currentPeriodStart(s.getCurrentPeriodStart())
        .currentPeriodEnd(s.getCurrentPeriodEnd())
        .nextBillingDate(s.getNextBillingDate())
        .lastPaymentDate(s.getLastPaymentDate())
        .failedPaymentCount(s.getFailedPaymentCount())
        .cancelledAt(s.getCancelledAt())
        .createdAt(s.getCreatedAt())
        .updatedAt(s.getUpdatedAt())
        .build();
  }

  private Subscription toDomain(SubscriptionDocument d) {
    Subscription subscription = new Subscription();
    subscription.setId(d.getId());
    subscription.setTenantId(d.getTenantId());
    subscription.setAsaasSubscriptionId(d.getAsaasSubscriptionId());
    subscription.setAsaasCustomerId(d.getAsaasCustomerId());
    subscription.setPlanTier(d.getPlanTier());
    subscription.setBillingType(d.getBillingType());
    subscription.setStatus(
        d.getStatus() != null ? SubscriptionStatus.valueOf(d.getStatus()) : null);
    subscription.setCurrentPeriodStart(d.getCurrentPeriodStart());
    subscription.setCurrentPeriodEnd(d.getCurrentPeriodEnd());
    subscription.setNextBillingDate(d.getNextBillingDate());
    subscription.setLastPaymentDate(d.getLastPaymentDate());
    subscription.setFailedPaymentCount(d.getFailedPaymentCount());
    subscription.setCancelledAt(d.getCancelledAt());
    subscription.setCreatedAt(d.getCreatedAt());
    subscription.setUpdatedAt(d.getUpdatedAt());
    return subscription;
  }
}
