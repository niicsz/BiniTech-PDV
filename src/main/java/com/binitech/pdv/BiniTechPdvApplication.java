package com.binitech.pdv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    exclude = {RedisRepositoriesAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@EnableMongoRepositories(basePackages = "com.binitech.pdv.adapters.outbound.persistence.repository")
@EnableScheduling
public class BiniTechPdvApplication {

  public static void main(String[] args) {
    SpringApplication.run(BiniTechPdvApplication.class, args);
  }
}
