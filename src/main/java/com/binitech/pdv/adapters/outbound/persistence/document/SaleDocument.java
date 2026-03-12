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
@Document(collection = "sales")
public class SaleDocument {

  @Id private String id;

  private List<SaleItemDocument> items;
  private List<PaymentDocument> payments;
  private double totalAmount;
  private double totalCost;
  private double totalPaid;
  private double change;
  private LocalDateTime timestamp;
  private String userId;
}
