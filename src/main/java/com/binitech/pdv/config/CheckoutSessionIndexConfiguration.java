package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.persistence.document.CheckoutSessionDocument;
import java.time.Duration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class CheckoutSessionIndexConfiguration {
  @Bean
  ApplicationRunner ensureCheckoutSessionIndexes(MongoTemplate mongo) {
    return arguments ->
        mongo
            .indexOps(CheckoutSessionDocument.class)
            .createIndex(
                new Index()
                    .on("createdAt", Sort.Direction.ASC)
                    .expire(Duration.ofDays(30))
                    .named("ttl_checkout_session_30d"));
  }
}
