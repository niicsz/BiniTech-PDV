package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.InvoiceDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataInvoiceRepository;
import com.binitech.pdv.application.ports.outbound.InvoiceRepositoryPort;
import com.binitech.pdv.domain.Invoice;
import com.binitech.pdv.utils.Enum.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class InvoiceRepositoryAdapter implements InvoiceRepositoryPort {

  private final SpringDataInvoiceRepository repository;

  public InvoiceRepositoryAdapter(SpringDataInvoiceRepository repository) {
    this.repository = repository;
  }

  @Override
  public Invoice save(Invoice invoice) {
    return toDomain(repository.save(toDocument(invoice)));
  }

  @Override
  public Optional<Invoice> findById(String id) {
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId) {
    return repository.findByStripeInvoiceId(stripeInvoiceId).map(this::toDomain);
  }

  @Override
  public List<Invoice> findAllByTenantIdOrderByDueDateDesc(String tenantId) {
    return repository.findAllByTenantIdOrderByDueDateDesc(tenantId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Invoice> findOverdueInvoicesBefore(LocalDate cutoffDate) {
    return repository
        .findByStatusAndDueDateBefore(InvoiceStatus.OVERDUE.name(), cutoffDate)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  private InvoiceDocument toDocument(Invoice i) {
    return InvoiceDocument.builder()
        .id(i.getId())
        .tenantId(i.getTenantId())
        .subscriptionId(i.getSubscriptionId())
        .stripeInvoiceId(i.getStripeInvoiceId())
        .amount(i.getAmount())
        .status(i.getStatus() != null ? i.getStatus().name() : null)
        .dueDate(i.getDueDate())
        .paidAt(i.getPaidAt())
        .description(i.getDescription())
        .baseAmount(i.getBaseAmount())
        .excessAmount(i.getExcessAmount())
        .createdAt(i.getCreatedAt())
        .updatedAt(i.getUpdatedAt())
        .build();
  }

  private Invoice toDomain(InvoiceDocument d) {
    return new Invoice(
        d.getId(),
        d.getTenantId(),
        d.getSubscriptionId(),
        d.getStripeInvoiceId(),
        d.getAmount(),
        d.getStatus() != null ? InvoiceStatus.valueOf(d.getStatus()) : null,
        d.getDueDate(),
        d.getPaidAt(),
        d.getDescription(),
        d.getBaseAmount(),
        d.getExcessAmount(),
        d.getCreatedAt(),
        d.getUpdatedAt());
  }
}
