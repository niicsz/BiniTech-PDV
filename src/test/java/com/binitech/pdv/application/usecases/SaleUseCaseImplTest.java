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
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.Enum.PaymentMethod;
import com.binitech.pdv.utils.Enum.Role;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleUseCaseImplTest {

  @Mock private SaleRepositoryPort saleRepository;
  @Mock private ProductRepositoryPort productRepository;

  private SaleUseCaseImpl saleUseCase;

  @BeforeEach
  void setUp() {
    saleUseCase = new SaleUseCaseImpl(saleRepository, productRepository);
  }

  private Product createProduct(String id, String userId, boolean active, int stock) {
    Product p = new Product();
    p.setId(id);
    p.setBarcode("BAR-" + id);
    p.setDescription("Produto " + id);
    p.setPrice(10.0);
    p.setCostPrice(5.0);
    p.setStockQuantity(stock);
    p.setActive(active);
    p.setUserId(userId);
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
      Product product = createProduct("p1", "user1", true, 10);
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

      Sale result = saleUseCase.createSale(sale, "user1");

      assertNotNull(result.getId());
      assertEquals("user1", result.getUserId());
      assertEquals(8, product.getStockQuantity());
      verify(productRepository, times(1)).findById("p1");
      verify(productRepository).save(product);
      verify(saleRepository).save(sale);
    }

    @Test
    @DisplayName("Deve criar venda com skipStockValidation mesmo sem estoque suficiente")
    void createSale_withSkipStock_shouldSucceed() {
      Product product = createProduct("p1", "user1", true, 1);
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

      Sale result = saleUseCase.createSale(sale, "user1");

      assertNotNull(result.getId());
      assertEquals(0, product.getStockQuantity());
    }

    @Test
    @DisplayName("Produto inexistente deve lançar ResourceNotFoundException")
    void createSale_withNonExistentProduct_shouldThrow() {
      Sale sale = createValidSale("p999", 1);
      when(productRepository.findById("p999")).thenReturn(Optional.empty());

      assertThrows(ResourceNotFoundException.class, () -> saleUseCase.createSale(sale, "user1"));
    }

    @Test
    @DisplayName("Produto de outro usuário deve lançar BusinessException")
    void createSale_withOtherUserProduct_shouldThrow() {
      Product product = createProduct("p1", "user2", true, 10);
      Sale sale = createValidSale("p1", 1);

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      BusinessException ex =
          assertThrows(BusinessException.class, () -> saleUseCase.createSale(sale, "user1"));

      assertTrue(ex.getMessage().contains("não pertence"));
    }

    @Test
    @DisplayName("Produto inativo deve lançar BusinessException")
    void createSale_withInactiveProduct_shouldThrow() {
      Product product = createProduct("p1", "user1", false, 10);
      Sale sale = createValidSale("p1", 1);

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      BusinessException ex =
          assertThrows(BusinessException.class, () -> saleUseCase.createSale(sale, "user1"));

      assertTrue(ex.getMessage().contains("inativo"));
    }

    @Test
    @DisplayName("Estoque insuficiente deve lançar BusinessException")
    void createSale_withInsufficientStock_shouldThrow() {
      Product product = createProduct("p1", "user1", true, 1);
      Sale sale = createValidSale("p1", 5);

      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      BusinessException ex =
          assertThrows(BusinessException.class, () -> saleUseCase.createSale(sale, "user1"));

      assertTrue(ex.getMessage().contains("Estoque insuficiente"));
    }
  }

  @Nested
  @DisplayName("findById")
  class FindByIdTests {

    @Test
    @DisplayName("Owner deve encontrar venda")
    void findById_byOwner_shouldReturn() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setUserId("user1");
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));

      Sale result = saleUseCase.findById("s1", "user1", Role.OPERATOR);

      assertEquals("s1", result.getId());
    }

    @Test
    @DisplayName("ADMIN deve encontrar venda de outro usuário")
    void findById_byAdmin_shouldReturn() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setUserId("user2");
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));

      Sale result = saleUseCase.findById("s1", "admin1", Role.ADMIN);

      assertEquals("s1", result.getId());
    }

    @Test
    @DisplayName("Não-owner não-admin deve lançar ResourceNotFoundException")
    void findById_byNonOwner_shouldThrow() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setUserId("user1");
      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));

      assertThrows(
          ResourceNotFoundException.class,
          () -> saleUseCase.findById("s1", "user2", Role.OPERATOR));
    }

    @Test
    @DisplayName("Venda inexistente deve lançar ResourceNotFoundException")
    void findById_nonExistent_shouldThrow() {
      when(saleRepository.findById("s999")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> saleUseCase.findById("s999", "user1", Role.OPERATOR));
    }
  }

  @Nested
  @DisplayName("listSalesByDay")
  class ListSalesByDayTests {

    @Test
    @DisplayName("ADMIN deve listar todas as vendas do dia")
    void listSalesByDay_asAdmin_shouldReturnAll() {
      when(saleRepository.findByTimestampBetween(any(), any())).thenReturn(List.of(new Sale()));

      List<Sale> result = saleUseCase.listSalesByDay(LocalDate.now(), "admin1", Role.ADMIN);

      assertEquals(1, result.size());
      verify(saleRepository).findByTimestampBetween(any(), any());
    }

    @Test
    @DisplayName("OPERATOR deve listar apenas suas vendas do dia")
    void listSalesByDay_asOperator_shouldReturnOwn() {
      when(saleRepository.findByTimestampBetweenAndUserId(any(), any(), eq("user1")))
          .thenReturn(List.of(new Sale()));

      List<Sale> result = saleUseCase.listSalesByDay(LocalDate.now(), "user1", Role.OPERATOR);

      assertEquals(1, result.size());
      verify(saleRepository).findByTimestampBetweenAndUserId(any(), any(), eq("user1"));
    }
  }

  @Nested
  @DisplayName("listSalesByPeriod")
  class ListSalesByPeriodTests {

    @Test
    @DisplayName("ADMIN deve listar todas as vendas do período")
    void listSalesByPeriod_asAdmin_shouldReturnAll() {
      when(saleRepository.findByTimestampBetween(any(), any())).thenReturn(List.of());

      List<Sale> result =
          saleUseCase.listSalesByPeriod(
              LocalDate.now().minusDays(7), LocalDate.now(), "admin1", Role.ADMIN);

      assertNotNull(result);
      verify(saleRepository).findByTimestampBetween(any(), any());
    }

    @Test
    @DisplayName("OPERATOR deve listar apenas suas vendas do período")
    void listSalesByPeriod_asOperator_shouldReturnOwn() {
      when(saleRepository.findByTimestampBetweenAndUserId(any(), any(), eq("user1")))
          .thenReturn(List.of());

      List<Sale> result =
          saleUseCase.listSalesByPeriod(
              LocalDate.now().minusDays(7), LocalDate.now(), "user1", Role.OPERATOR);

      assertNotNull(result);
      verify(saleRepository).findByTimestampBetweenAndUserId(any(), any(), eq("user1"));
    }
  }

  @Nested
  @DisplayName("listDebtors")
  class ListDebtorsTests {

    @Test
    @DisplayName("ADMIN deve listar todos os devedores")
    void listDebtors_asAdmin_shouldReturnAll() {
      when(saleRepository.findDebtors()).thenReturn(List.of());

      saleUseCase.listDebtors("admin1", Role.ADMIN);

      verify(saleRepository).findDebtors();
    }

    @Test
    @DisplayName("OPERATOR deve listar apenas seus devedores")
    void listDebtors_asOperator_shouldReturnOwn() {
      when(saleRepository.findDebtorsByUserId("user1")).thenReturn(List.of());

      saleUseCase.listDebtors("user1", Role.OPERATOR);

      verify(saleRepository).findDebtorsByUserId("user1");
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
      sale.setPaid(false);

      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));
      when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

      Sale result = saleUseCase.markAsPaid("s1", "user1", Role.OPERATOR);

      assertTrue(result.isPaid());
    }

    @Test
    @DisplayName("Venda inexistente deve lançar ResourceNotFoundException")
    void markAsPaid_nonExistent_shouldThrow() {
      when(saleRepository.findById("s999")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> saleUseCase.markAsPaid("s999", "user1", Role.OPERATOR));
    }

    @Test
    @DisplayName("Não-owner não-admin deve lançar ResourceNotFoundException")
    void markAsPaid_byNonOwner_shouldThrow() {
      Sale sale = new Sale();
      sale.setId("s1");
      sale.setUserId("user1");

      when(saleRepository.findById("s1")).thenReturn(Optional.of(sale));

      assertThrows(
          ResourceNotFoundException.class,
          () -> saleUseCase.markAsPaid("s1", "user2", Role.OPERATOR));
    }
  }
}
