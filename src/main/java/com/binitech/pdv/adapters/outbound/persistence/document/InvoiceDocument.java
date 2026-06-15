package com.binitech.pdv.adapters.outbound.persistence.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Document(collection = "invoices")
@CompoundIndexes({
  @CompoundIndex(name = "idx_status_dueDate", def = "{'status': 1, 'dueDate': 1}"),
  @CompoundIndex(name = "idx_tenantId_dueDate", def = "{'tenantId': 1, 'dueDate': -1}")
})
public class InvoiceDocument {

  @Id private String id;

  @Indexed private String tenantId;

  private String subscriptionId;

  @Indexed private String stripeInvoiceId;

  private BigDecimal amount;
  private String status;
  private LocalDate dueDate;
  private LocalDate paidAt;
  private String description;
  private BigDecimal baseAmount;
  private BigDecimal excessAmount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
