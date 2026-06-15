package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.Invoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepositoryPort {

  Invoice save(Invoice invoice);

  Optional<Invoice> findById(String id);

  Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId);

  List<Invoice> findAllByTenantIdOrderByDueDateDesc(String tenantId);

  List<Invoice> findOverdueInvoicesBefore(LocalDate cutoffDate);
}
