package com.binitech.pdv.domain;

import java.util.Objects;

public class SaleItem {

  private String productId;
  private String productDescription;
  private int quantity;
  private double unitPrice;
  private double costPrice;
  private double subtotal;

  public SaleItem() {}

  public SaleItem(
      String productId,
      String productDescription,
      int quantity,
      double unitPrice,
      double costPrice) {
    this.productId = productId;
    this.productDescription = productDescription;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.costPrice = costPrice;
    this.subtotal = quantity * unitPrice;
  }

  public void recalculateSubtotal() {
    this.subtotal = this.quantity * this.unitPrice;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getProductDescription() {
    return productDescription;
  }

  public void setProductDescription(String productDescription) {
    this.productDescription = productDescription;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public double getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(double unitPrice) {
    this.unitPrice = unitPrice;
  }

  public double getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(double costPrice) {
    this.costPrice = costPrice;
  }

  public double getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(double subtotal) {
    this.subtotal = subtotal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SaleItem saleItem = (SaleItem) o;
    return Objects.equals(productId, saleItem.productId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(productId);
  }

  @Override
  public String toString() {
    return "SaleItem{"
        + "productId='"
        + productId
        + '\''
        + ", productDescription='"
        + productDescription
        + '\''
        + ", quantity="
        + quantity
        + ", unitPrice="
        + unitPrice
        + ", costPrice="
        + costPrice
        + ", subtotal="
        + subtotal
        + '}';
  }
}
