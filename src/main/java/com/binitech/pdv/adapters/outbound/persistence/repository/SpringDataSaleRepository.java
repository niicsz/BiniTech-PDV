package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.SaleDocument;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSaleRepository extends MongoRepository<SaleDocument, String> {

  List<SaleDocument> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

  List<SaleDocument> findAllByUserId(String userId);

  List<SaleDocument> findByTimestampBetweenAndUserId(
      LocalDateTime start, LocalDateTime end, String userId);

  @Query("{ 'payments.method': 'CREDIARIO', 'paid': { $ne: true } }")
  List<SaleDocument> findDebtors();

  @Query("{ 'payments.method': 'CREDIARIO', 'paid': { $ne: true }, 'userId': ?0 }")
  List<SaleDocument> findDebtorsByUserId(String userId);
}
