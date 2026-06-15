package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.utils.Enum.SubscriptionStatus;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepositoryPort {

  Subscription save(Subscription subscription);

  Optional<Subscription> findById(String id);

  Optional<Subscription> findByTenantId(String tenantId);

  Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

  Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

  List<Subscription> findAllByStatus(SubscriptionStatus status);
}
