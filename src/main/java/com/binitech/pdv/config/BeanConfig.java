package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.application.usecases.AuthUseCaseImpl;
import com.binitech.pdv.application.usecases.ProductUseCaseImpl;
import com.binitech.pdv.application.usecases.SaleUseCaseImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class BeanConfig {

  private static final Logger log = LoggerFactory.getLogger(BeanConfig.class);

  @Bean
  public PasswordEncoder passwordEncoder(@Value("${security.pepper}") String pepper) {
    log.info("Configurando PasswordEncoder com Argon2 + pepper");
    Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    return new PepperedPasswordEncoder(argon2, pepper);
  }

  @Bean
  public AuthUseCasePort authUseCasePort(
      UserRepositoryPort userRepositoryPort,
      RefreshTokenRepositoryPort refreshTokenRepositoryPort,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder,
      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
    log.info("Configurando AuthUseCasePort com refresh expiration: {}ms", refreshExpiration);
    return new AuthUseCaseImpl(
        userRepositoryPort,
        refreshTokenRepositoryPort,
        jwtTokenProvider,
        passwordEncoder,
        refreshExpiration);
  }

  @Bean
  public ProductUseCasePort productUseCasePort(ProductRepositoryPort productRepositoryPort) {
    log.info("Configurando ProductUseCasePort");
    return new ProductUseCaseImpl(productRepositoryPort);
  }

  @Bean
  public SaleUseCasePort saleUseCasePort(
      SaleRepositoryPort saleRepositoryPort, ProductRepositoryPort productRepositoryPort) {
    log.info("Configurando SaleUseCasePort");
    return new SaleUseCaseImpl(saleRepositoryPort, productRepositoryPort);
  }
}
