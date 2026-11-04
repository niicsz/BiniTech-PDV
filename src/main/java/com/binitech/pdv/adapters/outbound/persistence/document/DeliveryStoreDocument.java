package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "delivery_stores")
public class DeliveryStoreDocument {

  @Id private String tenantId;
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
  private List<String> acceptedPayments;
  private String pixKey;
  private long lastOrderNumber;
  private Instant updatedAt;
}
