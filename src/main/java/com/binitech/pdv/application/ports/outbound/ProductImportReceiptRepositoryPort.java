package com.binitech.pdv.application.ports.outbound;

import java.util.Collection;
import java.util.List;

public interface ProductImportReceiptRepositoryPort {
  List<Receipt> findByTenantIdAndOperationIds(String tenantId, Collection<String> operationIds);

  void saveAll(String tenantId, List<Receipt> receipts);

  record Receipt(String operationId, int lineNumber, String action, String productId) {}
}
