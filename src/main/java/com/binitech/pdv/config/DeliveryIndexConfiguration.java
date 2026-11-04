package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.persistence.document.DeliveryOrderDocument;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class DeliveryIndexConfiguration {
  @Bean
  ApplicationRunner ensureDeliveryOrderIndexes(MongoTemplate mongo) {
    return arguments -> {
      mongo
          .indexOps(DeliveryOrderDocument.class)
          .createIndex(
              new Index()
                  .on("tenantId", Sort.Direction.ASC)
                  .on("createdAt", Sort.Direction.DESC)
                  .named("tenant_created"));
      mongo
          .indexOps(DeliveryOrderDocument.class)
          .createIndex(
              new Index()
                  .on("tenantId", Sort.Direction.ASC)
                  .on("status", Sort.Direction.ASC)
                  .on("createdAt", Sort.Direction.DESC)
                  .named("tenant_status_created"));
    };
  }
}
