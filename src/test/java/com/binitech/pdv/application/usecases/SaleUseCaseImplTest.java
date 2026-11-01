package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.SaleReturnRequest;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.SaleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SaleUseCaseImplTest {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

  @Mock private SaleRepositoryPort saleRepository;
  @Mock private ProductRepositoryPort productRepository;

  private SaleUseCaseImpl saleUseCase;

  @BeforeEach
  void setUp() {
    saleUseCase = new SaleUseCaseImpl(saleRepository, productRepository, BUSINESS_ZONE);
  }

  private Product createProduct(String id, String tenantId, boolean active, int stock) {
    Product p = new Product();
    p.setId(id);
    p.setBarcode("BAR-" + id);
    p.setDescription("Produto " + id);
    p.setPrice(10.0);
    p.setCostPrice(5.0);
    p.setStockQuantity(stock);
    p.setActive(active);
    p.setUserId("user1");
    p.setTenantId(tenantId);
    return p;
  }

  private Sale createValidSale(String productId, int quantity) {
    Sale sale = new Sale();
    SaleItem item = new SaleItem();
    item.setProductId(productId);
    item.setQuantity(quantity);
    sale.setItems(new ArrayList<>(List.of(item)));
    sale.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.CASH, quantity * 10.0))));
    return sale;
  }

  @Nested
  @DisplayName("createSale")
  class CreateSaleTests {

    @Test
    @DisplayName("Deve criar venda com sucesso e diminuir estoque")
    void createSale_withValidData_shouldSucceed() {
      Product product = createProduct("p1", "tenant1", true, 10);
      Sale sale = createValidSale("p1", 2);

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));
      when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
      when(saleRepository.save(any(Sale.class)))
          .thenAnswer(
              inv -> {
                Sale s = inv.getArgument(0);
                s.setId("sale1");
                return s;
              });

      Sale result = saleUseCase.createSale(sale, "user1", "tenant1");

      assertNotNull(result.getId());
      assertEquals("user1", result.getUserId());
      assertEquals("tenant1", result.getTenantId());
      assertEquals(8, product.getStockQuantity());
      verify(productRepository, times(1)).findById("p1");
      verify(productRepository).save(product);
      verify(saleRepository).save(sale);
    }

    @Test
    @DisplayName("Deve criar venda com skipStockValidation mesmo sem estoque suficiente")
    void createSale_withSkipStock_shouldSucceed() {
      Product product = createProduct("p1", "tenant1", true, 1);
      Sale sale = createValidSale("p1", 5);
      sale.setSkipStockValidation(true);

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));
      when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
      when(saleRepository.save(any(Sale.class)))
          .thenAnswer(
              inv -> {
                Sale s = inv.getArgument(0);
                s.setId("sale1");
                return s;
              });

      Sale result = saleUseCase.createSale(sale, "user1", "tenant1");

      assertNotNull(result.getId());
      assertEquals(0, product.getStockQuantity());
    }

    @Test
    @DisplayName("Produto inexistente deve lançar ResourceNotFoundException")
    void createSale_withNonExistentProduct_shouldThrow() {
      Sale sale = createValidSale("p999", 1);
      when(productRepository.findById("p999")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class, () -> saleUseCase.createSale(sale, "user1", "tenant1"));
    }

    @Test
    @DisplayName("Produto de outro tenant deve lançar BusinessException")
    void createSale_withOtherTenantProduct_shouldThrow() {
      Product product = createProduct("p1", "tenant2", true, 10);
      Sale sale = createValidSale("p1", 1);

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      BusinessException ex =
          assertThrows(
              BusinessException.class, () -> saleUseCase.createSale(sale, "user1", "tenant1"));

      assertTrue(ex.getMessage().contains("não pertence"));
    }

    @Test
    @DisplayName("Produto inativo deve lançar BusinessException")
    void createSale_withInactiveProduct_shouldThrow() {
      Product product = createProduct("p1", "tenant1", false, 10);
      Sale sale = createValidSale("p1", 1);

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      BusinessException ex =
          assertThrows(
              BusinessException.class, () -> saleUseCase.createSale(sale, "user1", "tenant1"));

      assertTrue(ex.getMessage().contains("inativo"));
    }

    @Test
    @DisplayName("Estoque insuficiente deve lançar BusinessException")
    void createSale_withInsufficientStock_shouldThrow() {
      Product product = createProduct("p1", "tenant1", true, 1);
      Sale sale = createValidSale("p1", 5);

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      BusinessException ex =
          assertThrows(
              BusinessException.class, () -> saleUseCase.createSale(sale, "user1", "tenant1"));

      assertTrue(ex.getMessage().contains("Estoque insuficiente"));
    }
  }

  @Nested
  @DisplayName("findById")
  class FindByIdTests {

    @Test
    @DisplayName("Deve encontrar venda do tenant")
    void findById_sameTenant_shouldReturn() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setTenantId("tenant1");
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));

      Sale result = saleUseCase.findById("s1", "tenant1");

      assertEquals("s1", result.getId());
    }

    @Test
    @DisplayName("Venda de outro tenant deve lançar ResourceNotFoundException")
    void findById_crossTenant_shouldThrow() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setTenantId("tenant2");
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));

      assertThrows(ResourceNotFoundException.class, () -> saleUseCase.findById("s1", "tenant1"));
    }

    @Test
    @DisplayName("Venda inexistente deve lançar ResourceNotFoundException")
    void findById_nonExistent_shouldThrow() {
      when(saleRepository.findById("s999")).thenReturn(Optional.empty());

      assertThrows(ResourceNotFoundException.class, () -> saleUseCase.findById("s999", "tenant1"));
    }
  }

  @Nested
  @DisplayName("listSalesByDay")
  class ListSalesByDayTests {

    @Test
    @DisplayName("Deve listar vendas do dia do tenant")
    void listSalesByDay_shouldReturnTenantSales() {
      when(saleRepository.findByTimestampRangeAndTenantId(any(), any(), eq("tenant1")))
          .thenReturn(List.of(new Sale()));

      List<Sale> result = saleUseCase.listSalesByDay(LocalDate.of(2024, 1, 15), "tenant1");

      assertEquals(1, result.size());
      verify(saleRepository)
          .findByTimestampRangeAndTenantId(
              Instant.parse("2024-01-15T03:00:00Z"),
              Instant.parse("2024-01-16T03:00:00Z"),
              "tenant1");
    }

    @Test
    @DisplayName("Deve respeitar uma virada histórica de horário de verão")
    void listSalesByDay_withDaylightSavingTransition_shouldUseZoneRules() {
      when(saleRepository.findByTimestampRangeAndTenantId(any(), any(), eq("tenant1")))
          .thenReturn(List.of());

      saleUseCase.listSalesByDay(LocalDate.of(2018, 11, 4), "tenant1");

      verify(saleRepository)
          .findByTimestampRangeAndTenantId(
              Instant.parse("2018-11-04T03:00:00Z"),
              Instant.parse("2018-11-05T02:00:00Z"),
              "tenant1");
    }
  }

  @Nested
  @DisplayName("listSalesByPeriod")
  class ListSalesByPeriodTests {

    @Test
    @DisplayName("Deve listar vendas do período do tenant")
    void listSalesByPeriod_shouldReturnTenantSales() {
      when(saleRepository.findByTimestampRangeAndTenantId(any(), any(), eq("tenant1")))
          .thenReturn(List.of());

      List<Sale> result =
          saleUseCase.listSalesByPeriod(
              LocalDate.of(2024, 1, 8), LocalDate.of(2024, 1, 15), "tenant1");

      assertNotNull(result);
      verify(saleRepository)
          .findByTimestampRangeAndTenantId(
              Instant.parse("2024-01-08T03:00:00Z"),
              Instant.parse("2024-01-16T03:00:00Z"),
              "tenant1");
    }

    @Test
    @DisplayName("Deve rejeitar período com datas invertidas")
    void listSalesByPeriod_withInvertedDates_shouldThrow() {
      LocalDate startDate = LocalDate.of(2024, 1, 16);
      LocalDate endDate = LocalDate.of(2024, 1, 15);

      assertThrows(
          BusinessException.class,
          () -> saleUseCase.listSalesByPeriod(startDate, endDate, "tenant1"));

      verifyNoInteractions(saleRepository);
    }
  }

  @Nested
  @DisplayName("listDebtors")
  class ListDebtorsTests {

    @Test
    @DisplayName("Deve listar devedores do tenant")
    void listDebtors_shouldReturnTenantDebtors() {
      when(saleRepository.findDebtorsByTenantId("tenant1")).thenReturn(List.of());

      saleUseCase.listDebtors("tenant1");

      verify(saleRepository).findDebtorsByTenantId("tenant1");
    }
  }

  @Nested
  @DisplayName("markAsPaid")
  class MarkAsPaidTests {

    @Test
    @DisplayName("Owner deve marcar venda como paga")
    void markAsPaid_byOwner_shouldSucceed() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setUserId("user1");
      sale.setTenantId("tenant1");
      sale.setPaid(false);

      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));
      when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

      Sale result = saleUseCase.markAsPaid("s1", "user1", "tenant1", Role.OPERATOR);

      assertTrue(result.isPaid());
    }

    @Test
    @DisplayName("Venda inexistente deve lançar ResourceNotFoundException")
    void markAsPaid_nonExistent_shouldThrow() {
      when(saleRepository.findById("s999")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> saleUseCase.markAsPaid("s999", "user1", "tenant1", Role.OPERATOR));
    }

    @Test
    @DisplayName("Não-owner não-privilegiado deve lançar ResourceNotFoundException")
    void markAsPaid_byNonOwner_shouldThrow() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setUserId("user1");
      sale.setTenantId("tenant1");

      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));

      assertThrows(
          ResourceNotFoundException.class,
          () -> saleUseCase.markAsPaid("s1", "user2", "tenant1", Role.OPERATOR));
    }
  }

  @Nested
  @DisplayName("registerReturn")
  class RegisterReturnTests {

    private Sale storedSale() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setTenantId("tenant1");
      sale.setItems(new ArrayList<>(List.of(new SaleItem("p1", "Produto p1", 3, 10.0, 5.0))));
      sale.recalculateTotal();
      sale.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.CASH, 30.0))));
      return sale;
    }

    private SaleReturnRequest request(boolean restock) {
      return new SaleReturnRequest(Map.of("p1", 2), RefundMethod.CASH, "Defeito", restock);
    }

    @Test
    @DisplayName("Registra a devolução, grava com a versão esperada e repõe o estoque")
    void registersAndRestocks() {
      Sale sale = storedSale();
      Product product = createProduct("p1", "tenant1", true, 4);
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));
      when(saleRepository.appendReturn(sale, 0)).thenReturn(true);
      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      Sale result =
          saleUseCase.registerReturn("s1", request(true), "admin", "tenant1", Role.TENANT_ADMIN);

      assertEquals(SaleStatus.PARTIALLY_RETURNED, result.getStatus());
      assertEquals(20.0, result.getReturnedAmount());
      assertEquals(1, result.getReturnsVersion());
      assertEquals("admin", result.getReturns().get(0).getUserId());
      assertEquals(6, product.getStockQuantity());
      verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Sem repor estoque, os produtos não são tocados")
    void withoutRestock() {
      Sale sale = storedSale();
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));
      when(saleRepository.appendReturn(sale, 0)).thenReturn(true);

      saleUseCase.registerReturn("s1", request(false), "admin", "tenant1", Role.ADMIN);

      verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("Produto apagado ou de outra loja não recebe estoque")
    void skipsMissingOrForeignProduct() {
      Sale sale = storedSale();
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));
      when(saleRepository.appendReturn(sale, 0)).thenReturn(true);
      when(productRepository.findById("p1"))
          .thenReturn(Optional.of(createProduct("p1", "outra", true, 1)));

      saleUseCase.registerReturn("s1", request(true), "admin", "tenant1", Role.TENANT_ADMIN);

      verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devolução concorrente na mesma venda é recusada sem mexer no estoque")
    void concurrentReturn() {
      Sale sale = storedSale();
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));
      when(saleRepository.appendReturn(sale, 0)).thenReturn(false);

      assertThrows(
          BusinessException.class,
          () ->
              saleUseCase.registerReturn(
                  "s1", request(true), "admin", "tenant1", Role.TENANT_ADMIN));
      verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("Operador não registra devolução e venda de outra loja não aparece")
    void permissions() {
      assertThrows(
          AccessDeniedException.class,
          () -> saleUseCase.registerReturn("s1", request(true), "op", "tenant1", Role.OPERATOR));
      verifyNoInteractions(saleRepository);

      when(saleRepository.findById("s1")).thenReturn(Optional.of(storedSale()));
      assertThrows(
          ResourceNotFoundException.class,
          () ->
              saleUseCase.registerReturn(
                  "s1", request(true), "admin", "tenant2", Role.TENANT_ADMIN));
    }

    @Test
    @DisplayName("Venda devolvida por completo não pode ser marcada como paga")
    void returnedSaleCannotBePaid() {
      Sale sale = storedSale();
      sale.setStatus(SaleStatus.RETURNED);
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));

      assertThrows(
          BusinessException.class,
          () -> saleUseCase.markAsPaid("s1", "admin", "tenant1", Role.TENANT_ADMIN));
      verify(saleRepository, never()).save(any());
    }
  }
}
