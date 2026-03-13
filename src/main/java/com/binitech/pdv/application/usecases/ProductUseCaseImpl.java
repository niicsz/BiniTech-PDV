package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.Enum.Role;
import java.util.List;

public class ProductUseCaseImpl implements ProductUseCasePort {

  private final ProductRepositoryPort productRepository;

  public ProductUseCaseImpl(ProductRepositoryPort productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public Product createProduct(Product product, String userId) {
    if (productRepository.existsByBarcodeAndUserId(product.getBarcode(), userId)) {
      throw new BusinessException(
          "Já existe um produto cadastrado com o código de barras: " + product.getBarcode());
    }
    product.setActive(true);
    product.setUserId(userId);
    return productRepository.save(product);
  }

  @Override
  public Product updateProduct(String id, Product product, String userId, Role role) {
    Product existing =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));

    if (role != Role.ADMIN && !existing.getUserId().equals(userId)) {
      throw new BusinessException("Você não tem permissão para alterar este produto.");
    }

    if (!existing.getBarcode().equals(product.getBarcode())
        && productRepository.existsByBarcodeAndUserId(product.getBarcode(), existing.getUserId())) {
      throw new BusinessException(
          "Já existe um produto cadastrado com o código de barras: " + product.getBarcode());
    }

    existing.setBarcode(product.getBarcode());
    existing.setDescription(product.getDescription());
    existing.setPrice(product.getPrice());
    existing.setCostPrice(product.getCostPrice());
    existing.setStockQuantity(product.getStockQuantity());
    existing.setCategory(product.getCategory());

    return productRepository.save(existing);
  }

  @Override
  public void deleteProduct(String id, String userId, Role role) {
    Product existing =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));

    if (role != Role.ADMIN && !existing.getUserId().equals(userId)) {
      throw new BusinessException("Você não tem permissão para remover este produto.");
    }

    existing.setActive(false);
    productRepository.save(existing);
  }

  @Override
  public Product findById(String id, String userId, Role role) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));

    if (role != Role.ADMIN && !product.getUserId().equals(userId)) {
      throw new ResourceNotFoundException("Produto", "id", id);
    }

    return product;
  }

  @Override
  public Product findByBarcode(String barcode, String userId) {
    return productRepository
        .findByBarcodeAndUserId(barcode, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Produto", "código de barras", barcode));
  }

  @Override
  public List<Product> listAll(String userId, Role role) {
    if (role == Role.ADMIN) {
      return productRepository.findAll();
    }
    return productRepository.findAllByUserId(userId);
  }
}
