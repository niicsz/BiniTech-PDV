package com.binitech.pdv.domain;

import com.binitech.pdv.utils.Enum.TenantStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Tenant {

  private String id;
  private String name;
  private String slug;
  private TenantStatus status;
  private String planId;
  private String billingEmail;
  private LocalDate trialEndsAt;
  private LocalDate blockedAt;
  private String blockReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Tenant() {}

  public Tenant(
      String id,
      String name,
      String slug,
      TenantStatus status,
      String planId,
      String billingEmail,
      LocalDate trialEndsAt,
      LocalDate blockedAt,
      String blockReason,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.slug = slug;
    this.status = status;
    this.planId = planId;
    this.billingEmail = billingEmail;
    this.trialEndsAt = trialEndsAt;
    this.blockedAt = blockedAt;
    this.blockReason = blockReason;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public TenantStatus getStatus() {
    return status;
  }

  public void setStatus(TenantStatus status) {
    this.status = status;
  }

  public String getPlanId() {
    return planId;
  }

  public void setPlanId(String planId) {
    this.planId = planId;
  }

  public String getBillingEmail() {
    return billingEmail;
  }

  public void setBillingEmail(String billingEmail) {
    this.billingEmail = billingEmail;
  }

  public LocalDate getTrialEndsAt() {
    return trialEndsAt;
  }

  public void setTrialEndsAt(LocalDate trialEndsAt) {
    this.trialEndsAt = trialEndsAt;
  }

  public LocalDate getBlockedAt() {
    return blockedAt;
  }

  public void setBlockedAt(LocalDate blockedAt) {
    this.blockedAt = blockedAt;
  }

  public String getBlockReason() {
    return blockReason;
  }

  public void setBlockReason(String blockReason) {
    this.blockReason = blockReason;
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
    Tenant tenant = (Tenant) o;
    return Objects.equals(id, tenant.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Tenant{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", slug='"
        + slug
        + '\''
        + ", status="
        + status
        + '}';
  }
}
