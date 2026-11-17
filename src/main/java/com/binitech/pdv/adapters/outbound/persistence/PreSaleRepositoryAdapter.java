package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.PreSaleCounterDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.PreSaleDocument;
import com.binitech.pdv.application.ports.outbound.PreSaleRepositoryPort;
import com.binitech.pdv.domain.PreSale;
import com.binitech.pdv.domain.PreSaleItem;
import com.binitech.pdv.utils.enums.PreSaleStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class PreSaleRepositoryAdapter implements PreSaleRepositoryPort {

  private static final String TENANT_ID = "tenantId";
  private static final String STATUS = "status";
  private static final String UPDATED_AT = "updatedAt";

  private final MongoTemplate mongo;

  public PreSaleRepositoryAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public PreSale save(PreSale preSale) {
    return toDomain(mongo.save(toDocument(preSale)));
  }

  @Override
  public long nextNumber(String tenantId) {
    PreSaleCounterDocument counter =
        mongo.findAndModify(
            Query.query(Criteria.where("_id").is(tenantId)),
            new Update().inc("lastNumber", 1),
            FindAndModifyOptions.options().upsert(true).returnNew(true),
            PreSaleCounterDocument.class);
    return counter.getLastNumber();
  }

  @Override
  public Optional<PreSale> findByIdAndTenantId(String id, String tenantId) {
    return Optional.ofNullable(mongo.findOne(byIdAndTenant(id, tenantId), PreSaleDocument.class))
        .map(PreSaleRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<PreSale> findByNumber(String tenantId, long number) {
    Query query = Query.query(Criteria.where(TENANT_ID).is(tenantId).and("number").is(number));
    return Optional.ofNullable(mongo.findOne(query, PreSaleDocument.class))
        .map(PreSaleRepositoryAdapter::toDomain);
  }

  @Override
  public List<PreSale> findRecent(String tenantId, PreSaleStatus status, int limit) {
    Criteria criteria = Criteria.where(TENANT_ID).is(tenantId);
    if (status != null) {
      criteria = criteria.and(STATUS).is(status.name());
    }
    Query query =
        Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(limit);
    return mongo.find(query, PreSaleDocument.class).stream()
        .map(PreSaleRepositoryAdapter::toDomain)
        .toList();
  }

  @Override
  public boolean close(String id, String tenantId, PreSaleStatus target, Instant at) {
    Query query =
        byIdAndTenant(id, tenantId)
            .addCriteria(Criteria.where(STATUS).is(PreSaleStatus.OPEN.name()));
    Update update = new Update().set(STATUS, target.name()).set("closedAt", at).set(UPDATED_AT, at);
    return mongo.updateFirst(query, update, PreSaleDocument.class).getModifiedCount() == 1;
  }

  @Override
  public boolean reopen(String id, String tenantId) {
    Query query =
        byIdAndTenant(id, tenantId)
            .addCriteria(Criteria.where(STATUS).is(PreSaleStatus.PAID.name()))
            .addCriteria(Criteria.where("saleId").is(null));
    Update update =
        new Update()
            .set(STATUS, PreSaleStatus.OPEN.name())
            .unset("closedAt")
            .set(UPDATED_AT, Instant.now());
    return mongo.updateFirst(query, update, PreSaleDocument.class).getModifiedCount() == 1;
  }

  @Override
  public void attachSale(String id, String tenantId, String saleId) {
    mongo.updateFirst(
        byIdAndTenant(id, tenantId),
        new Update().set("saleId", saleId).set(UPDATED_AT, Instant.now()),
        PreSaleDocument.class);
  }

  private static Query byIdAndTenant(String id, String tenantId) {
    return Query.query(Criteria.where("_id").is(id).and(TENANT_ID).is(tenantId));
  }

  private static PreSaleDocument toDocument(PreSale preSale) {
    PreSaleDocument document = new PreSaleDocument();
    document.setId(preSale.getId());
    document.setTenantId(preSale.getTenantId());
    document.setNumber(preSale.getNumber());
    document.setStatus(preSale.getStatus().name());
    document.setItems(
        preSale.getItems().stream()
            .map(
                item ->
                    new PreSaleDocument.Item(
                        item.productId(), item.description(), item.unitPrice(), item.quantity()))
            .toList());
    document.setTotal(preSale.getTotal());
    document.setNotes(preSale.getNotes());
    document.setAttendantId(preSale.getAttendantId());
    document.setAttendantName(preSale.getAttendantName());
    document.setSaleId(preSale.getSaleId());
    document.setCreatedAt(preSale.getCreatedAt());
    document.setUpdatedAt(preSale.getUpdatedAt());
    document.setClosedAt(preSale.getClosedAt());
    return document;
  }

  private static PreSale toDomain(PreSaleDocument document) {
    PreSale preSale = new PreSale();
    preSale.setId(document.getId());
    preSale.setTenantId(document.getTenantId());
    preSale.setNumber(document.getNumber());
    preSale.setStatus(PreSaleStatus.valueOf(document.getStatus()));
    preSale.setItems(
        document.getItems() == null
            ? List.of()
            : document.getItems().stream()
                .map(
                    item ->
                        new PreSaleItem(
                            item.getProductId(),
                            item.getDescription(),
                            item.getUnitPrice(),
                            item.getQuantity()))
                .toList());
    preSale.setTotal(document.getTotal());
    preSale.setNotes(document.getNotes());
    preSale.setAttendantId(document.getAttendantId());
    preSale.setAttendantName(document.getAttendantName());
    preSale.setSaleId(document.getSaleId());
    preSale.setCreatedAt(document.getCreatedAt());
    preSale.setUpdatedAt(document.getUpdatedAt());
    preSale.setClosedAt(document.getClosedAt());
    return preSale;
  }
}
