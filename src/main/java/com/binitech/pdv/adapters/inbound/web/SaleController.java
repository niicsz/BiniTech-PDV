package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.SalesApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateSaleDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.SaleDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.utils.Enum.Role;
import com.binitech.pdv.utils.LogSanitizer;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaleController implements SalesApi {

  private static final Logger log = LoggerFactory.getLogger(SaleController.class);

  private final SaleUseCasePort saleUseCase;
  private final WebMapper webMapper;
  private final AuthenticatedUserProvider userProvider;

  public SaleController(
      SaleUseCasePort saleUseCase, WebMapper webMapper, AuthenticatedUserProvider userProvider) {
    this.saleUseCase = saleUseCase;
    this.webMapper = webMapper;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<SaleDTO> createSale(CreateSaleDTO createSaleDTO) {
    int itemCount = createSaleDTO.getItems() != null ? createSaleDTO.getItems().size() : 0;
    log.info("Criando venda com {} item(ns)", itemCount);
    Sale sale = webMapper.toDomain(createSaleDTO);
    Sale created = saleUseCase.createSale(sale, userProvider.getUserId());
    log.info(
        "Venda criada com sucesso: id={} total={}",
        LogSanitizer.maskId(created.getId()),
        created.getTotalAmount());
    return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toDto(created));
  }

  @Override
  public ResponseEntity<List<SaleDTO>> listSales(
      LocalDate date, LocalDate startDate, LocalDate endDate, Integer page, Integer size) {
    String userId = userProvider.getUserId();
    Role role = userProvider.getUserRole();
    List<Sale> sales;

    if (date != null) {
      log.debug("Listando vendas por dia: date={} role={}", date, role);
      sales = saleUseCase.listSalesByDay(date, userId, role);
    } else if (startDate != null && endDate != null) {
      log.debug(
          "Listando vendas por período: startDate={} endDate={} role={}", startDate, endDate, role);
      sales = saleUseCase.listSalesByPeriod(startDate, endDate, userId, role);
    } else {
      log.debug("Listando todas as vendas: role={}", role);
      sales = saleUseCase.listAll(userId, role, page, size);
    }

    log.info("Retornando {} venda(s)", sales.size());
    return ResponseEntity.ok(webMapper.toSaleDtoList(sales));
  }

  @Override
  public ResponseEntity<SaleDTO> getSaleById(String id) {
    log.debug("Buscando venda por id={}", LogSanitizer.maskId(id));
    Sale sale = saleUseCase.findById(id, userProvider.getUserId(), userProvider.getUserRole());
    log.debug(
        "Venda encontrada: id={} total={}",
        LogSanitizer.maskId(sale.getId()),
        sale.getTotalAmount());
    return ResponseEntity.ok(webMapper.toDto(sale));
  }

  @Override
  public ResponseEntity<List<SaleDTO>> listDebtors() {
    Role role = userProvider.getUserRole();
    log.debug("Listando devedores: role={}", role);
    List<Sale> debtors = saleUseCase.listDebtors(userProvider.getUserId(), role);
    if (log.isInfoEnabled()) {
      log.info("Retornando {} venda(s) em débito", debtors.size());
    }
    return ResponseEntity.ok(webMapper.toSaleDtoList(debtors));
  }

  @Override
  public ResponseEntity<SaleDTO> markSaleAsPaid(String id) {
    if (log.isInfoEnabled()) {
      log.info("Marcando venda como paga: id={}", LogSanitizer.maskId(id));
    }
    Sale updated = saleUseCase.markAsPaid(id, userProvider.getUserId(), userProvider.getUserRole());
    if (log.isInfoEnabled()) {
      log.info("Venda marcada como paga com sucesso: id={}", LogSanitizer.maskId(updated.getId()));
    }
    return ResponseEntity.ok(webMapper.toDto(updated));
  }
}
