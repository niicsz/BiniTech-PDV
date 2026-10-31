package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.FiscalProfileDocument;
import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapper;
import com.binitech.pdv.application.ports.outbound.FiscalProfileRepositoryPort;
import com.binitech.pdv.domain.FiscalProfile;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class FiscalProfileRepositoryAdapter implements FiscalProfileRepositoryPort {

  private final MongoTemplate mongo;
  private final PersistenceMapper mapper;

  public FiscalProfileRepositoryAdapter(MongoTemplate mongo, PersistenceMapper mapper) {
    this.mongo = mongo;
    this.mapper = mapper;
  }

  @Override
  public FiscalProfile save(FiscalProfile profile) {
    return mapper.toDomain(mongo.save(mapper.toDocument(profile)));
  }

  @Override
  public Optional<FiscalProfile> findByTenantId(String tenantId) {
    return Optional.ofNullable(
            mongo.findOne(
                Query.query(Criteria.where("tenantId").is(tenantId)), FiscalProfileDocument.class))
        .map(mapper::toDomain);
  }
}
