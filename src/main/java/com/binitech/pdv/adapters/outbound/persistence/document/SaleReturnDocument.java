package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnDocument {

  private String id;
  private LocalDateTime timestamp;
  private String userId;
  private List<SaleItemDocument> items;
  private double refundAmount;
  private double refundCost;
  private String refundMethod;
  private String reason;
  private boolean restocked;
}
