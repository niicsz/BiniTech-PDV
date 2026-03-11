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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthUseCasePort authUseCasePort(
      UserRepositoryPort userRepositoryPort,
      RefreshTokenRepositoryPort refreshTokenRepositoryPort,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder,
      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
    return new AuthUseCaseImpl(
        userRepositoryPort,
        refreshTokenRepositoryPort,
        jwtTokenProvider,
        passwordEncoder,
        refreshExpiration);
  }

  @Bean
  public ProductUseCasePort productUseCasePort(ProductRepositoryPort productRepositoryPort) {
    return new ProductUseCaseImpl(productRepositoryPort);
  }

  @Bean
  public SaleUseCasePort saleUseCasePort(
      SaleRepositoryPort saleRepositoryPort, ProductRepositoryPort productRepositoryPort) {
    return new SaleUseCaseImpl(saleRepositoryPort, productRepositoryPort);
  }
}
