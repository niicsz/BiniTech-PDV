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
@Document(collection = "access_logs")
public class AccessLogDocument {

  @Id private String id;
  private String event;
  private String username;
  private String tenantId;
  private String ip;
  private Instant occurredAt;
}
