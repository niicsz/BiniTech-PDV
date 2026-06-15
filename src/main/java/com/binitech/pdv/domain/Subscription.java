package com.binitech.pdv.domain;

import com.binitech.pdv.utils.Enum.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Subscription {

  private String id;
  private String tenantId;
  private String stripeSubscriptionId;
  private String stripeCustomerId;
  private String stripePriceId;
  private String planTier;
  private SubscriptionStatus status;
  private LocalDate currentPeriodStart;
  private LocalDate currentPeriodEnd;
  private LocalDate nextBillingDate;
  private LocalDate lastPaymentDate;
  private int failedPaymentCount;
  private LocalDate cancelledAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Subscription() {}

  public Subscription(
      String id,
      String tenantId,
      String stripeSubscriptionId,
      String stripeCustomerId,
      String stripePriceId,
      String planTier,
      SubscriptionStatus status,
      LocalDate currentPeriodStart,
      LocalDate currentPeriodEnd,
      LocalDate nextBillingDate,
      LocalDate lastPaymentDate,
      int failedPaymentCount,
      LocalDate cancelledAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.tenantId = tenantId;
    this.stripeSubscriptionId = stripeSubscriptionId;
    this.stripeCustomerId = stripeCustomerId;
    this.stripePriceId = stripePriceId;
    this.planTier = planTier;
    this.status = status;
    this.currentPeriodStart = currentPeriodStart;
    this.currentPeriodEnd = currentPeriodEnd;
    this.nextBillingDate = nextBillingDate;
    this.lastPaymentDate = lastPaymentDate;
    this.failedPaymentCount = failedPaymentCount;
    this.cancelledAt = cancelledAt;
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

  public String getStripeSubscriptionId() {
    return stripeSubscriptionId;
  }

  public void setStripeSubscriptionId(String stripeSubscriptionId) {
    this.stripeSubscriptionId = stripeSubscriptionId;
  }

  public String getStripeCustomerId() {
    return stripeCustomerId;
  }

  public void setStripeCustomerId(String stripeCustomerId) {
    this.stripeCustomerId = stripeCustomerId;
  }

  public String getStripePriceId() {
    return stripePriceId;
  }

  public void setStripePriceId(String stripePriceId) {
    this.stripePriceId = stripePriceId;
  }

  public String getPlanTier() {
    return planTier;
  }

  public void setPlanTier(String planTier) {
    this.planTier = planTier;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public void setStatus(SubscriptionStatus status) {
    this.status = status;
  }

  public LocalDate getCurrentPeriodStart() {
    return currentPeriodStart;
  }

  public void setCurrentPeriodStart(LocalDate currentPeriodStart) {
    this.currentPeriodStart = currentPeriodStart;
  }

  public LocalDate getCurrentPeriodEnd() {
    return currentPeriodEnd;
  }

  public void setCurrentPeriodEnd(LocalDate currentPeriodEnd) {
    this.currentPeriodEnd = currentPeriodEnd;
  }

  public LocalDate getNextBillingDate() {
    return nextBillingDate;
  }

  public void setNextBillingDate(LocalDate nextBillingDate) {
    this.nextBillingDate = nextBillingDate;
  }

  public LocalDate getLastPaymentDate() {
    return lastPaymentDate;
  }

  public void setLastPaymentDate(LocalDate lastPaymentDate) {
    this.lastPaymentDate = lastPaymentDate;
  }

  public int getFailedPaymentCount() {
    return failedPaymentCount;
  }

  public void setFailedPaymentCount(int failedPaymentCount) {
    this.failedPaymentCount = failedPaymentCount;
  }

  public LocalDate getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(LocalDate cancelledAt) {
    this.cancelledAt = cancelledAt;
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
    Subscription that = (Subscription) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
