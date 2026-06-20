package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.mapper.PersistenceMapper;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ProductRepositoryAdapter implements ProductRepositoryPort {

  private static final String FIELD_DESCRIPTION = "description";

  private final SpringDataProductRepository repository;
  private final PersistenceMapper mapper;

  public ProductRepositoryAdapter(
      SpringDataProductRepository repository, PersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(
            value = "product_by_id",
            key = "#product.id",
            condition = "#product.id != null"),
        @CacheEvict(value = "product_by_barcode", allEntries = true),
        @CacheEvict(value = "product_by_barcode_tenant", allEntries = true),
        @CacheEvict(value = "products_by_user", allEntries = true),
        @CacheEvict(value = "products_by_tenant", allEntries = true),
        @CacheEvict(value = "products_all", allEntries = true)
      })
  public Product save(Product product) {
    var document = mapper.toDocument(product);
    var saved = repository.save(document);
    return mapper.toDomain(saved);
  }

  @Override
  @Cacheable(value = "product_by_id", key = "#id", unless = "#result == null")
  public Optional<Product> findById(String id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Product> findByBarcode(String barcode) {
    return repository.findByBarcode(barcode).map(mapper::toDomain);
  }

  @Override
  @Cacheable(value = "products_all", key = "#page + ':' + #size")
  public List<Product> findAll(int page, int size) {
    return repository
        .findAll(PageRequest.of(page, size, Sort.by(FIELD_DESCRIPTION).ascending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(value = "product_by_id", key = "#id"),
        @CacheEvict(value = "product_by_barcode", allEntries = true),
        @CacheEvict(value = "product_by_barcode_tenant", allEntries = true),
        @CacheEvict(value = "products_by_user", allEntries = true),
        @CacheEvict(value = "products_by_tenant", allEntries = true),
        @CacheEvict(value = "products_all", allEntries = true)
      })
  public void deleteById(String id) {
    repository.deleteById(id);
  }

  @Override
  public boolean existsByBarcode(String barcode) {
    return repository.existsByBarcode(barcode);
  }

  @Override
  @Cacheable(value = "products_by_user", key = "#userId + ':' + #page + ':' + #size")
  public List<Product> findAllByUserId(String userId, int page, int size) {
    return repository
        .findAllByUserId(userId, PageRequest.of(page, size, Sort.by(FIELD_DESCRIPTION).ascending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Cacheable(
      value = "product_by_barcode",
      key = "#barcode + ':' + #userId",
      unless = "#result == null")
  public Optional<Product> findByBarcodeAndUserId(String barcode, String userId) {
    return repository.findByBarcodeAndUserId(barcode, userId).map(mapper::toDomain);
  }

  @Override
  public boolean existsByBarcodeAndUserId(String barcode, String userId) {
    return repository.existsByBarcodeAndUserId(barcode, userId);
  }

  @Override
  @Cacheable(value = "products_by_tenant", key = "#tenantId + ':' + #page + ':' + #size")
  public List<Product> findAllByTenantId(String tenantId, int page, int size) {
    return repository
        .findAllByTenantId(
            tenantId, PageRequest.of(page, size, Sort.by(FIELD_DESCRIPTION).ascending()))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Cacheable(
      value = "product_by_barcode_tenant",
      key = "#barcode + ':' + #tenantId",
      unless = "#result == null")
  public Optional<Product> findByBarcodeAndTenantId(String barcode, String tenantId) {
    return repository.findByBarcodeAndTenantId(barcode, tenantId).map(mapper::toDomain);
  }

  @Override
  public boolean existsByBarcodeAndTenantId(String barcode, String tenantId) {
    return repository.existsByBarcodeAndTenantId(barcode, tenantId);
  }

  @Override
  public long countActiveByTenantId(String tenantId) {
    return repository.countByTenantIdAndActiveTrue(tenantId);
  }
}
