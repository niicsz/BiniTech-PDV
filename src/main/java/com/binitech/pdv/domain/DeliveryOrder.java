package com.binitech.pdv.domain;

import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.DeliveryFulfillment;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeliveryOrder {

  private static final Map<DeliveryOrderStatus, Set<DeliveryOrderStatus>> NEXT =
      Map.of(
          DeliveryOrderStatus.RECEIVED,
              EnumSet.of(DeliveryOrderStatus.ACCEPTED, DeliveryOrderStatus.CANCELLED),
          DeliveryOrderStatus.ACCEPTED,
              EnumSet.of(DeliveryOrderStatus.DISPATCHED, DeliveryOrderStatus.CANCELLED),
          DeliveryOrderStatus.DISPATCHED, EnumSet.of(DeliveryOrderStatus.CANCELLED),
          DeliveryOrderStatus.COMPLETED, EnumSet.noneOf(DeliveryOrderStatus.class),
          DeliveryOrderStatus.CANCELLED, EnumSet.noneOf(DeliveryOrderStatus.class));

  private static final Set<DeliveryOrderStatus> COMPLETABLE =
      EnumSet.of(DeliveryOrderStatus.ACCEPTED, DeliveryOrderStatus.DISPATCHED);

  private String id;
  private String tenantId;
  private long number;
  private DeliveryOrderStatus status;
  private DeliveryFulfillment fulfillment;
  private String customerName;
  private String customerPhone;
  private DeliveryAddress address;
  private List<DeliveryOrderItem> items;
  private double subtotal;
  private double deliveryFee;
  private double total;
  private DeliveryPaymentMethod paymentMethod;
  private Double changeFor;
  private String notes;
  private String saleId;
  private Instant createdAt;
  private Instant updatedAt;

  public void ensureCanMoveTo(DeliveryOrderStatus target) {
    if (target == DeliveryOrderStatus.COMPLETED) {
      throw new BusinessException("Use a opção Concluir para finalizar o pedido.");
    }
    if (!NEXT.get(status).contains(target)) {
      throw new BusinessException(
          "O pedido #" + number + " não pode passar de " + status + " para " + target + ".");
    }
  }

  public void ensureCanComplete() {
    if (!COMPLETABLE.contains(status)) {
      throw new BusinessException(
          "Aceite o pedido #" + number + " antes de concluir. Situação atual: " + status + ".");
    }
  }

  public void recalculate(double fee) {
    this.subtotal = Money.sum(items.stream().map(DeliveryOrderItem::subtotal));
    this.deliveryFee = fulfillment == DeliveryFulfillment.DELIVERY ? Money.round(fee) : 0;
    this.total = Money.round(subtotal + deliveryFee);
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

  public DeliveryOrderStatus getStatus() {
    return status;
  }

  public void setStatus(DeliveryOrderStatus status) {
    this.status = status;
  }

  public DeliveryFulfillment getFulfillment() {
    return fulfillment;
  }

  public void setFulfillment(DeliveryFulfillment fulfillment) {
    this.fulfillment = fulfillment;
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

  public DeliveryAddress getAddress() {
    return address;
  }

  public void setAddress(DeliveryAddress address) {
    this.address = address;
  }

  public List<DeliveryOrderItem> getItems() {
    return items;
  }

  public void setItems(List<DeliveryOrderItem> items) {
    this.items = items;
  }

  public double getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(double subtotal) {
    this.subtotal = subtotal;
  }

  public double getDeliveryFee() {
    return deliveryFee;
  }

  public void setDeliveryFee(double deliveryFee) {
    this.deliveryFee = deliveryFee;
  }

  public double getTotal() {
    return total;
  }

  public void setTotal(double total) {
    this.total = total;
  }

  public DeliveryPaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(DeliveryPaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public Double getChangeFor() {
    return changeFor;
  }

  public void setChangeFor(Double changeFor) {
    this.changeFor = changeFor;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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
}
