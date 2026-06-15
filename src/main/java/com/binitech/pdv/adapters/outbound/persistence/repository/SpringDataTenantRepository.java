package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.TenantDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataTenantRepository extends MongoRepository<TenantDocument, String> {

  Optional<TenantDocument> findBySlug(String slug);

  Optional<TenantDocument> findByBillingEmail(String billingEmail);

  boolean existsBySlug(String slug);

  List<TenantDocument> findAllByStatus(String status);
}
