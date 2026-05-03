package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.Enum.Role;
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
class ProductUseCaseImplTest {

  @Mock private ProductRepositoryPort productRepository;

  private ProductUseCaseImpl productUseCase;

  @BeforeEach
  void setUp() {
    productUseCase = new ProductUseCaseImpl(productRepository);
  }

  private Product createProduct(String id, String barcode, String userId) {
    Product p = new Product();
    p.setId(id);
    p.setBarcode(barcode);
    p.setDescription("Produto " + id);
    p.setPrice(10.0);
    p.setCostPrice(5.0);
    p.setStockQuantity(100);
    p.setUserId(userId);
    p.setActive(true);
    p.setCategory("GERAL");
    return p;
  }

  @Nested
  @DisplayName("createProduct")
  class CreateProductTests {

    @Test
    @DisplayName("Deve criar produto com sucesso")
    void createProduct_withValidData_shouldSave() {
      Product product = createProduct(null, "123456", null);
      when(productRepository.existsByBarcodeAndUserId("123456", "user1")).thenReturn(false);
      when(productRepository.save(any(Product.class)))
          .thenAnswer(
              inv -> {
                Product p = inv.getArgument(0);
                p.setId("generated-id");
                return p;
              });

      Product result = productUseCase.createProduct(product, "user1");

      assertNotNull(result.getId());
      assertTrue(result.isActive());
      assertEquals("user1", result.getUserId());
      verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Deve lançar BusinessException para barcode duplicado")
    void createProduct_withDuplicateBarcode_shouldThrow() {
      Product product = createProduct(null, "123456", null);
      when(productRepository.existsByBarcodeAndUserId("123456", "user1")).thenReturn(true);

      BusinessException ex =
          assertThrows(
              BusinessException.class, () -> productUseCase.createProduct(product, "user1"));

      assertTrue(ex.getMessage().contains("código de barras"));
      verify(productRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("updateProduct")
  class UpdateProductTests {

    @Test
    @DisplayName("Owner deve atualizar produto com sucesso")
    void updateProduct_byOwner_shouldSucceed() {
      Product existing = createProduct("p1", "111", "user1");
      Product updated = createProduct(null, "111", null);
      updated.setDescription("Atualizado");

      when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
      when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

      Product result = productUseCase.updateProduct("p1", updated, "user1", Role.OPERATOR, null);

      assertEquals("Atualizado", result.getDescription());
    }

    @Test
    @DisplayName("ADMIN deve atualizar produto de outro usuário")
    void updateProduct_byAdmin_shouldSucceed() {
      Product existing = createProduct("p1", "111", "user2");
      Product updated = createProduct(null, "111", null);

      when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
      when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

      assertDoesNotThrow(
          () -> productUseCase.updateProduct("p1", updated, "admin1", Role.ADMIN, null));
    }

    @Test
    @DisplayName("Não-owner não-admin deve lançar BusinessException")
    void updateProduct_byNonOwnerNonAdmin_shouldThrow() {
      Product existing = createProduct("p1", "111", "user1");
      Product updated = createProduct(null, "111", null);

      when(productRepository.findById("p1")).thenReturn(Optional.of(existing));

      BusinessException ex =
          assertThrows(
              BusinessException.class,
              () -> productUseCase.updateProduct("p1", updated, "user2", Role.OPERATOR, null));

      assertTrue(ex.getMessage().contains("permissão"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao trocar barcode para um duplicado")
    void updateProduct_withDuplicateBarcode_shouldThrow() {
      Product existing = createProduct("p1", "111", "user1");
      Product updated = createProduct(null, "222", null);

      when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
      when(productRepository.existsByBarcodeAndUserId("222", "user1")).thenReturn(true);

      BusinessException ex =
          assertThrows(
              BusinessException.class,
              () -> productUseCase.updateProduct("p1", updated, "user1", Role.OPERATOR, null));

      assertTrue(ex.getMessage().contains("código de barras"));
    }

    @Test
    @DisplayName("Produto inexistente deve lançar ResourceNotFoundException")
    void updateProduct_withNonExistentProduct_shouldThrow() {
      when(productRepository.findById("p999")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> productUseCase.updateProduct("p999", new Product(), "user1", Role.OPERATOR, null));
    }

    @Test
    @DisplayName("activeOverride deve alterar status active")
    void updateProduct_withActiveOverride_shouldChangeActive() {
      Product existing = createProduct("p1", "111", "user1");
      existing.setActive(true);
      Product updated = createProduct(null, "111", null);

      when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
      when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

      Product result = productUseCase.updateProduct("p1", updated, "user1", Role.OPERATOR, false);

      assertFalse(result.isActive());
    }
  }

  @Nested
  @DisplayName("deactivateProduct")
  class DeactivateProductTests {

    @Test
    @DisplayName("Owner deve desativar produto com sucesso")
    void deactivateProduct_byOwner_shouldSetInactive() {
      Product existing = createProduct("p1", "111", "user1");
      when(productRepository.findById("p1")).thenReturn(Optional.of(existing));
      when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

      productUseCase.deactivateProduct("p1", "user1", Role.OPERATOR);

      assertFalse(existing.isActive());
      verify(productRepository).save(existing);
    }

    @Test
    @DisplayName("Não-owner não-admin deve lançar BusinessException")
    void deactivateProduct_byNonOwnerNonAdmin_shouldThrow() {
      Product existing = createProduct("p1", "111", "user1");
      when(productRepository.findById("p1")).thenReturn(Optional.of(existing));

      assertThrows(
          BusinessException.class,
          () -> productUseCase.deactivateProduct("p1", "user2", Role.OPERATOR));
    }

    @Test
    @DisplayName("Produto inexistente deve lançar ResourceNotFoundException")
    void deactivateProduct_nonExistent_shouldThrow() {
      when(productRepository.findById("p999")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> productUseCase.deactivateProduct("p999", "user1", Role.OPERATOR));
    }
  }

  @Nested
  @DisplayName("findById")
  class FindByIdTests {

    @Test
    @DisplayName("Owner deve encontrar produto")
    void findById_byOwner_shouldReturn() {
      Product product = createProduct("p1", "111", "user1");
      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      Product result = productUseCase.findById("p1", "user1", Role.OPERATOR);

      assertEquals("p1", result.getId());
    }

    @Test
    @DisplayName("ADMIN deve encontrar produto de outro usuário")
    void findById_byAdmin_shouldReturn() {
      Product product = createProduct("p1", "111", "user2");
      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      Product result = productUseCase.findById("p1", "admin1", Role.ADMIN);

      assertEquals("p1", result.getId());
    }

    @Test
    @DisplayName("Não-owner não-admin deve lançar ResourceNotFoundException")
    void findById_byNonOwnerNonAdmin_shouldThrow() {
      Product product = createProduct("p1", "111", "user1");
      when(productRepository.findById("p1")).thenReturn(Optional.of(product));

      assertThrows(
          ResourceNotFoundException.class,
          () -> productUseCase.findById("p1", "user2", Role.OPERATOR));
    }

    @Test
    @DisplayName("Produto inexistente deve lançar ResourceNotFoundException")
    void findById_nonExistent_shouldThrow() {
      when(productRepository.findById("p999")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class,
          () -> productUseCase.findById("p999", "user1", Role.OPERATOR));
    }
  }

  @Nested
  @DisplayName("findByBarcode")
  class FindByBarcodeTests {

    @Test
    @DisplayName("Deve encontrar produto pelo barcode")
    void findByBarcode_shouldReturn() {
      Product product = createProduct("p1", "111", "user1");
      when(productRepository.findByBarcodeAndUserId("111", "user1"))
          .thenReturn(Optional.of(product));

      Product result = productUseCase.findByBarcode("111", "user1");

      assertEquals("p1", result.getId());
    }

    @Test
    @DisplayName("Barcode inexistente deve lançar ResourceNotFoundException")
    void findByBarcode_nonExistent_shouldThrow() {
      when(productRepository.findByBarcodeAndUserId("999", "user1")).thenReturn(Optional.empty());

      assertThrows(
          ResourceNotFoundException.class, () -> productUseCase.findByBarcode("999", "user1"));
    }
  }

  @Nested
  @DisplayName("listAll")
  class ListAllTests {

    @Test
    @DisplayName("ADMIN deve receber todos os produtos")
    void listAll_asAdmin_shouldReturnAll() {
      List<Product> allProducts =
          List.of(createProduct("p1", "111", "user1"), createProduct("p2", "222", "user2"));
      when(productRepository.findAll(0, 100)).thenReturn(allProducts);

      List<Product> result = productUseCase.listAll("admin1", Role.ADMIN);

      assertEquals(2, result.size());
      verify(productRepository).findAll(0, 100);
      verify(productRepository, never()).findAllByUserId(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("OPERATOR deve receber apenas seus produtos")
    void listAll_asOperator_shouldReturnOwn() {
      List<Product> ownProducts = List.of(createProduct("p1", "111", "user1"));
      when(productRepository.findAllByUserId("user1", 0, 100)).thenReturn(ownProducts);

      List<Product> result = productUseCase.listAll("user1", Role.OPERATOR);

      assertEquals(1, result.size());
      verify(productRepository).findAllByUserId("user1", 0, 100);
      verify(productRepository, never()).findAll(anyInt(), anyInt());
    }
  }
}
