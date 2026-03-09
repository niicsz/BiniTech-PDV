package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.SaleDocument;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSaleRepository extends MongoRepository<SaleDocument, String> {

  List<SaleDocument> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
