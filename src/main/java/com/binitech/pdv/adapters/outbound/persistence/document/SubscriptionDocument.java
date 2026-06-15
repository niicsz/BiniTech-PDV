package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "subscriptions")
public class SubscriptionDocument {

  @Id private String id;

  @Indexed private String tenantId;

  @Indexed private String stripeSubscriptionId;
  @Indexed private String stripeCustomerId;
  private String stripePriceId;
  private String planTier;
  private String status;
  private LocalDate currentPeriodStart;
  private LocalDate currentPeriodEnd;
  private LocalDate nextBillingDate;
  private LocalDate lastPaymentDate;
  private int failedPaymentCount;
  private LocalDate cancelledAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
