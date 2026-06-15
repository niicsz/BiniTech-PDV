package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.TenantDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataTenantRepository;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.utils.Enum.TenantStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TenantRepositoryAdapter implements TenantRepositoryPort {

  private final SpringDataTenantRepository repository;

  public TenantRepositoryAdapter(SpringDataTenantRepository repository) {
    this.repository = repository;
  }

  @Override
  public Tenant save(Tenant tenant) {
    return toDomain(repository.save(toDocument(tenant)));
  }

  @Override
  public Optional<Tenant> findById(String id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Tenant> findBySlug(String slug) {
    return repository.findBySlug(slug).map(this::toDomain);
  }

  @Override
  public Optional<Tenant> findByBillingEmail(String billingEmail) {
    return repository.findByBillingEmail(billingEmail).map(this::toDomain);
  }

  @Override
  public boolean existsBySlug(String slug) {
    return repository.existsBySlug(slug);
  }

  @Override
  public List<Tenant> findAll() {
    return repository.findAll().stream().map(this::toDomain).toList();
  }

  @Override
  public List<Tenant> findAllByStatus(TenantStatus status) {
    return repository.findAllByStatus(status.name()).stream().map(this::toDomain).toList();
  }

  private TenantDocument toDocument(Tenant tenant) {
    return TenantDocument.builder()
        .id(tenant.getId())
        .slug(tenant.getSlug())
        .name(tenant.getName())
        .status(tenant.getStatus() != null ? tenant.getStatus().name() : null)
        .planId(tenant.getPlanId())
        .billingEmail(tenant.getBillingEmail())
        .trialEndsAt(tenant.getTrialEndsAt())
        .blockedAt(tenant.getBlockedAt())
        .blockReason(tenant.getBlockReason())
        .createdAt(tenant.getCreatedAt())
        .updatedAt(tenant.getUpdatedAt())
        .build();
  }

  private Tenant toDomain(TenantDocument doc) {
    return new Tenant(
        doc.getId(),
        doc.getName(),
        doc.getSlug(),
        doc.getStatus() != null ? TenantStatus.valueOf(doc.getStatus()) : null,
        doc.getPlanId(),
        doc.getBillingEmail(),
        doc.getTrialEndsAt(),
        doc.getBlockedAt(),
        doc.getBlockReason(),
        doc.getCreatedAt(),
        doc.getUpdatedAt());
  }
}
