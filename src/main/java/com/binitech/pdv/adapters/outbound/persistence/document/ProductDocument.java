package com.binitech.pdv.adapters.outbound.persistence.document;

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
@Document(collection = "products")
@CompoundIndexes({
  @CompoundIndex(name = "idx_barcode_userId", def = "{'barcode': 1, 'userId': 1}"),
  @CompoundIndex(name = "idx_userId_active", def = "{'userId': 1, 'active': 1}")
})
public class ProductDocument {

  @Id private String id;

  @Indexed(unique = false)
  private String barcode;

  private String description;
  private double price;
  private double costPrice;
  private int stockQuantity;
  private boolean active;
  private String userId;
  private String category;
}
