package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
@Document(collection = "cash_sessions")
public class CashSessionDocument {

  @Id private String id;
  private String tenantId;
  private String userId;
  private String operatorName;
  private LocalDateTime openedAt;
  private double openingAmount;
  private String status;
  private List<Movement> movements;
  private LocalDateTime closedAt;
  private String closedByUserId;
  private Double countedAmount;
  private Double difference;
  private String notes;
  private Summary closingSummary;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Movement {
    private String id;
    private String type;
    private double amount;
    private String reason;
    private String userId;
    private LocalDateTime timestamp;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Summary {
    private int salesCount;
    private double salesTotal;
    private double discountTotal;
    private double surchargeTotal;
    private Map<String, Double> receivedByMethod;
    private double cashSales;
    private double cashRefunds;
    private double supplies;
    private double withdrawals;
    private double expectedCash;
  }
}
