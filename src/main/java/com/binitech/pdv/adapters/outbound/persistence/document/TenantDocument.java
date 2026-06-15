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
@Document(collection = "tenants")
public class TenantDocument {

  @Id private String id;

  @Indexed(unique = true)
  private String slug;

  private String name;
  private String status;
  private String planId;
  private String billingEmail;
  private LocalDate trialEndsAt;
  private LocalDate blockedAt;
  private String blockReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
