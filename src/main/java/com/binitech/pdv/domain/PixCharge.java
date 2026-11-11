package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.PixChargeStatus;
import java.time.LocalDateTime;

public class PixCharge {

  private String id;
  private String tenantId;
  private String userId;
  private String providerPaymentId;
  private double amount;
  private PixChargeStatus status;
  private String qrCode;
  private String qrCodeBase64;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
  private LocalDateTime lastCheckedAt;
  private LocalDateTime paidAt;
  private String saleId;
  private String deliveryOrderId;
  private String failureReason;
  private Sale saleRequest;

  public boolean isPending() {
    return status == PixChargeStatus.PENDING;
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

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getProviderPaymentId() {
    return providerPaymentId;
  }

  public void setProviderPaymentId(String providerPaymentId) {
    this.providerPaymentId = providerPaymentId;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public PixChargeStatus getStatus() {
    return status;
  }

  public void setStatus(PixChargeStatus status) {
    this.status = status;
  }

  public String getQrCode() {
    return qrCode;
  }

  public void setQrCode(String qrCode) {
    this.qrCode = qrCode;
  }

  public String getQrCodeBase64() {
    return qrCodeBase64;
  }

  public void setQrCodeBase64(String qrCodeBase64) {
    this.qrCodeBase64 = qrCodeBase64;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public LocalDateTime getLastCheckedAt() {
    return lastCheckedAt;
  }

  public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
    this.lastCheckedAt = lastCheckedAt;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public String getSaleId() {
    return saleId;
  }

  public void setSaleId(String saleId) {
    this.saleId = saleId;
  }

  public String getDeliveryOrderId() {
    return deliveryOrderId;
  }

  public void setDeliveryOrderId(String deliveryOrderId) {
    this.deliveryOrderId = deliveryOrderId;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public Sale getSaleRequest() {
    return saleRequest;
  }

  public void setSaleRequest(Sale saleRequest) {
    this.saleRequest = saleRequest;
  }
}
