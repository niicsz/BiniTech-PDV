package com.binitech.pdv.domain;

import java.io.Serializable;
import java.util.Objects;

public class Product implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String barcode;
  private String description;
  private double price;
  private double costPrice;
  private int stockQuantity;
  private boolean active;
  private String userId;
  private String category;

  public Product() {
    this.active = true;
  }

  public Product(
      String id,
      String barcode,
      String description,
      double price,
      double costPrice,
      int stockQuantity,
      boolean active,
      String userId,
      String category) {
    this.id = id;
    this.barcode = barcode;
    this.description = description;
    this.price = price;
    this.costPrice = costPrice;
    this.stockQuantity = stockQuantity;
    this.active = active;
    this.userId = userId;
    this.category = category;
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

  public double getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(double costPrice) {
    this.costPrice = costPrice;
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

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
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
        + ", costPrice="
        + costPrice
        + ", stockQuantity="
        + stockQuantity
        + ", active="
        + active
        + ", category='"
        + category
        + '\''
        + '}';
  }
}
