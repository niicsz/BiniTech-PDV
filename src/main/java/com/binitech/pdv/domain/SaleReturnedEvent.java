package com.binitech.pdv.domain;

import java.time.Instant;
import java.util.List;

public record SaleReturnedEvent(
    String tenantId,
    String saleId,
    String returnId,
    Instant occurredAt,
    boolean fullReturn,
    String reason,
    String consumerCpf,
    String consumerName,
    List<SaleCompletedEvent.Item> items) {}
