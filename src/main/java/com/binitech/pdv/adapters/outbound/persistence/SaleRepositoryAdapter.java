package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.SaleDocument;
import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapper;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataSaleRepository;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Sale;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class SaleRepositoryAdapter implements SaleRepositoryPort {

  private static final String FIELD_TIMESTAMP = "timestamp";
  private static final String FIELD_RETURNS_VERSION = "returnsVersion";
  private static final String FIELD_RETURNS_FISCAL_PENDING = "returns.fiscalPending";

  private final SpringDataSaleRepository repository;
  private final PersistenceMapper mapper;
  private final MongoTemplate mongo;

  public SaleRepositoryAdapter(
      SpringDataSaleRepository repository, PersistenceMapper mapper, MongoTemplate mongo) {
    this.repository = repository;
    this.mapper = mapper;
    this.mongo = mongo;
  }

  @Override
  public List<Sale> findFiscalPending(int limit) {
    Query query =
        Query.query(Criteria.where("fiscalPending").is(true))
            .with(Sort.by(Sort.Direction.ASC, FIELD_TIMESTAMP))
            .limit(limit);
    return mongo.find(query, SaleDocument.class).stream().map(mapper::toDomain).toList();
  }

  @Override
  public void markFiscalPublished(String id) {
    mongo.updateFirst(
        Query.query(Criteria.where("_id").is(id)),
        Update.update("fiscalPending", false),
        SaleDocument.class);
  }

  @Override
  public List<Sale> findReturnFiscalPending(int limit) {
    Query query =
        Query.query(Criteria.where(FIELD_RETURNS_FISCAL_PENDING).is(true))
            .with(Sort.by(Sort.Direction.ASC, FIELD_TIMESTAMP))
            .limit(limit);
    return mongo.find(query, SaleDocument.class).stream().map(mapper::toDomain).toList();
  }

  @Override
  public void markReturnFiscalPublished(String saleId, String returnId) {
    mongo.updateFirst(
        Query.query(Criteria.where("_id").is(saleId).and("returns._id").is(returnId)),
        Update.update("returns.$.fiscalPending", false),
        SaleDocument.class);
  }

  @Override
  public boolean appendReturn(Sale sale, int expectedReturnsVersion) {
    Criteria version =
        expectedReturnsVersion == 0
            ? Criteria.where(FIELD_RETURNS_VERSION).in(null, 0)
            : Criteria.where(FIELD_RETURNS_VERSION).is(expectedReturnsVersion);
    Query query = Query.query(Criteria.where("_id").is(sale.getId()).andOperator(version));
    Update update =
        new Update()
            .set("returns", sale.getReturns().stream().map(mapper::toDocument).toList())
            .set("returnedAmount", sale.getReturnedAmount())
            .set("returnedCost", sale.getReturnedCost())
            .set("status", sale.getStatus().name())
            .set(FIELD_RETURNS_VERSION, expectedReturnsVersion + 1);
    return mongo.updateFirst(query, update, SaleDocument.class).getModifiedCount() == 1;
  }

  @Override
  public Sale save(Sale sale) {
    var document = mapper.toDocument(sale);
    var saved = repository.save(document);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<Sale> findById(String id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Sale> findByTimestampBetween(LocalDateTime start, LocalDateTime end) {
    return repository.findByTimestampBetween(start, end).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Sale> findAll(int page, int size) {
    return repository
        .findAll(PageRequest.of(page, size, Sort.by(FIELD_TIMESTAMP).descending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findAllByUserId(String userId, int page, int size) {
    return repository
        .findAllByUserId(userId, PageRequest.of(page, size, Sort.by(FIELD_TIMESTAMP).descending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findByTimestampBetweenAndUserId(
      LocalDateTime start, LocalDateTime end, String userId) {
    return repository.findByTimestampBetweenAndUserId(start, end, userId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findDebtors() {
    return repository.findDebtors().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Sale> findDebtorsByUserId(String userId) {
    return repository.findDebtorsByUserId(userId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Sale> findAllByTenantId(String tenantId, int page, int size) {
    return repository
        .findAllByTenantId(
            tenantId, PageRequest.of(page, size, Sort.by(FIELD_TIMESTAMP).descending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findByTimestampRangeAndTenantId(
      Instant startInclusive, Instant endExclusive, String tenantId) {
    return repository
        .findByTimestampRangeAndTenantId(startInclusive, endExclusive, tenantId)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findDebtorsByTenantId(String tenantId) {
    return repository.findDebtorsByTenantId(tenantId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public long countByTenantIdAndTimestampBetween(
      String tenantId, LocalDateTime start, LocalDateTime end) {
    return repository.countByTenantIdAndTimestampBetween(tenantId, start, end);
  }
}
