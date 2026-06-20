package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.util.List;
import java.util.Optional;

public interface TenantRepositoryPort {

  Tenant save(Tenant tenant);

  Optional<Tenant> findById(String id);

  Optional<Tenant> findBySlug(String slug);

  Optional<Tenant> findByBillingEmail(String billingEmail);

  boolean existsBySlug(String slug);

  List<Tenant> findAll();

  List<Tenant> findAllByStatus(TenantStatus status);
}
