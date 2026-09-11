package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.ProductImportIntegrationApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.*;
import com.binitech.pdv.application.ports.inbound.ProductImportIntegrationUseCasePort;
import com.binitech.pdv.application.ports.inbound.ProductImportIntegrationUseCasePort.*;
import com.binitech.pdv.utils.enums.Role;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductImportIntegrationController implements ProductImportIntegrationApi {
  private final ProductImportIntegrationUseCasePort useCase;

  public ProductImportIntegrationController(ProductImportIntegrationUseCasePort useCase) {
    this.useCase = useCase;
  }

  @Override
  public ResponseEntity<Void> authorizeProductImport(String ignored, ImportIdentityDTO request) {
    useCase.authorize(identity(request));
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ImportLookupResponseDTO> lookupProductsForImport(
      String ignored, ImportLookupRequestDTO request) {
    List<ImportProductDTO> products =
        useCase.lookup(identity(request.getIdentity()), Set.copyOf(request.getBarcodes())).stream()
            .map(this::product)
            .toList();
    return ResponseEntity.ok(new ImportLookupResponseDTO(products));
  }

  @Override
  public ResponseEntity<ImportBatchResponseDTO> applyProductImportBatch(
      String ignored, ImportBatchRequestDTO request) {
    Set<UpdateField> fields =
        request.getUpdateFields().stream()
            .map(v -> UpdateField.valueOf(v.name()))
            .collect(Collectors.toSet());
    List<ImportCommand> commands = request.getCommands().stream().map(this::command).toList();
    List<ImportProductResultDTO> results =
        useCase
            .apply(
                identity(request.getIdentity()),
                ImportMode.valueOf(request.getMode().name()),
                StockMode.valueOf(request.getStockMode().name()),
                fields,
                commands)
            .stream()
            .map(this::result)
            .toList();
    return ResponseEntity.ok(new ImportBatchResponseDTO(results));
  }

  private ImportIdentity identity(ImportIdentityDTO source) {
    return new ImportIdentity(
        source.getUserId(), source.getTenantId(), Role.valueOf(source.getRole().name()));
  }

  private ImportCommand command(ImportProductCommandDTO source) {
    return new ImportCommand(
        source.getOperationId(),
        source.getLineNumber(),
        source.getBarcode(),
        source.getName(),
        source.getPrice(),
        source.getCost(),
        source.getStockQuantity(),
        source.getCategory(),
        source.getActive());
  }

  private ImportProductDTO product(ImportProduct source) {
    return new ImportProductDTO()
        .id(source.id())
        .barcode(source.barcode())
        .name(source.name())
        .price(source.price())
        .cost(source.cost())
        .stockQuantity(source.stockQuantity())
        .category(source.category())
        .active(source.active())
        .ownerId(source.ownerId());
  }

  private ImportProductResultDTO result(ImportResult source) {
    return new ImportProductResultDTO(
            source.lineNumber(),
            ImportProductResultDTO.ActionEnum.fromValue(source.action().name()))
        .productId(source.productId())
        .error(source.error());
  }
}
