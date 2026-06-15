package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.InvoiceDocument;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataInvoiceRepository extends MongoRepository<InvoiceDocument, String> {

  Optional<InvoiceDocument> findByStripeInvoiceId(String stripeInvoiceId);

  List<InvoiceDocument> findAllByTenantIdOrderByDueDateDesc(String tenantId);

  List<InvoiceDocument> findByStatusAndDueDateBefore(String status, LocalDate date);
}
