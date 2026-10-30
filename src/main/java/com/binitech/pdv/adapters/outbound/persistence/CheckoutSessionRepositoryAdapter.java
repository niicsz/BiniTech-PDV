package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.CheckoutSessionDocument;
import com.binitech.pdv.application.ports.outbound.CheckoutSessionRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class CheckoutSessionRepositoryAdapter implements CheckoutSessionRepositoryPort {

  private final MongoTemplate mongo;

  public CheckoutSessionRepositoryAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public void save(String checkoutId, String tenantId) {
    mongo.save(
        CheckoutSessionDocument.builder()
            .id(checkoutId)
            .tenantId(tenantId)
            .createdAt(Instant.now())
            .build());
  }

  @Override
  public Optional<String> findTenantId(String checkoutId) {
    return Optional.ofNullable(mongo.findById(checkoutId, CheckoutSessionDocument.class))
        .map(CheckoutSessionDocument::getTenantId);
  }
}
