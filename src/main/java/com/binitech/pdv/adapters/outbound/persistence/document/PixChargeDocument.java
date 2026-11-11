package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pix_charges")
public class PixChargeDocument {

  @Id private String id;
  private String tenantId;
  private String userId;
  private String provider;
  private String providerPaymentId;
  private double amount;
  private String status;
  private String qrCode;
  private String qrCodeBase64;
  private LocalDateTime createdAt;
  private LocalDateTime expiresAt;
  private LocalDateTime lastCheckedAt;
  private LocalDateTime paidAt;
  private String saleId;
  private String deliveryOrderId;
  private String failureReason;
  private SaleRequest saleRequest;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SaleRequest {
    private List<Item> items;
    private List<PaymentDocument> payments;
    private String customerName;
    private String customerPhone;
    private String customerCpf;
    private double discount;
    private double surcharge;
    private boolean skipStockValidation;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item {
    private String productId;
    private int quantity;
  }
}
