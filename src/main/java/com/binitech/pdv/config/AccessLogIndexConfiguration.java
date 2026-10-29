package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.persistence.document.AccessLogDocument;
import java.time.Duration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class AccessLogIndexConfiguration {
  @Bean
  ApplicationRunner ensureAccessLogIndexes(MongoTemplate mongo) {
    return arguments -> {
      var indexes = mongo.indexOps(AccessLogDocument.class);
      indexes.createIndex(
          new Index()
              .on("occurredAt", Sort.Direction.ASC)
              .expire(Duration.ofDays(180))
              .named("ttl_access_log_180d"));
      indexes.createIndex(
          new Index()
              .on("tenantId", Sort.Direction.ASC)
              .on("occurredAt", Sort.Direction.DESC)
              .named("idx_access_log_tenant"));
    };
  }
}
