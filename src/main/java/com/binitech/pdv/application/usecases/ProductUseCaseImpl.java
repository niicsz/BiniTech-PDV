package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.Role;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductUseCaseImpl implements ProductUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(ProductUseCaseImpl.class);
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_PAGE_SIZE = 100;
  private static final int MAX_PAGE_SIZE = 200;

  private final ProductRepositoryPort productRepository;

  public ProductUseCaseImpl(ProductRepositoryPort productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public Product createProduct(Product product, String userId, String tenantId) {
    if (log.isInfoEnabled()) {
      log.info(
          "Criando produto: barcode={} tenantId={}",
          product.getBarcode(),
          LogSanitizer.maskId(tenantId));
    }
    if (productRepository.existsByBarcodeAndTenantId(product.getBarcode(), tenantId)) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Produto duplicado - barcode={} já existe para tenantId={}",
            product.getBarcode(),
            LogSanitizer.maskId(tenantId));
      }
      throw new BusinessException(
          "Já existe um produto cadastrado com o código de barras: " + product.getBarcode());
    }
    product.setActive(true);
    product.setUserId(userId);
    product.setTenantId(tenantId);
    Product saved = productRepository.save(product);
    if (log.isInfoEnabled()) {
      log.info(
          "Produto criado com sucesso: id={} barcode={}",
          LogSanitizer.maskId(saved.getId()),
          saved.getBarcode());
    }
    return saved;
  }

  @Override
  public Product updateProduct(
      String id,
      Product product,
      String userId,
      String tenantId,
      Role role,
      Boolean activeOverride) {
    if (log.isInfoEnabled()) {
      log.info(
          "Atualizando produto: id={} tenantId={} role={}",
          LogSanitizer.maskId(id),
          LogSanitizer.maskId(tenantId),
          role);
    }
    Product existing = findOwnedProduct(id, tenantId);

    if (!isPrivileged(role) && !Objects.equals(existing.getUserId(), userId)) {
      if (log.isWarnEnabled()) {
        log.warn("Sem permissão para alterar produto: id={}", LogSanitizer.maskId(id));
      }
      throw new BusinessException("Você não tem permissão para alterar este produto.");
    }

    if (!existing.getBarcode().equals(product.getBarcode())
        && productRepository.existsByBarcodeAndTenantId(product.getBarcode(), tenantId)) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Barcode duplicado na atualização: barcode={} tenantId={}",
            product.getBarcode(),
            LogSanitizer.maskId(tenantId));
      }
      throw new BusinessException(
          "Já existe um produto cadastrado com o código de barras: " + product.getBarcode());
    }

    existing.setBarcode(product.getBarcode());
    existing.setDescription(product.getDescription());
    existing.setPrice(product.getPrice());
    existing.setCostPrice(product.getCostPrice());
    existing.setStockQuantity(product.getStockQuantity());
    existing.setCategory(product.getCategory());
    if (activeOverride != null) {
      existing.setActive(activeOverride);
    }

    Product updated = productRepository.save(existing);
    if (log.isInfoEnabled()) {
      log.info(
          "Produto atualizado com sucesso: id={} barcode={}",
          LogSanitizer.maskId(updated.getId()),
          updated.getBarcode());
    }
    return updated;
  }

  @Override
  public void deactivateProduct(String id, String userId, String tenantId, Role role) {
    if (log.isInfoEnabled()) {
      log.info("Desativando produto: id={} role={}", LogSanitizer.maskId(id), role);
    }
    Product existing = findOwnedProduct(id, tenantId);

    if (!isPrivileged(role) && !Objects.equals(existing.getUserId(), userId)) {
      if (log.isWarnEnabled()) {
        log.warn("Sem permissão para desativar produto: id={}", LogSanitizer.maskId(id));
      }
      throw new BusinessException("Você não tem permissão para remover este produto.");
    }

    existing.setActive(false);
    productRepository.save(existing);
    if (log.isInfoEnabled()) {
      log.info("Produto desativado com sucesso: id={}", LogSanitizer.maskId(id));
    }
  }

  @Override
  public Product findById(String id, String tenantId) {
    log.debug("Buscando produto por id={} tenantId={}", id, LogSanitizer.maskId(tenantId));
    return findOwnedProduct(id, tenantId);
  }

  @Override
  public Product findByBarcode(String barcode, String tenantId) {
    log.debug(
        "Buscando produto por barcode={} tenantId={}", barcode, LogSanitizer.maskId(tenantId));
    return productRepository
        .findByBarcodeAndTenantId(barcode, tenantId)
        .orElseThrow(
            () -> {
              log.warn("Produto não encontrado por barcode={}", barcode);
              return new ResourceNotFoundException("Produto", "código de barras", barcode);
            });
  }

  @Override
  public List<Product> listAll(String tenantId) {
    if (log.isDebugEnabled()) {
      log.debug("Listando todos os produtos: tenantId={}", LogSanitizer.maskId(tenantId));
    }
    return listAll(tenantId, DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
  }

  @Override
  public List<Product> listAll(String tenantId, int page, int size) {
    int sanitizedPage = sanitizePage(page);
    int sanitizedSize = sanitizeSize(size);
    if (log.isDebugEnabled()) {
      log.debug(
          "Listando produtos paginados: tenantId={} page={} size={}",
          LogSanitizer.maskId(tenantId),
          sanitizedPage,
          sanitizedSize);
    }
    List<Product> products =
        productRepository.findAllByTenantId(tenantId, sanitizedPage, sanitizedSize);
    if (log.isDebugEnabled()) {
      log.debug(
          "Total de produtos retornados para tenantId={}: {}",
          LogSanitizer.maskId(tenantId),
          products.size());
    }
    return products;
  }

  private Product findOwnedProduct(String id, String tenantId) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn("Produto não encontrado: id={}", LogSanitizer.maskId(id));
                  return new ResourceNotFoundException("Produto", "id", id);
                });
    if (!Objects.equals(product.getTenantId(), tenantId)) {
      log.warn(
          "Acesso negado ao produto de outro tenant: id={} tenantId={}",
          LogSanitizer.maskId(id),
          LogSanitizer.maskId(tenantId));
      throw new ResourceNotFoundException("Produto", "id", id);
    }
    return product;
  }

  private boolean isPrivileged(Role role) {
    return role == Role.ADMIN || role == Role.TENANT_ADMIN || role == Role.SUPER_ADMIN;
  }

  private int sanitizePage(Integer page) {
    return page == null || page < 0 ? DEFAULT_PAGE : page;
  }

  private int sanitizeSize(Integer size) {
    if (size == null || size <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(size, MAX_PAGE_SIZE);
  }
}
