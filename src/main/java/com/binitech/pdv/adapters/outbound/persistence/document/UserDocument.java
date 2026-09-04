package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@CompoundIndexes({
  @CompoundIndex(
      name = "idx_username_tenantId",
      def = "{'username': 1, 'tenantId': 1}",
      unique = true)
})
public class UserDocument {

  @Id private String id;

  private String username;
  private String name;
  private String email;
  private String password;
  private String role;
  private String tenantId;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
