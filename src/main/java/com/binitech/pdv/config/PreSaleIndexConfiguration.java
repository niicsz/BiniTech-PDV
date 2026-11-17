package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.persistence.document.PreSaleDocument;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class PreSaleIndexConfiguration {
  @Bean
  ApplicationRunner ensurePreSaleIndexes(MongoTemplate mongo) {
    return arguments -> {
      mongo
          .indexOps(PreSaleDocument.class)
          .createIndex(
              new Index()
                  .on("tenantId", Sort.Direction.ASC)
                  .on("number", Sort.Direction.ASC)
                  .unique()
                  .named("tenant_number"));
      mongo
          .indexOps(PreSaleDocument.class)
          .createIndex(
              new Index()
                  .on("tenantId", Sort.Direction.ASC)
                  .on("status", Sort.Direction.ASC)
                  .on("createdAt", Sort.Direction.DESC)
                  .named("tenant_status_created"));
    };
  }
}
