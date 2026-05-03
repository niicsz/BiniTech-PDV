package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.ProductsApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateProductDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.ProductDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.application.ports.inbound.ProductUseCasePort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.utils.LogSanitizer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController implements ProductsApi {

  private static final Logger log = LoggerFactory.getLogger(ProductController.class);

  private final ProductUseCasePort productUseCase;
  private final WebMapper webMapper;
  private final AuthenticatedUserProvider userProvider;

  public ProductController(
      ProductUseCasePort productUseCase,
      WebMapper webMapper,
      AuthenticatedUserProvider userProvider) {
    this.productUseCase = productUseCase;
    this.webMapper = webMapper;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<List<ProductDTO>> listProducts(Integer page, Integer size) {
    log.debug("Listando produtos para role={}", userProvider.getUserRole());
    List<Product> products =
        productUseCase.listAll(userProvider.getUserId(), userProvider.getUserRole(), page, size);
    log.info("Retornando {} produto(s)", products.size());
    return ResponseEntity.ok(webMapper.toProductDtoList(products));
  }

  @Override
  public ResponseEntity<ProductDTO> createProduct(CreateProductDTO createProductDTO) {
    log.info("Criando produto: barcode={}", createProductDTO.getBarcode());
    Product product = webMapper.toDomain(createProductDTO);
    Product created = productUseCase.createProduct(product, userProvider.getUserId());
    log.info(
        "Produto criado com sucesso: id={} barcode={}",
        LogSanitizer.maskId(created.getId()),
        created.getBarcode());
    return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toDto(created));
  }

  @Override
  public ResponseEntity<ProductDTO> getProductById(String id) {
    log.debug("Buscando produto por id={}", LogSanitizer.maskId(id));
    Product product =
        productUseCase.findById(id, userProvider.getUserId(), userProvider.getUserRole());
    log.debug("Produto encontrado: id={}", LogSanitizer.maskId(product.getId()));
    return ResponseEntity.ok(webMapper.toDto(product));
  }

  @Override
  public ResponseEntity<ProductDTO> updateProduct(String id, CreateProductDTO createProductDTO) {
    log.info("Atualizando produto id={}", LogSanitizer.maskId(id));
    Product product = webMapper.toDomain(createProductDTO);
    Product updated =
        productUseCase.updateProduct(
            id,
            product,
            userProvider.getUserId(),
            userProvider.getUserRole(),
            createProductDTO.getActive());
    log.info("Produto atualizado com sucesso: id={}", LogSanitizer.maskId(updated.getId()));
    return ResponseEntity.ok(webMapper.toDto(updated));
  }

  @Override
  public ResponseEntity<Void> deactivateProduct(String id) {
    log.info("Desativando produto id={}", LogSanitizer.maskId(id));
    productUseCase.deactivateProduct(id, userProvider.getUserId(), userProvider.getUserRole());
    log.info("Produto desativado com sucesso: id={}", LogSanitizer.maskId(id));
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ProductDTO> getProductByBarcode(String barcode) {
    log.debug("Buscando produto por barcode={}", barcode);
    Product product = productUseCase.findByBarcode(barcode, userProvider.getUserId());
    log.debug("Produto encontrado por barcode: id={}", LogSanitizer.maskId(product.getId()));
    return ResponseEntity.ok(webMapper.toDto(product));
  }
}
