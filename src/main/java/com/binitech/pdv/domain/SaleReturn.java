package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.RefundMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaleReturn {

  private String id;
  private LocalDateTime timestamp;
  private String userId;
  private List<SaleItem> items = new ArrayList<>();
  private double refundAmount;
  private double refundCost;
  private RefundMethod refundMethod;
  private String reason;
  private boolean restocked;
  private boolean fiscalPending;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public List<SaleItem> getItems() {
    return items;
  }

  public void setItems(List<SaleItem> items) {
    this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
  }

  public double getRefundAmount() {
    return refundAmount;
  }

  public void setRefundAmount(double refundAmount) {
    this.refundAmount = refundAmount;
  }

  public double getRefundCost() {
    return refundCost;
  }

  public void setRefundCost(double refundCost) {
    this.refundCost = refundCost;
  }

  public RefundMethod getRefundMethod() {
    return refundMethod;
  }

  public void setRefundMethod(RefundMethod refundMethod) {
    this.refundMethod = refundMethod;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public boolean isRestocked() {
    return restocked;
  }

  public void setRestocked(boolean restocked) {
    this.restocked = restocked;
  }

  public boolean isFiscalPending() {
    return fiscalPending;
  }

  public void setFiscalPending(boolean fiscalPending) {
    this.fiscalPending = fiscalPending;
  }
}
