package com.binitech.pdv.adapters.outbound.persistence.document;

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
@Document(collection = "products")
public class ProductDocument {

  @Id private String id;

  @Indexed(unique = false)
  private String barcode;

  private String description;
  private double price;
  private int stockQuantity;
  private boolean active;
  private String userId;
}
