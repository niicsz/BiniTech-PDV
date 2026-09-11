package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.ProductImportReceiptDocument;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataProductImportReceiptRepository
    extends MongoRepository<ProductImportReceiptDocument, String> {
  List<ProductImportReceiptDocument> findByTenantIdAndOperationIdIn(
      String tenantId, Collection<String> operationIds);
}
