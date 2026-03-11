package com.binitech.pdv.domain;

import java.util.Objects;

public class Product {

  private String id;
  private String barcode;
  private String description;
  private double price;
  private int stockQuantity;
  private boolean active;
  private String userId;

  public Product() {
    this.active = true;
  }

  public Product(
      String id,
      String barcode,
      String description,
      double price,
      int stockQuantity,
      boolean active,
      String userId) {
    this.id = id;
    this.barcode = barcode;
    this.description = description;
    this.price = price;
    this.stockQuantity = stockQuantity;
    this.active = active;
    this.userId = userId;
  }

  public void decreaseStock(int quantity) {
    if (quantity > this.stockQuantity) {
      throw new IllegalArgumentException(
          "Estoque insuficiente para o produto: " + this.description);
    }
    this.stockQuantity -= quantity;
  }

  public void increaseStock(int quantity) {
    this.stockQuantity += quantity;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public int getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(int stockQuantity) {
    this.stockQuantity = stockQuantity;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Product product = (Product) o;
    return Objects.equals(id, product.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Product{"
        + "id='"
        + id
        + '\''
        + ", barcode='"
        + barcode
        + '\''
        + ", description='"
        + description
        + '\''
        + ", price="
        + price
        + ", stockQuantity="
        + stockQuantity
        + ", active="
        + active
        + '}';
  }
}
