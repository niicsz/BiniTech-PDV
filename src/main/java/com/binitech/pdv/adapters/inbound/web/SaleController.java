package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.SalesApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateSaleDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.SaleDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.domain.Sale;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaleController implements SalesApi {

  private final SaleUseCasePort saleUseCase;
  private final WebMapper webMapper;

  public SaleController(SaleUseCasePort saleUseCase, WebMapper webMapper) {
    this.saleUseCase = saleUseCase;
    this.webMapper = webMapper;
  }

  @Override
  public ResponseEntity<SaleDTO> createSale(CreateSaleDTO createSaleDTO) {
    Sale sale = webMapper.toDomain(createSaleDTO);
    Sale created = saleUseCase.createSale(sale);
    return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toDto(created));
  }

  @Override
  public ResponseEntity<List<SaleDTO>> listSales(
      LocalDate date, LocalDate startDate, LocalDate endDate) {
    List<Sale> sales;

    if (date != null) {
      sales = saleUseCase.listSalesByDay(date);
    } else if (startDate != null && endDate != null) {
      sales = saleUseCase.listSalesByPeriod(startDate, endDate);
    } else {
      sales = saleUseCase.listAll();
    }

    return ResponseEntity.ok(webMapper.toSaleDtoList(sales));
  }

  @Override
  public ResponseEntity<SaleDTO> getSaleById(String id) {
    Sale sale = saleUseCase.findById(id);
    return ResponseEntity.ok(webMapper.toDto(sale));
  }
}
