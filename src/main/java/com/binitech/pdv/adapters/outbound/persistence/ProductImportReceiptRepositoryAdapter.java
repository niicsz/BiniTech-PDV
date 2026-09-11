package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.ProductImportReceiptDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductImportReceiptRepository;
import com.binitech.pdv.application.ports.outbound.ProductImportReceiptRepositoryPort;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductImportReceiptRepositoryAdapter implements ProductImportReceiptRepositoryPort {
  private final SpringDataProductImportReceiptRepository repository;

  public ProductImportReceiptRepositoryAdapter(
      SpringDataProductImportReceiptRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<Receipt> findByTenantIdAndOperationIds(
      String tenantId, Collection<String> operationIds) {
    return repository.findByTenantIdAndOperationIdIn(tenantId, operationIds).stream()
        .map(
            document ->
                new Receipt(
                    document.operationId(),
                    document.lineNumber(),
                    document.action(),
                    document.productId()))
        .toList();
  }

  @Override
  public void saveAll(String tenantId, List<Receipt> receipts) {
    Instant now = Instant.now();
    repository.saveAll(
        receipts.stream()
            .map(
                receipt ->
                    new ProductImportReceiptDocument(
                        null,
                        tenantId,
                        receipt.operationId(),
                        receipt.lineNumber(),
                        receipt.action(),
                        receipt.productId(),
                        now))
            .toList());
  }
}
