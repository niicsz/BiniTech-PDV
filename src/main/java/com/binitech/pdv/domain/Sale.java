package com.binitech.pdv.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Sale {

  private String id;
  private List<SaleItem> items;
  private List<Payment> payments;
  private double totalAmount;
  private double totalCost;
  private double totalPaid;
  private double change;
  private LocalDateTime timestamp;
  private String userId;
  private String customerName;
  private String customerPhone;
  private boolean skipStockValidation;
  private boolean paid;

  public Sale() {
    this.items = new ArrayList<>();
    this.payments = new ArrayList<>();
    this.timestamp = LocalDateTime.now();
  }

  public void addItem(SaleItem item) {
    item.recalculateSubtotal();
    this.items.add(item);
    recalculateTotal();
  }

  public void removeItem(String productId) {
    this.items.removeIf(i -> i.getProductId().equals(productId));
    recalculateTotal();
  }

  public void recalculateTotal() {
    this.totalAmount = items.stream().mapToDouble(SaleItem::getSubtotal).sum();
    this.totalCost = items.stream().mapToDouble(i -> i.getCostPrice() * i.getQuantity()).sum();
  }

  public void calculatePayment() {
    this.totalPaid = payments.stream().mapToDouble(Payment::getAmount).sum();
    this.change = Math.max(0, this.totalPaid - this.totalAmount);
  }

  public void validate() {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("A venda deve ter ao menos um item.");
    }
    if (payments == null || payments.isEmpty()) {
      throw new IllegalArgumentException("A venda deve ter ao menos uma forma de pagamento.");
    }
    recalculateTotal();
    calculatePayment();
    if (this.totalPaid < this.totalAmount) {
      throw new IllegalArgumentException(
          String.format("Pagamento insuficiente. Total: %.2f, Pago: %.2f", totalAmount, totalPaid));
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<SaleItem> getItems() {
    return items;
  }

  public void setItems(List<SaleItem> items) {
    this.items = items;
  }

  public List<Payment> getPayments() {
    return payments;
  }

  public void setPayments(List<Payment> payments) {
    this.payments = payments;
  }

  public double getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(double totalAmount) {
    this.totalAmount = totalAmount;
  }

  public double getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(double totalCost) {
    this.totalCost = totalCost;
  }

  public double getTotalPaid() {
    return totalPaid;
  }

  public void setTotalPaid(double totalPaid) {
    this.totalPaid = totalPaid;
  }

  public double getChange() {
    return change;
  }

  public void setChange(double change) {
    this.change = change;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public void setCustomerPhone(String customerPhone) {
    this.customerPhone = customerPhone;
  }

  public boolean isSkipStockValidation() {
    return skipStockValidation;
  }

  public void setSkipStockValidation(boolean skipStockValidation) {
    this.skipStockValidation = skipStockValidation;
  }

  public boolean isPaid() {
    return paid;
  }

  public void setPaid(boolean paid) {
    this.paid = paid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Sale sale = (Sale) o;
    return Objects.equals(id, sale.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Sale{"
        + "id='"
        + id
        + '\''
        + ", totalAmount="
        + totalAmount
        + ", totalCost="
        + totalCost
        + ", totalPaid="
        + totalPaid
        + ", change="
        + change
        + ", timestamp="
        + timestamp
        + ", items="
        + items.size()
        + '}';
  }
}
