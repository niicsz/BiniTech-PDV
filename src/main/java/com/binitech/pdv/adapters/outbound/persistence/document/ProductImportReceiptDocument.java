package com.binitech.pdv.adapters.outbound.persistence.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "product_command_receipts")
@CompoundIndex(
    name = "uk_product_command_tenant_operation",
    def = "{'tenantId':1,'operationId':1}",
    unique = true)
public record ProductImportReceiptDocument(
    @Id String id,
    String tenantId,
    String operationId,
    int lineNumber,
    String action,
    String productId,
    Instant createdAt) {}
