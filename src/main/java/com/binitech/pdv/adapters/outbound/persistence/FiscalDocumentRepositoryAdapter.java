package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.FiscalDocumentDocument;
import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapper;
import com.binitech.pdv.application.ports.outbound.FiscalDocumentRepositoryPort;
import com.binitech.pdv.domain.FiscalDocument;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class FiscalDocumentRepositoryAdapter implements FiscalDocumentRepositoryPort {

  private final MongoTemplate mongo;
  private final PersistenceMapper mapper;

  public FiscalDocumentRepositoryAdapter(MongoTemplate mongo, PersistenceMapper mapper) {
    this.mongo = mongo;
    this.mapper = mapper;
  }

  @Override
  public FiscalDocument save(FiscalDocument document) {
    return mapper.toDomain(mongo.save(mapper.toDocument(document)));
  }

  @Override
  public Optional<FiscalDocument> findBySaleIdAndTenantId(String saleId, String tenantId) {
    Query query = Query.query(Criteria.where("saleId").is(saleId).and("tenantId").is(tenantId));
    return Optional.ofNullable(mongo.findOne(query, FiscalDocumentDocument.class))
        .map(mapper::toDomain);
  }
}
