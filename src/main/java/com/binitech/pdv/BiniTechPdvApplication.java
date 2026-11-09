package com.binitech.pdv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    exclude = {
      DataRedisRepositoriesAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class
    })
@EnableMongoRepositories(basePackages = "com.binitech.pdv.adapters.outbound.persistence.repository")
@EnableScheduling
public class BiniTechPdvApplication {

  public static void main(String[] args) {
    SpringApplication.run(BiniTechPdvApplication.class, args);
  }
}
