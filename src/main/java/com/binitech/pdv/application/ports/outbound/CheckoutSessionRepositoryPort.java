package com.binitech.pdv.application.ports.outbound;

import java.util.Optional;

public interface CheckoutSessionRepositoryPort {
  void save(String checkoutId, String tenantId);

  Optional<String> findTenantId(String checkoutId);
}
