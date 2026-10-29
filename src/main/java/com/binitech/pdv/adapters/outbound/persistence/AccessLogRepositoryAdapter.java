package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.AccessLogDocument;
import com.binitech.pdv.application.ports.outbound.AccessLogRepositoryPort;
import com.binitech.pdv.domain.AccessLog;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccessLogRepositoryAdapter implements AccessLogRepositoryPort {

  private final MongoTemplate mongo;

  public AccessLogRepositoryAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public void save(AccessLog accessLog) {
    mongo.insert(
        AccessLogDocument.builder()
            .event(accessLog.event())
            .username(accessLog.username())
            .tenantId(accessLog.tenantId())
            .ip(accessLog.ip())
            .occurredAt(accessLog.occurredAt())
            .build());
  }
}
