package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.persistence.document.CashSessionDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.SaleDocument;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

@Configuration
public class CashSessionIndexConfiguration {
  @Bean
  ApplicationRunner ensureCashSessionIndexes(MongoTemplate mongo) {
    return arguments -> {
      mongo
          .indexOps(CashSessionDocument.class)
          .createIndex(
              new Index()
                  .on("tenantId", Sort.Direction.ASC)
                  .on("userId", Sort.Direction.ASC)
                  .unique()
                  .partial(PartialIndexFilter.of(Criteria.where("status").is("OPEN")))
                  .named("one_open_session_per_user"));
      mongo
          .indexOps(CashSessionDocument.class)
          .createIndex(
              new Index()
                  .on("tenantId", Sort.Direction.ASC)
                  .on("openedAt", Sort.Direction.DESC)
                  .named("tenant_opened"));
      mongo
          .indexOps(SaleDocument.class)
          .createIndex(
              new Index()
                  .on("cashSessionId", Sort.Direction.ASC)
                  .sparse()
                  .named("idx_cashSessionId"));
      mongo
          .indexOps(SaleDocument.class)
          .createIndex(
              new Index()
                  .on("returns.cashSessionId", Sort.Direction.ASC)
                  .sparse()
                  .named("idx_returns_cashSessionId"));
    };
  }
}
