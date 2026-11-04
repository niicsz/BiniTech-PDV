package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

public class DeliveryStore {

  private String tenantId;
  private boolean enabled;
  private boolean open;
  private String whatsapp;
  private String message;
  private double deliveryFee;
  private double minimumOrder;
  private int estimatedMinutes;
  private boolean deliveryAvailable;
  private boolean pickupAvailable;
  private String pickupAddress;
  private Set<DeliveryPaymentMethod> acceptedPayments;
  private String pixKey;
  private Instant updatedAt;

  public static DeliveryStore defaults(String tenantId) {
    DeliveryStore store = new DeliveryStore();
    store.tenantId = tenantId;
    store.estimatedMinutes = 40;
    store.deliveryAvailable = true;
    store.pickupAvailable = true;
    store.acceptedPayments = EnumSet.allOf(DeliveryPaymentMethod.class);
    return store;
  }

  public boolean acceptsOrders() {
    return enabled && open;
  }

  public boolean accepts(DeliveryPaymentMethod method) {
    return acceptedPayments != null && acceptedPayments.contains(method);
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isOpen() {
    return open;
  }

  public void setOpen(boolean open) {
    this.open = open;
  }

  public String getWhatsapp() {
    return whatsapp;
  }

  public void setWhatsapp(String whatsapp) {
    this.whatsapp = whatsapp;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public double getDeliveryFee() {
    return deliveryFee;
  }

  public void setDeliveryFee(double deliveryFee) {
    this.deliveryFee = deliveryFee;
  }

  public double getMinimumOrder() {
    return minimumOrder;
  }

  public void setMinimumOrder(double minimumOrder) {
    this.minimumOrder = minimumOrder;
  }

  public int getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public void setEstimatedMinutes(int estimatedMinutes) {
    this.estimatedMinutes = estimatedMinutes;
  }

  public boolean isDeliveryAvailable() {
    return deliveryAvailable;
  }

  public void setDeliveryAvailable(boolean deliveryAvailable) {
    this.deliveryAvailable = deliveryAvailable;
  }

  public boolean isPickupAvailable() {
    return pickupAvailable;
  }

  public void setPickupAvailable(boolean pickupAvailable) {
    this.pickupAvailable = pickupAvailable;
  }

  public String getPickupAddress() {
    return pickupAddress;
  }

  public void setPickupAddress(String pickupAddress) {
    this.pickupAddress = pickupAddress;
  }

  public Set<DeliveryPaymentMethod> getAcceptedPayments() {
    return acceptedPayments;
  }

  public void setAcceptedPayments(Set<DeliveryPaymentMethod> acceptedPayments) {
    this.acceptedPayments = acceptedPayments;
  }

  public String getPixKey() {
    return pixKey;
  }

  public void setPixKey(String pixKey) {
    this.pixKey = pixKey;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
