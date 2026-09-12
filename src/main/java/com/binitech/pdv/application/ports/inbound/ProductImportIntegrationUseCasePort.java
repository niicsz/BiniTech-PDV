package com.binitech.pdv.application.ports.inbound;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface ProductImportIntegrationUseCasePort {
  void authorize(ImportIdentity identity);

  List<ImportProduct> lookup(ImportIdentity identity, Set<String> barcodes);

  List<ImportResult> apply(
      ImportIdentity identity,
      ImportMode mode,
      StockMode stockMode,
      Set<UpdateField> updateFields,
      List<ImportCommand> commands);

  record ImportIdentity(String userId, String tenantId) {}

  record ImportProduct(
      String id,
      String barcode,
      String name,
      BigDecimal price,
      BigDecimal cost,
      int stockQuantity,
      String category,
      boolean active,
      String ownerId) {}

  record ImportCommand(
      String operationId,
      int lineNumber,
      String barcode,
      String name,
      BigDecimal price,
      BigDecimal cost,
      int stockQuantity,
      String category,
      Boolean active) {}

  record ImportResult(int lineNumber, ResultAction action, String productId, String error) {}

  enum ImportMode {
    CREATE_ONLY,
    CREATE_AND_UPDATE
  }

  enum StockMode {
    IGNORE,
    REPLACE,
    INCREMENT
  }

  enum UpdateField {
    NAME,
    PRICE,
    COST,
    CATEGORY,
    ACTIVE
  }

  enum ResultAction {
    CREATED,
    UPDATED,
    IGNORED,
    ERROR
  }
}
