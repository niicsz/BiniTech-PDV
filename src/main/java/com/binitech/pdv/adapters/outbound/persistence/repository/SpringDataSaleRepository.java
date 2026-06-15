package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.SaleDocument;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSaleRepository extends MongoRepository<SaleDocument, String> {

  List<SaleDocument> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

  List<SaleDocument> findAllByUserId(String userId, Pageable pageable);

  List<SaleDocument> findByTimestampBetweenAndUserId(
      LocalDateTime start, LocalDateTime end, String userId);

  @Query("{ 'payments.method': 'CREDIARIO', 'paid': false }")
  List<SaleDocument> findDebtors();

  @Query("{ 'payments.method': 'CREDIARIO', 'paid': false, 'userId': ?0 }")
  List<SaleDocument> findDebtorsByUserId(String userId);

  List<SaleDocument> findAllByTenantId(String tenantId, Pageable pageable);

  List<SaleDocument> findByTimestampBetweenAndTenantId(
      LocalDateTime start, LocalDateTime end, String tenantId);

  @Query("{ 'payments.method': 'CREDIARIO', 'paid': false, 'tenantId': ?0 }")
  List<SaleDocument> findDebtorsByTenantId(String tenantId);

  long countByTenantIdAndTimestampBetween(String tenantId, LocalDateTime start, LocalDateTime end);
}
