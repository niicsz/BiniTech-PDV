package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapper;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.Product;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductRepositoryAdapter implements ProductRepositoryPort {

  private final SpringDataProductRepository repository;
  private final PersistenceMapper mapper;

  public ProductRepositoryAdapter(
      SpringDataProductRepository repository, PersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Product save(Product product) {
    var document = mapper.toDocument(product);
    var saved = repository.save(document);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<Product> findById(String id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Product> findByBarcode(String barcode) {
    return repository.findByBarcode(barcode).map(mapper::toDomain);
  }

  @Override
  public List<Product> findAll() {
    return repository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
  }

  @Override
  public void deleteById(String id) {
    repository.deleteById(id);
  }

  @Override
  public boolean existsByBarcode(String barcode) {
    return repository.existsByBarcode(barcode);
  }

  @Override
  public List<Product> findAllByUserId(String userId) {
    return repository.findAllByUserId(userId).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Product> findByBarcodeAndUserId(String barcode, String userId) {
    return repository.findByBarcodeAndUserId(barcode, userId).map(mapper::toDomain);
  }

  @Override
  public boolean existsByBarcodeAndUserId(String barcode, String userId) {
    return repository.existsByBarcodeAndUserId(barcode, userId);
  }
}
