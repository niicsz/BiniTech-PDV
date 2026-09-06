package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataUserRepository;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.inbound.PasswordResetUseCasePort;
import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.inbound.TenantUseCasePort;
import com.binitech.pdv.application.ports.inbound.UserManagementUseCasePort;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.InvoiceRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
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
import com.binitech.pdv.application.usecases.UserManagementUseCaseImpl;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

  private static final Logger log = LoggerFactory.getLogger(BeanConfig.class);

  @Bean
  public AuthUseCasePort authUseCasePort(
      UserRepositoryPort userRepositoryPort,
      TenantRepositoryPort tenantRepositoryPort,
      AuthenticationGateway authenticationGateway) {
    log.info("Configurando AuthUseCasePort com serviço de autenticação externo");
    return new AuthUseCaseImpl(userRepositoryPort, tenantRepositoryPort, authenticationGateway);
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
      AuthenticationGateway authenticationGateway,
      EmailServicePort emailServicePort) {
    log.info("Configurando TenantUseCasePort");
    return new TenantUseCaseImpl(
        tenantRepositoryPort, userRepositoryPort, authenticationGateway, emailServicePort);
  }

  @Bean
  public UserManagementUseCasePort userManagementUseCasePort(
      UserRepositoryPort userRepositoryPort,
      TenantRepositoryPort tenantRepositoryPort,
      AuthenticationGateway authenticationGateway) {
    log.info("Configurando UserManagementUseCasePort");
    return new UserManagementUseCaseImpl(
        userRepositoryPort, tenantRepositoryPort, authenticationGateway);
  }

  @Bean
  public PasswordResetUseCasePort passwordResetUseCasePort(
      AuthenticationGateway authenticationGateway,
      EmailServicePort emailServicePort,
      @Value("${app.frontend-url}") String frontendUrl) {
    return new PasswordResetUseCaseImpl(authenticationGateway, emailServicePort, frontendUrl);
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
