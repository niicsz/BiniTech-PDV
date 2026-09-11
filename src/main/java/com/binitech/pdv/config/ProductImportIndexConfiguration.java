package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.persistence.document.ProductImportReceiptDocument;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class ProductImportIndexConfiguration {
  @Bean
  ApplicationRunner ensureProductImportReceiptIndex(MongoTemplate mongo) {
    return arguments ->
        mongo
            .indexOps(ProductImportReceiptDocument.class)
            .createIndex(
                new Index()
                    .on("tenantId", Sort.Direction.ASC)
                    .on("operationId", Sort.Direction.ASC)
                    .unique()
                    .named("uk_product_command_tenant_operation"));
  }
}
