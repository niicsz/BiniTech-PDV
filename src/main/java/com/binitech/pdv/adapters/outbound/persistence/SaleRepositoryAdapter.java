package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapper;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataSaleRepository;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Sale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SaleRepositoryAdapter implements SaleRepositoryPort {

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
    return repository.findByTimestampBetween(start, end).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Sale> findAll() {
    return repository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
  }

  @Override
  public List<Sale> findAllByUserId(String userId) {
    return repository.findAllByUserId(userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Sale> findByTimestampBetweenAndUserId(
      LocalDateTime start, LocalDateTime end, String userId) {
    return repository.findByTimestampBetweenAndUserId(start, end, userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Sale> findDebtors() {
    return repository.findDebtors().stream().map(mapper::toDomain).collect(Collectors.toList());
  }

  @Override
  public List<Sale> findDebtorsByUserId(String userId) {
    return repository.findDebtorsByUserId(userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
