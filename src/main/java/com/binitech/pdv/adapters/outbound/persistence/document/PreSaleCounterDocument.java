package com.binitech.pdv.adapters.outbound.persistence.document;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "pre_sale_counters")
public class PreSaleCounterDocument {

  @Id private String tenantId;
  private long lastNumber;
}
