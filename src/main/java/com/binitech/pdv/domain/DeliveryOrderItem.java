package com.binitech.pdv.domain;

public record DeliveryOrderItem(
    String productId, String description, double unitPrice, int quantity, String notes) {

  public double subtotal() {
    return Money.multiply(unitPrice, quantity);
  }
}
