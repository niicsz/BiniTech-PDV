package com.binitech.pdv.domain;

public record PreSaleItem(String productId, String description, double unitPrice, int quantity) {

  public double subtotal() {
    return Money.multiply(unitPrice, quantity);
  }
}
