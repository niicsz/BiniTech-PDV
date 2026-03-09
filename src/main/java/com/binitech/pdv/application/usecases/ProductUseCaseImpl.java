package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import java.util.List;

public class ProductUseCaseImpl implements ProductUseCasePort {

  private final ProductRepositoryPort productRepository;

  public ProductUseCaseImpl(ProductRepositoryPort productRepository) {
    this.productRepository = productRepository;
  }

  @Override
  public Product createProduct(Product product) {
    if (productRepository.existsByBarcode(product.getBarcode())) {
      throw new BusinessException(
          "Já existe um produto cadastrado com o código de barras: " + product.getBarcode());
    }
    product.setActive(true);
    return productRepository.save(product);
  }

  @Override
  public Product updateProduct(String id, Product product) {
    Product existing =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));

    if (!existing.getBarcode().equals(product.getBarcode())
        && productRepository.existsByBarcode(product.getBarcode())) {
      throw new BusinessException(
          "Já existe um produto cadastrado com o código de barras: " + product.getBarcode());
    }

    existing.setBarcode(product.getBarcode());
    existing.setDescription(product.getDescription());
    existing.setPrice(product.getPrice());
    existing.setStockQuantity(product.getStockQuantity());

    return productRepository.save(existing);
  }

  @Override
  public void deleteProduct(String id) {
    Product existing =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));
    existing.setActive(false);
    productRepository.save(existing);
  }

  @Override
  public Product findById(String id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", id));
  }

  @Override
  public Product findByBarcode(String barcode) {
    return productRepository
        .findByBarcode(barcode)
        .orElseThrow(() -> new ResourceNotFoundException("Produto", "código de barras", barcode));
  }

  @Override
  public List<Product> listAll() {
    return productRepository.findAll();
  }
}
