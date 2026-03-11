package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.SalesApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateSaleDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.SaleDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.utils.Enum.Role;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    Sale created = saleUseCase.createSale(sale, getUserId());
    return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toDto(created));
  }

  @Override
  public ResponseEntity<List<SaleDTO>> listSales(
      LocalDate date, LocalDate startDate, LocalDate endDate) {
    String userId = getUserId();
    Role role = getUserRole();
    List<Sale> sales;

    if (date != null) {
      sales = saleUseCase.listSalesByDay(date, userId, role);
    } else if (startDate != null && endDate != null) {
      sales = saleUseCase.listSalesByPeriod(startDate, endDate, userId, role);
    } else {
      sales = saleUseCase.listAll(userId, role);
    }

    return ResponseEntity.ok(webMapper.toSaleDtoList(sales));
  }

  @Override
  public ResponseEntity<SaleDTO> getSaleById(String id) {
    Sale sale = saleUseCase.findById(id, getUserId(), getUserRole());
    return ResponseEntity.ok(webMapper.toDto(sale));
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
