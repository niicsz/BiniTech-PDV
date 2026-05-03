package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.ProductDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataProductRepository extends MongoRepository<ProductDocument, String> {

  Optional<ProductDocument> findByBarcode(String barcode);

  boolean existsByBarcode(String barcode);

  List<ProductDocument> findAllByUserId(String userId, Pageable pageable);

  Optional<ProductDocument> findByBarcodeAndUserId(String barcode, String userId);

  boolean existsByBarcodeAndUserId(String barcode, String userId);
}
