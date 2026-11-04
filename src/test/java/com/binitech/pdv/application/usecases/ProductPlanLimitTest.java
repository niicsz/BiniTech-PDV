package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductPlanLimitTest {

  private final ProductRepositoryPort products = mock(ProductRepositoryPort.class);
  private final TenantRepositoryPort tenants = mock(TenantRepositoryPort.class);
  private final ProductUseCaseImpl useCase = new ProductUseCaseImpl(products, tenants);
  private final Tenant tenant = new Tenant();

  @BeforeEach
  void setUp() {
    tenant.setId("t1");
    tenant.setPlanId("starter");
    when(tenants.findById("t1")).thenReturn(Optional.of(tenant));
    when(products.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private static Product product() {
    Product product = new Product();
    product.setBarcode("7891000100103");
    product.setDescription("Arroz");
    product.setPrice(10);
    return product;
  }

  @Test
  @DisplayName("Abaixo do limite do plano cadastra normalmente")
  void createsBelowLimit() {
    when(products.countActiveByTenantId("t1")).thenReturn(2999L);
    assertEquals("Arroz", useCase.createProduct(product(), "u1", "t1").getDescription());
  }

  @Test
  @DisplayName("No limite do plano recusa com mensagem que explica o que fazer")
  void refusesAtLimit() {
    when(products.countActiveByTenantId("t1")).thenReturn(3000L);
    Product product = product();
    BusinessException error =
        assertThrows(BusinessException.class, () -> useCase.createProduct(product, "u1", "t1"));
    assertTrue(error.getMessage().contains("3.000 produtos ativos"));
    assertTrue(error.getMessage().contains("Não cobramos por produto a mais"));
    verify(products, never()).save(any());
  }

  @Test
  @DisplayName("Enterprise não tem limite de produtos")
  void enterpriseUnlimited() {
    tenant.setPlanId("enterprise");
    when(products.countActiveByTenantId("t1")).thenReturn(1_000_000L);
    assertEquals("Arroz", useCase.createProduct(product(), "u1", "t1").getDescription());
  }
}
