package com.binitech.pdv.adapters.outbound.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItemDocument {

  private String productId;
  private String productDescription;
  private int quantity;
  private double unitPrice;
  private double costPrice;
  private double subtotal;
}
