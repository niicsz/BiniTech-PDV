package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.PreSalesApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreatePreSaleDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PreSaleDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PreSaleItemDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PreSaleStatusDTO;
import com.binitech.pdv.application.ports.inbound.PreSaleUseCasePort;
import com.binitech.pdv.application.ports.inbound.PreSaleUseCasePort.Attendant;
import com.binitech.pdv.application.ports.inbound.PreSaleUseCasePort.ItemRequest;
import com.binitech.pdv.domain.PreSale;
import com.binitech.pdv.domain.PreSaleItem;
import com.binitech.pdv.utils.enums.PreSaleStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PreSaleController implements PreSalesApi {

  private final PreSaleUseCasePort preSales;
  private final AuthenticatedUserProvider userProvider;

  public PreSaleController(PreSaleUseCasePort preSales, AuthenticatedUserProvider userProvider) {
    this.preSales = preSales;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<PreSaleDTO> createPreSale(CreatePreSaleDTO dto) {
    List<ItemRequest> items =
        dto.getItems() == null
            ? List.of()
            : dto.getItems().stream()
                .map(item -> new ItemRequest(item.getProductId(), item.getQuantity()))
                .toList();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(toDto(preSales.create(attendant(), items, dto.getNotes())));
  }

  @Override
  public ResponseEntity<List<PreSaleDTO>> listPreSales(PreSaleStatusDTO status) {
    PreSaleStatus filter = status == null ? null : PreSaleStatus.valueOf(status.name());
    return ResponseEntity.ok(
        preSales.list(attendant(), filter).stream().map(PreSaleController::toDto).toList());
  }

  @Override
  public ResponseEntity<PreSaleDTO> getPreSaleByNumber(Long number) {
    return ResponseEntity.ok(toDto(preSales.findByNumber(attendant(), number)));
  }

  @Override
  public ResponseEntity<PreSaleDTO> cancelPreSale(String id) {
    return ResponseEntity.ok(toDto(preSales.cancel(attendant(), id)));
  }

  private Attendant attendant() {
    return new Attendant(
        userProvider.getUserId(),
        userProvider.getUsername(),
        userProvider.getTenantId(),
        userProvider.getUserRole());
  }

  private static PreSaleDTO toDto(PreSale preSale) {
    return new PreSaleDTO()
        .id(preSale.getId())
        .number(preSale.getNumber())
        .status(PreSaleStatusDTO.valueOf(preSale.getStatus().name()))
        .items(preSale.getItems().stream().map(PreSaleController::toDto).toList())
        .total(preSale.getTotal())
        .notes(preSale.getNotes())
        .attendantName(preSale.getAttendantName())
        .saleId(preSale.getSaleId())
        .createdAt(time(preSale.getCreatedAt()))
        .closedAt(time(preSale.getClosedAt()));
  }

  private static PreSaleItemDTO toDto(PreSaleItem item) {
    return new PreSaleItemDTO()
        .productId(item.productId())
        .description(item.description())
        .unitPrice(item.unitPrice())
        .quantity(item.quantity())
        .subtotal(item.subtotal());
  }

  private static OffsetDateTime time(Instant value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
  }
}
