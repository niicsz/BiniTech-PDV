package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapper;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataSaleRepository;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Sale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class SaleRepositoryAdapter implements SaleRepositoryPort {

  private static final String FIELD_TIMESTAMP = "timestamp";

  private final SpringDataSaleRepository repository;
  private final PersistenceMapper mapper;

  public SaleRepositoryAdapter(SpringDataSaleRepository repository, PersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Sale save(Sale sale) {
    var document = mapper.toDocument(sale);
    var saved = repository.save(document);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<Sale> findById(String id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Sale> findByTimestampBetween(LocalDateTime start, LocalDateTime end) {
    return repository.findByTimestampBetween(start, end).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Sale> findAll(int page, int size) {
    return repository
        .findAll(PageRequest.of(page, size, Sort.by(FIELD_TIMESTAMP).descending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findAllByUserId(String userId, int page, int size) {
    return repository
        .findAllByUserId(userId, PageRequest.of(page, size, Sort.by(FIELD_TIMESTAMP).descending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findByTimestampBetweenAndUserId(
      LocalDateTime start, LocalDateTime end, String userId) {
    return repository.findByTimestampBetweenAndUserId(start, end, userId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findDebtors() {
    return repository.findDebtors().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Sale> findDebtorsByUserId(String userId) {
    return repository.findDebtorsByUserId(userId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Sale> findAllByTenantId(String tenantId, int page, int size) {
    return repository
        .findAllByTenantId(
            tenantId, PageRequest.of(page, size, Sort.by(FIELD_TIMESTAMP).descending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findByTimestampBetweenAndTenantId(
      LocalDateTime start, LocalDateTime end, String tenantId) {
    return repository.findByTimestampBetweenAndTenantId(start, end, tenantId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Sale> findDebtorsByTenantId(String tenantId) {
    return repository.findDebtorsByTenantId(tenantId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public long countByTenantIdAndTimestampBetween(
      String tenantId, LocalDateTime start, LocalDateTime end) {
    return repository.countByTenantIdAndTimestampBetween(tenantId, start, end);
  }
}
