package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.ProductsApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateProductDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.ProductDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.utils.Enum.Role;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {

  private static final Logger log = LoggerFactory.getLogger(ProductController.class);

  private final ProductUseCasePort productUseCase;
  private final WebMapper webMapper;

  public ProductController(ProductUseCasePort productUseCase, WebMapper webMapper) {
    this.productUseCase = productUseCase;
    this.webMapper = webMapper;
  }

  @Override
  public ResponseEntity<List<ProductDTO>> listProducts() {
    log.info("Listando produtos para userId={} role={}", getUserId(), getUserRole());
    List<Product> products = productUseCase.listAll(getUserId(), getUserRole());
    log.info("Retornando {} produto(s)", products.size());
    return ResponseEntity.ok(webMapper.toProductDtoList(products));
  }

  @Override
  public ResponseEntity<ProductDTO> createProduct(CreateProductDTO createProductDTO) {
    log.info(
        "Criando produto: barcode={} description={}",
        createProductDTO.getBarcode(),
        createProductDTO.getDescription());
    Product product = webMapper.toDomain(createProductDTO);
    Product created = productUseCase.createProduct(product, getUserId());
    log.info("Produto criado com sucesso: id={} barcode={}", created.getId(), created.getBarcode());
    return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toDto(created));
  }

  @Override
  public ResponseEntity<ProductDTO> getProductById(String id) {
    log.info("Buscando produto por id={}", id);
    Product product = productUseCase.findById(id, getUserId(), getUserRole());
    log.info("Produto encontrado: id={} description={}", product.getId(), product.getDescription());
    return ResponseEntity.ok(webMapper.toDto(product));
  }

  @Override
  public ResponseEntity<ProductDTO> updateProduct(String id, CreateProductDTO createProductDTO) {
    log.info("Atualizando produto id={} barcode={}", id, createProductDTO.getBarcode());
    Product product = webMapper.toDomain(createProductDTO);
    Product updated =
        productUseCase.updateProduct(
            id, product, getUserId(), getUserRole(), createProductDTO.getActive());
    log.info("Produto atualizado com sucesso: id={}", updated.getId());
    return ResponseEntity.ok(webMapper.toDto(updated));
  }

  @Override
  public ResponseEntity<Void> deleteProduct(String id) {
    log.info("Removendo produto id={}", id);
    productUseCase.deleteProduct(id, getUserId(), getUserRole());
    log.info("Produto removido (desativado) com sucesso: id={}", id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ProductDTO> getProductByBarcode(String barcode) {
    log.info("Buscando produto por barcode={}", barcode);
    Product product = productUseCase.findByBarcode(barcode, getUserId());
    log.info(
        "Produto encontrado por barcode: id={} description={}",
        product.getId(),
        product.getDescription());
    return ResponseEntity.ok(webMapper.toDto(product));
  }

  private String getUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return (String) auth.getPrincipal();
  }

  private Role getUserRole() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String role =
        auth.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse("OPERATOR");
    return Role.valueOf(role);
  }
}
