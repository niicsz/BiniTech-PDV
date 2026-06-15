package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.SubscriptionDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataSubscriptionRepository;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.utils.Enum.SubscriptionStatus;
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
  public Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
    return repository.findByStripeSubscriptionId(stripeSubscriptionId).map(this::toDomain);
  }

  @Override
  public Optional<Subscription> findByStripeCustomerId(String stripeCustomerId) {
    return repository.findByStripeCustomerId(stripeCustomerId).map(this::toDomain);
  }

  @Override
  public List<Subscription> findAllByStatus(SubscriptionStatus status) {
    return repository.findAllByStatus(status.name()).stream().map(this::toDomain).toList();
  }

  private SubscriptionDocument toDocument(Subscription s) {
    return SubscriptionDocument.builder()
        .id(s.getId())
        .tenantId(s.getTenantId())
        .stripeSubscriptionId(s.getStripeSubscriptionId())
        .stripeCustomerId(s.getStripeCustomerId())
        .stripePriceId(s.getStripePriceId())
        .planTier(s.getPlanTier())
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
    return new Subscription(
        d.getId(),
        d.getTenantId(),
        d.getStripeSubscriptionId(),
        d.getStripeCustomerId(),
        d.getStripePriceId(),
        d.getPlanTier(),
        d.getStatus() != null ? SubscriptionStatus.valueOf(d.getStatus()) : null,
        d.getCurrentPeriodStart(),
        d.getCurrentPeriodEnd(),
        d.getNextBillingDate(),
        d.getLastPaymentDate(),
        d.getFailedPaymentCount(),
        d.getCancelledAt(),
        d.getCreatedAt(),
        d.getUpdatedAt());
  }
}
