package com.binitech.pdv.application.ports.outbound;

import java.util.Optional;

public interface CheckoutSessionRepositoryPort {
  void save(String checkoutId, String tenantId);

  void save(String checkoutId, String tenantId, String billingType);

  Optional<String> findTenantId(String checkoutId);

  Optional<String> findBillingType(String checkoutId);

  boolean markPaid(String checkoutId);
}
