package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataPasswordResetTokenRepository;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataUserRepository;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.inbound.PasswordResetUseCasePort;
import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.inbound.TenantUseCasePort;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.InvoiceRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.application.usecases.AuthUseCaseImpl;
import com.binitech.pdv.application.usecases.BillingUseCaseImpl;
import com.binitech.pdv.application.usecases.PasswordResetUseCaseImpl;
import com.binitech.pdv.application.usecases.ProductUseCaseImpl;
import com.binitech.pdv.application.usecases.SaleUseCaseImpl;
import com.binitech.pdv.application.usecases.TenantUseCaseImpl;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
      TenantRepositoryPort tenantRepositoryPort,
      RefreshTokenRepositoryPort refreshTokenRepositoryPort,
      JwtTokenProvider jwtTokenProvider,
      TokenBlacklistService tokenBlacklistService,
      PasswordEncoder passwordEncoder,
      @Value("${jwt.refresh-expiration}") long refreshExpiration,
      @Value("${security.dummy-password-hash}") String dummyPasswordHash) {
    log.info("Configurando AuthUseCasePort com refresh expiration: {}ms", refreshExpiration);
    return new AuthUseCaseImpl(
        userRepositoryPort,
        tenantRepositoryPort,
        refreshTokenRepositoryPort,
        jwtTokenProvider,
        tokenBlacklistService,
        passwordEncoder,
        new AuthSessionConfig(refreshExpiration, dummyPasswordHash));
  }

  @Bean
  public ProductUseCasePort productUseCasePort(ProductRepositoryPort productRepositoryPort) {
    log.info("Configurando ProductUseCasePort");
    return new ProductUseCaseImpl(productRepositoryPort);
  }

  @Bean
  public SaleUseCasePort saleUseCasePort(
      SaleRepositoryPort saleRepositoryPort,
      ProductRepositoryPort productRepositoryPort,
      @Value("${app.business-time-zone:America/Sao_Paulo}") String businessTimeZone) {
    log.info("Configurando SaleUseCasePort");
    return new SaleUseCaseImpl(
        saleRepositoryPort, productRepositoryPort, ZoneId.of(businessTimeZone));
  }

  @Bean
  public TenantUseCasePort tenantUseCasePort(
      TenantRepositoryPort tenantRepositoryPort,
      UserRepositoryPort userRepositoryPort,
      PasswordEncoder passwordEncoder,
      EmailServicePort emailServicePort) {
    log.info("Configurando TenantUseCasePort");
    return new TenantUseCaseImpl(
        tenantRepositoryPort, userRepositoryPort, passwordEncoder, emailServicePort);
  }

  @Bean
  public PasswordResetUseCasePort passwordResetUseCasePort(
      UserRepositoryPort userRepositoryPort,
      TenantRepositoryPort tenantRepositoryPort,
      SpringDataPasswordResetTokenRepository resetTokenRepository,
      PasswordEncoder passwordEncoder,
      EmailServicePort emailServicePort,
      RefreshTokenRepositoryPort refreshTokenRepositoryPort,
      TokenBlacklistService tokenBlacklistService,
      @Value("${app.frontend-url}") String frontendUrl) {
    log.info("Configurando PasswordResetUseCasePort");
    return new PasswordResetUseCaseImpl(
        userRepositoryPort,
        tenantRepositoryPort,
        resetTokenRepository,
        emailServicePort,
        new PasswordResetConfig(
            frontendUrl, passwordEncoder, refreshTokenRepositoryPort, tokenBlacklistService));
  }

  @Bean
  public BillingUseCasePort billingUseCasePort(
      SubscriptionRepositoryPort subscriptionRepositoryPort,
      InvoiceRepositoryPort invoiceRepositoryPort,
      TenantRepositoryPort tenantRepositoryPort,
      SpringDataProductRepository springDataProductRepository,
      SpringDataUserRepository springDataUserRepository,
      @Value("${app.frontend-url}") String frontendUrl,
      StripeGateway stripeGateway,
      StripeProperties stripeProperties) {
    log.info("Configurando BillingUseCasePort");
    return new BillingUseCaseImpl(
        subscriptionRepositoryPort,
        invoiceRepositoryPort,
        tenantRepositoryPort,
        springDataProductRepository,
        springDataUserRepository,
        new BillingStripeConfig(frontendUrl, stripeGateway, stripeProperties));
  }
}
