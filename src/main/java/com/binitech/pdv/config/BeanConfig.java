package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.application.usecases.ProductUseCaseImpl;
import com.binitech.pdv.application.usecases.SaleUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

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
