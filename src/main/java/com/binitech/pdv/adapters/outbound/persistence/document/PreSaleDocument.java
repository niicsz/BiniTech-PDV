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
@Document(collection = "pre_sales")
public class PreSaleDocument {

  @Id private String id;
  private String tenantId;
  private long number;
  private String status;
  private List<Item> items;
  private double total;
  private String notes;
  private String attendantId;
  private String attendantName;
  private String saleId;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant closedAt;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item {
    private String productId;
    private String description;
    private double unitPrice;
    private int quantity;
  }
}
