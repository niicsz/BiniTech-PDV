package com.binitech.pdv.domain;

import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.PreSaleStatus;
import java.time.Instant;
import java.util.List;

public class PreSale {

  private String id;
  private String tenantId;
  private long number;
  private PreSaleStatus status;
  private List<PreSaleItem> items;
  private double total;
  private String notes;
  private String attendantId;
  private String attendantName;
  private String saleId;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant closedAt;

  public void recalculate() {
    this.total = Money.sum(items.stream().map(PreSaleItem::subtotal));
  }

  public boolean isOpen() {
    return status == PreSaleStatus.OPEN;
  }

  public void ensureOpen() {
    if (status == PreSaleStatus.PAID) {
      throw new BusinessException("A comanda #" + number + " já foi paga.");
    }
    if (status == PreSaleStatus.CANCELLED) {
      throw new BusinessException("A comanda #" + number + " foi cancelada.");
    }
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

  public long getNumber() {
    return number;
  }

  public void setNumber(long number) {
    this.number = number;
  }

  public PreSaleStatus getStatus() {
    return status;
  }

  public void setStatus(PreSaleStatus status) {
    this.status = status;
  }

  public List<PreSaleItem> getItems() {
    return items;
  }

  public void setItems(List<PreSaleItem> items) {
    this.items = items;
  }

  public double getTotal() {
    return total;
  }

  public void setTotal(double total) {
    this.total = total;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getAttendantId() {
    return attendantId;
  }

  public void setAttendantId(String attendantId) {
    this.attendantId = attendantId;
  }

  public String getAttendantName() {
    return attendantName;
  }

  public void setAttendantName(String attendantName) {
    this.attendantName = attendantName;
  }

  public String getSaleId() {
    return saleId;
  }

  public void setSaleId(String saleId) {
    this.saleId = saleId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }
}
