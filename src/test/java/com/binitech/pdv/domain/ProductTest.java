package com.binitech.pdv.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

  @Test
  @DisplayName("Construtor padrão deve inicializar active como true")
  void defaultConstructor_shouldSetActiveTrue() {
    Product product = new Product();
    assertTrue(product.isActive());
  }

  @Test
  @DisplayName("decreaseStock deve reduzir quantidade quando estoque suficiente")
  void decreaseStock_withSufficientStock_shouldDecreaseQuantity() {
    Product product = new Product();
    product.setStockQuantity(10);
    product.setDescription("Produto Teste");

    product.decreaseStock(3);

    assertEquals(7, product.getStockQuantity());
  }

  @Test
  @DisplayName("decreaseStock deve reduzir para zero quando quantidade exata")
  void decreaseStock_withExactQuantity_shouldSetToZero() {
    Product product = new Product();
    product.setStockQuantity(5);
    product.setDescription("Produto Teste");

    product.decreaseStock(5);

    assertEquals(0, product.getStockQuantity());
  }

  @Test
  @DisplayName("decreaseStock deve lançar exceção quando estoque insuficiente")
  void decreaseStock_withInsufficientStock_shouldThrowException() {
    Product product = new Product();
    product.setStockQuantity(2);
    product.setDescription("Produto Teste");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> product.decreaseStock(5));

    assertTrue(exception.getMessage().contains("Estoque insuficiente"));
    assertTrue(exception.getMessage().contains("Produto Teste"));
  }

  @Test
  @DisplayName("increaseStock deve aumentar a quantidade")
  void increaseStock_shouldIncreaseQuantity() {
    Product product = new Product();
    product.setStockQuantity(5);

    product.increaseStock(3);

    assertEquals(8, product.getStockQuantity());
  }

  @Test
  @DisplayName("increaseStock com zero não deve alterar estoque")
  void increaseStock_withZero_shouldNotChangeStock() {
    Product product = new Product();
    product.setStockQuantity(5);

    product.increaseStock(0);

    assertEquals(5, product.getStockQuantity());
  }

  @Test
  @DisplayName("equals deve retornar true para mesmo id")
  void equals_withSameId_shouldReturnTrue() {
    Product p1 = new Product();
    p1.setId("123");
    Product p2 = new Product();
    p2.setId("123");

    assertEquals(p1, p2);
  }

  @Test
  @DisplayName("equals deve retornar false para ids diferentes")
  void equals_withDifferentId_shouldReturnFalse() {
    Product p1 = new Product();
    p1.setId("123");
    Product p2 = new Product();
    p2.setId("456");

    assertNotEquals(p1, p2);
  }
}
