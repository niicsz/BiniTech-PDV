package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "delivery_orders")
public class DeliveryOrderDocument {

  @Id private String id;
  private String tenantId;
  private long number;
  private String status;
  private String fulfillment;
  private String customerName;
  private String customerPhone;
  private Address address;
  private List<Item> items;
  private double subtotal;
  private double deliveryFee;
  private double total;
  private String paymentMethod;
  private Double changeFor;
  private String notes;
  private String saleId;
  private String pixChargeId;
  private Instant paidAt;
  private Instant createdAt;
  private Instant updatedAt;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Address {
    private String street;
    private String number;
    private String complement;
    private String district;
    private String reference;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item {
    private String productId;
    private String description;
    private double unitPrice;
    private int quantity;
    private String notes;
  }
}
