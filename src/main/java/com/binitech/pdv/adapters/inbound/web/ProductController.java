package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.ProductsApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateProductDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.ProductDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.domain.Product;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {

  private final ProductUseCasePort productUseCase;
  private final WebMapper webMapper;

  public ProductController(ProductUseCasePort productUseCase, WebMapper webMapper) {
    this.productUseCase = productUseCase;
    this.webMapper = webMapper;
  }

  @Override
  public ResponseEntity<List<ProductDTO>> listProducts() {
    List<Product> products = productUseCase.listAll();
    return ResponseEntity.ok(webMapper.toProductDtoList(products));
  }

  @Override
  public ResponseEntity<ProductDTO> createProduct(CreateProductDTO createProductDTO) {
    Product product = webMapper.toDomain(createProductDTO);
    Product created = productUseCase.createProduct(product);
    return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toDto(created));
  }

  @Override
  public ResponseEntity<ProductDTO> getProductById(String id) {
    Product product = productUseCase.findById(id);
    return ResponseEntity.ok(webMapper.toDto(product));
  }

  @Override
  public ResponseEntity<ProductDTO> updateProduct(String id, CreateProductDTO createProductDTO) {
    Product product = webMapper.toDomain(createProductDTO);
    Product updated = productUseCase.updateProduct(id, product);
    return ResponseEntity.ok(webMapper.toDto(updated));
  }

  @Override
  public ResponseEntity<Void> deleteProduct(String id) {
    productUseCase.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ProductDTO> getProductByBarcode(String barcode) {
    Product product = productUseCase.findByBarcode(barcode);
    return ResponseEntity.ok(webMapper.toDto(product));
  }
}
