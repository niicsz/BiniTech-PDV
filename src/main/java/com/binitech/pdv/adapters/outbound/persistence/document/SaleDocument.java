package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sales")
@CompoundIndexes({
  @CompoundIndex(name = "idx_userId_timestamp", def = "{'userId': 1, 'timestamp': -1}"),
  @CompoundIndex(name = "idx_userId_paid", def = "{'userId': 1, 'paid': 1}"),
  @CompoundIndex(name = "idx_paid_method", def = "{'paid': 1, 'payments.method': 1}"),
  @CompoundIndex(name = "idx_tenantId_timestamp", def = "{'tenantId': 1, 'timestamp': -1}"),
  @CompoundIndex(name = "idx_tenantId_paid", def = "{'tenantId': 1, 'paid': 1}")
})
public class SaleDocument {

  @Id private String id;

  private List<SaleItemDocument> items;
  private List<PaymentDocument> payments;
  private double totalAmount;
  private double totalCost;
  private double totalPaid;
  private double change;

  @Indexed private LocalDateTime timestamp;

  private String userId;
  private String tenantId;
  private String customerName;
  private String customerPhone;
  private boolean paid;
}
