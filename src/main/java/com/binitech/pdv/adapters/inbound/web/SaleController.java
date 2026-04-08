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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaleController implements SalesApi {

  private static final Logger log = LoggerFactory.getLogger(SaleController.class);

  private final SaleUseCasePort saleUseCase;
  private final WebMapper webMapper;

  public SaleController(SaleUseCasePort saleUseCase, WebMapper webMapper) {
    this.saleUseCase = saleUseCase;
    this.webMapper = webMapper;
  }

  @Override
  public ResponseEntity<SaleDTO> createSale(CreateSaleDTO createSaleDTO) {
    log.info("Criando venda com {} item(ns) para userId={}", createSaleDTO.getItems() != null ? createSaleDTO.getItems().size() : 0, getUserId());
    Sale sale = webMapper.toDomain(createSaleDTO);
    Sale created = saleUseCase.createSale(sale, getUserId());
    log.info("Venda criada com sucesso: id={} total={}", created.getId(), created.getTotalAmount());
    return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toDto(created));
  }

  @Override
  public ResponseEntity<List<SaleDTO>> listSales(
      LocalDate date, LocalDate startDate, LocalDate endDate) {
    String userId = getUserId();
    Role role = getUserRole();
    List<Sale> sales;

    if (date != null) {
      log.info("Listando vendas por dia: date={} userId={} role={}", date, userId, role);
      sales = saleUseCase.listSalesByDay(date, userId, role);
    } else if (startDate != null && endDate != null) {
      log.info("Listando vendas por período: startDate={} endDate={} userId={} role={}", startDate, endDate, userId, role);
      sales = saleUseCase.listSalesByPeriod(startDate, endDate, userId, role);
    } else {
      log.info("Listando todas as vendas: userId={} role={}", userId, role);
      sales = saleUseCase.listAll(userId, role);
    }

    log.info("Retornando {} venda(s)", sales.size());
    return ResponseEntity.ok(webMapper.toSaleDtoList(sales));
  }

  @Override
  public ResponseEntity<SaleDTO> getSaleById(String id) {
    log.info("Buscando venda por id={}", id);
    Sale sale = saleUseCase.findById(id, getUserId(), getUserRole());
    log.info("Venda encontrada: id={} total={}", sale.getId(), sale.getTotalAmount());
    return ResponseEntity.ok(webMapper.toDto(sale));
  }

  @Override
  public ResponseEntity<List<SaleDTO>> listDebtors() {
    String userId = getUserId();
    Role role = getUserRole();
    log.info("Listando devedores: userId={} role={}", userId, role);
    List<Sale> debtors = saleUseCase.listDebtors(userId, role);
    log.info("Retornando {} venda(s) em débito", debtors.size());
    return ResponseEntity.ok(webMapper.toSaleDtoList(debtors));
  }

  @Override
  public ResponseEntity<SaleDTO> markSaleAsPaid(String id) {
    log.info("Marcando venda como paga: id={}", id);
    Sale updated = saleUseCase.markAsPaid(id, getUserId(), getUserRole());
    log.info("Venda marcada como paga com sucesso: id={}", updated.getId());
    return ResponseEntity.ok(webMapper.toDto(updated));
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
