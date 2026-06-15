package com.binitech.pdv.domain;

import com.binitech.pdv.utils.Enum.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Invoice {

  private String id;
  private String tenantId;
  private String subscriptionId;
  private String stripeInvoiceId;
  private BigDecimal amount;
  private InvoiceStatus status;
  private LocalDate dueDate;
  private LocalDate paidAt;
  private String description;
  private BigDecimal baseAmount;
  private BigDecimal excessAmount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Invoice() {}

  public Invoice(
      String id,
      String tenantId,
      String subscriptionId,
      String stripeInvoiceId,
      BigDecimal amount,
      InvoiceStatus status,
      LocalDate dueDate,
      LocalDate paidAt,
      String description,
      BigDecimal baseAmount,
      BigDecimal excessAmount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.subscriptionId = subscriptionId;
    this.stripeInvoiceId = stripeInvoiceId;
    this.amount = amount;
    this.status = status;
    this.dueDate = dueDate;
    this.paidAt = paidAt;
    this.description = description;
    this.baseAmount = baseAmount;
    this.excessAmount = excessAmount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public String getStripeInvoiceId() {
    return stripeInvoiceId;
  }

  public void setStripeInvoiceId(String stripeInvoiceId) {
    this.stripeInvoiceId = stripeInvoiceId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public InvoiceStatus getStatus() {
    return status;
  }

  public void setStatus(InvoiceStatus status) {
    this.status = status;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public LocalDate getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDate paidAt) {
    this.paidAt = paidAt;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
  }

  public void setBaseAmount(BigDecimal baseAmount) {
    this.baseAmount = baseAmount;
  }

  public BigDecimal getExcessAmount() {
    return excessAmount;
  }

  public void setExcessAmount(BigDecimal excessAmount) {
    this.excessAmount = excessAmount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Invoice invoice = (Invoice) o;
    return Objects.equals(id, invoice.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
