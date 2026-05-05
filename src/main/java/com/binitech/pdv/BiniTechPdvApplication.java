package com.binitech.pdv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@EnableMongoRepositories(basePackages = "com.binitech.pdv.adapters.outbound.persistence.repository")
public class BiniTechPdvApplication {

  public static void main(String[] args) {
    SpringApplication.run(BiniTechPdvApplication.class, args);
  }
}
