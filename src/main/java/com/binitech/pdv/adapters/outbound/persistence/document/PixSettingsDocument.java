package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.LocalDateTime;
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
@Document(collection = "pix_settings")
public class PixSettingsDocument {

  @Id private String tenantId;
  private boolean enabled;
  private String provider;
  private String accessTokenEncrypted;
  private String webhookSecretEncrypted;
  private String accountName;
  private LocalDateTime updatedAt;
}
