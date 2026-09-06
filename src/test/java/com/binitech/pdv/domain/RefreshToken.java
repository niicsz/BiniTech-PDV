package com.binitech.pdv.domain;

import java.time.Instant;

public class RefreshToken {

  private String id;
  private String token;
  private String userId;
  private String tenantId;
  private Instant expiryDate;

  public RefreshToken() {}

  public RefreshToken(String id, String token, String userId, Instant expiryDate) {
    this(id, token, userId, null, expiryDate);
  }

  public RefreshToken(String id, String token, String userId, String tenantId, Instant expiryDate) {
    this.id = id;
    this.token = token;
    this.userId = userId;
    this.tenantId = tenantId;
    this.expiryDate = expiryDate;
  }

  public boolean isExpired() {
    return Instant.now().isAfter(this.expiryDate);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public Instant getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(Instant expiryDate) {
    this.expiryDate = expiryDate;
  }
}
