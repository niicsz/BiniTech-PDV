package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.SubscriptionDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSubscriptionRepository
    extends MongoRepository<SubscriptionDocument, String> {

  Optional<SubscriptionDocument> findByTenantId(String tenantId);

  Optional<SubscriptionDocument> findByStripeSubscriptionId(String stripeSubscriptionId);

  Optional<SubscriptionDocument> findByStripeCustomerId(String stripeCustomerId);

  List<SubscriptionDocument> findAllByStatus(String status);
}
