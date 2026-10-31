package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.CheckoutSessionDocument;
import com.binitech.pdv.application.ports.outbound.CheckoutSessionRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class CheckoutSessionRepositoryAdapter implements CheckoutSessionRepositoryPort {

  private final MongoTemplate mongo;

  public CheckoutSessionRepositoryAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public void save(String checkoutId, String tenantId) {
    save(checkoutId, tenantId, null);
  }

  @Override
  public void save(String checkoutId, String tenantId, String billingType) {
    mongo.save(
        CheckoutSessionDocument.builder()
            .id(checkoutId)
            .tenantId(tenantId)
            .billingType(billingType)
            .createdAt(Instant.now())
            .build());
  }

  @Override
  public Optional<String> findTenantId(String checkoutId) {
    return find(checkoutId).map(CheckoutSessionDocument::getTenantId);
  }

  @Override
  public Optional<String> findBillingType(String checkoutId) {
    return find(checkoutId).map(CheckoutSessionDocument::getBillingType);
  }

  @Override
  public boolean markPaid(String checkoutId) {
    Query unpaid = Query.query(Criteria.where("_id").is(checkoutId).and("paidAt").isNull());
    return mongo
            .updateFirst(
                unpaid, Update.update("paidAt", Instant.now()), CheckoutSessionDocument.class)
            .getModifiedCount()
        > 0;
  }

  private Optional<CheckoutSessionDocument> find(String checkoutId) {
    return Optional.ofNullable(mongo.findById(checkoutId, CheckoutSessionDocument.class));
  }
}
