package com.binitech.pdv.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleCompletedEvent(
    String tenantId,
    String saleId,
    Instant occurredAt,
    String consumerCpf,
    String consumerName,
    List<Item> items,
    List<Payment> payments) {

  public record Item(
      String productId,
      String description,
      String barcode,
      String ncm,
      String cfop,
      String csosn,
      String unit,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal total) {}

  public record Payment(String method, BigDecimal amount) {}
}
