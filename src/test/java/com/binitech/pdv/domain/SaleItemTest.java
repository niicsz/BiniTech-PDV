package com.binitech.pdv.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaleItemTest {

  @Test
  @DisplayName("Construtor com args deve calcular subtotal corretamente")
  void constructor_shouldCalculateSubtotal() {
    SaleItem item = new SaleItem("p1", "Produto 1", 3, 10.0, 5.0);

    assertEquals(30.0, item.getSubtotal(), 0.01);
  }

  @Test
  @DisplayName("recalculateSubtotal deve atualizar o subtotal")
  void recalculateSubtotal_shouldUpdateSubtotal() {
    SaleItem item = new SaleItem();
    item.setQuantity(4);
    item.setUnitPrice(15.0);

    item.recalculateSubtotal();

    assertEquals(60.0, item.getSubtotal(), 0.01);
  }

  @Test
  @DisplayName("recalculateSubtotal com quantidade zero deve retornar zero")
  void recalculateSubtotal_withZeroQuantity_shouldReturnZero() {
    SaleItem item = new SaleItem();
    item.setQuantity(0);
    item.setUnitPrice(10.0);

    item.recalculateSubtotal();

    assertEquals(0.0, item.getSubtotal(), 0.01);
  }

  @Test
  @DisplayName("equals deve comparar por productId")
  void equals_shouldCompareByProductId() {
    SaleItem item1 = new SaleItem("p1", "Produto 1", 1, 10.0, 5.0);
    SaleItem item2 = new SaleItem("p1", "Produto Diferente", 2, 20.0, 10.0);

    assertEquals(item1, item2);
  }

  @Test
  @DisplayName("equals com productId diferente deve retornar false")
  void equals_withDifferentProductId_shouldReturnFalse() {
    SaleItem item1 = new SaleItem("p1", "Produto 1", 1, 10.0, 5.0);
    SaleItem item2 = new SaleItem("p2", "Produto 2", 1, 10.0, 5.0);

    assertNotEquals(item1, item2);
  }
}
