package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.Enum.Role;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductUseCaseImpl implements ProductUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(ProductUseCaseImpl.class);

  private final ProductRepositoryPort productRepository;

  public ProductUseCaseImpl(ProductRepositoryPort productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public Product createProduct(Product product, String userId) {
    log.info("Criando produto: barcode={} userId={}", product.getBarcode(), userId);
    if (productRepository.existsByBarcodeAndUserId(product.getBarcode(), userId)) {
      log.warn(
          "Produto duplicado - barcode={} já existe para userId={}", product.getBarcode(), userId);
      throw new BusinessException(
          "Já existe um produto cadastrado com o código de barras: " + product.getBarcode());
    }
    product.setActive(true);
    product.setUserId(userId);
    Product saved = productRepository.save(product);
    log.info("Produto criado com sucesso: id={} barcode={}", saved.getId(), saved.getBarcode());
    return saved;
  }

  @Override
  public Product updateProduct(
      String id, Product product, String userId, Role role, Boolean activeOverride) {
    log.info("Atualizando produto: id={} userId={} role={}", id, userId, role);
    Product existing =
        productRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn("Produto não encontrado para atualização: id={}", id);
                  return new ResourceNotFoundException("Produto", "id", id);
                });

    if (role != Role.ADMIN && !existing.getUserId().equals(userId)) {
      log.warn(
          "Sem permissão para alterar produto: id={} ownerUserId={} requestUserId={}",
          id,
          existing.getUserId(),
          userId);
      throw new BusinessException("Você não tem permissão para alterar este produto.");
    }

    if (!existing.getBarcode().equals(product.getBarcode())
        && productRepository.existsByBarcodeAndUserId(product.getBarcode(), existing.getUserId())) {
      log.warn(
          "Barcode duplicado na atualização: barcode={} userId={}",
          product.getBarcode(),
          existing.getUserId());
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
    log.info(
        "Produto atualizado com sucesso: id={} barcode={}", updated.getId(), updated.getBarcode());
    return updated;
  }

  @Override
  public void deleteProduct(String id, String userId, Role role) {
    log.info("Desativando produto: id={} userId={} role={}", id, userId, role);
    Product existing =
        productRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn("Produto não encontrado para exclusão: id={}", id);
                  return new ResourceNotFoundException("Produto", "id", id);
                });

    if (role != Role.ADMIN && !existing.getUserId().equals(userId)) {
      log.warn(
          "Sem permissão para remover produto: id={} ownerUserId={} requestUserId={}",
          id,
          existing.getUserId(),
          userId);
      throw new BusinessException("Você não tem permissão para remover este produto.");
    }

    existing.setActive(false);
    productRepository.save(existing);
    log.info("Produto desativado com sucesso: id={} barcode={}", id, existing.getBarcode());
  }

  @Override
  public Product findById(String id, String userId, Role role) {
    log.debug("Buscando produto por id={} userId={} role={}", id, userId, role);
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn("Produto não encontrado: id={}", id);
                  return new ResourceNotFoundException("Produto", "id", id);
                });

    if (role != Role.ADMIN && !product.getUserId().equals(userId)) {
      log.warn(
          "Acesso negado ao produto: id={} ownerUserId={} requestUserId={}",
          id,
          product.getUserId(),
          userId);
      throw new ResourceNotFoundException("Produto", "id", id);
    }

    log.debug(
        "Produto encontrado: id={} description={}", product.getId(), product.getDescription());
    return product;
  }

  @Override
  public Product findByBarcode(String barcode, String userId) {
    log.debug("Buscando produto por barcode={} userId={}", barcode, userId);
    return productRepository
        .findByBarcodeAndUserId(barcode, userId)
        .orElseThrow(
            () -> {
              log.warn("Produto não encontrado por barcode={} userId={}", barcode, userId);
              return new ResourceNotFoundException("Produto", "código de barras", barcode);
            });
  }

  @Override
  public List<Product> listAll(String userId, Role role) {
    log.debug("Listando todos os produtos: userId={} role={}", userId, role);
    if (role == Role.ADMIN) {
      List<Product> all = productRepository.findAll();
      log.debug("Total de produtos (ADMIN): {}", all.size());
      return all;
    }
    List<Product> userProducts = productRepository.findAllByUserId(userId);
    log.debug("Total de produtos para userId={}: {}", userId, userProducts.size());
    return userProducts;
  }
}
