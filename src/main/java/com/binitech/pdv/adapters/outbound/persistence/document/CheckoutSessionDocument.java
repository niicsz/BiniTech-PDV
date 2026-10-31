package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.Instant;
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
@Document(collection = "checkout_sessions")
public class CheckoutSessionDocument {

  @Id private String id;
  private String tenantId;
  private String billingType;
  private Instant createdAt;
  private Instant paidAt;
}
