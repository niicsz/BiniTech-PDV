package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.Enum.Role;
import com.binitech.pdv.utils.LogSanitizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class SaleUseCaseImpl implements SaleUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(SaleUseCaseImpl.class);
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_PAGE_SIZE = 100;
  private static final int MAX_PAGE_SIZE = 200;

  private final SaleRepositoryPort saleRepository;
  private final ProductRepositoryPort productRepository;

  public SaleUseCaseImpl(
      SaleRepositoryPort saleRepository, ProductRepositoryPort productRepository) {
    this.saleRepository = saleRepository;
    this.productRepository = productRepository;
  }

  @Override
  @Transactional
  public Sale createSale(Sale sale, String userId) {
    log.info(
        "Criando venda com {} item(ns) para userId={}",
        sale.getItems().size(),
        LogSanitizer.maskId(userId));

    Map<String, Product> productsById = new HashMap<>();
    for (SaleItem item : sale.getItems()) {
      if (productsById.containsKey(item.getProductId())) {
        continue;
      }
      Product product =
          productRepository
              .findById(item.getProductId())
              .orElseThrow(
                  () -> {
                    log.error(
                        "Produto não encontrado ao criar venda: productId={}",
                        LogSanitizer.maskId(item.getProductId()));
                    return new ResourceNotFoundException("Produto", "id", item.getProductId());
                  });
      productsById.put(product.getId(), product);
    }

    for (SaleItem item : sale.getItems()) {
      Product product = productsById.get(item.getProductId());

      if (!product.getUserId().equals(userId)) {
        log.warn(
            "Produto não pertence ao usuário: productId={}", LogSanitizer.maskId(product.getId()));
        throw new BusinessException(
            "Produto não pertence ao seu catálogo: " + product.getDescription());
      }

      if (!product.isActive()) {
        log.warn(
            "Tentativa de venda de produto inativo: productId={}",
            LogSanitizer.maskId(product.getId()));
        throw new BusinessException("Produto inativo: " + product.getDescription());
      }

      if (!sale.isSkipStockValidation() && product.getStockQuantity() < item.getQuantity()) {
        log.warn(
            "Estoque insuficiente: productId={} disponível={} solicitado={}",
            LogSanitizer.maskId(product.getId()),
            product.getStockQuantity(),
            item.getQuantity());
        throw new BusinessException(
            String.format(
                "Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
                product.getDescription(), product.getStockQuantity(), item.getQuantity()));
      }

      item.setProductDescription(product.getDescription());
      item.setUnitPrice(product.getPrice());
      item.setCostPrice(product.getCostPrice());
      item.recalculateSubtotal();
    }

    sale.recalculate();
    sale.validate();

    for (SaleItem item : sale.getItems()) {
      Product product = productsById.get(item.getProductId());
      if (sale.isSkipStockValidation()) {
        int available = product.getStockQuantity();
        int toDecrease = Math.min(available, item.getQuantity());
        if (toDecrease > 0) {
          product.decreaseStock(toDecrease);
          log.info(
              "Estoque decrementado (skip validation): productId={} decrementado={} restante={}",
              LogSanitizer.maskId(product.getId()),
              toDecrease,
              product.getStockQuantity());
        }
      } else {
        product.decreaseStock(item.getQuantity());
        log.info(
            "Estoque decrementado: productId={} quantidade={} restante={}",
            LogSanitizer.maskId(product.getId()),
            item.getQuantity(),
            product.getStockQuantity());
      }
    }

    for (Product product : productsById.values()) {
      productRepository.save(product);
    }

    sale.setUserId(userId);
    sale.setTimestamp(LocalDateTime.now());

    Sale saved = saleRepository.save(sale);
    log.info(
        "Venda criada com sucesso: id={} total={}",
        LogSanitizer.maskId(saved.getId()),
        saved.getTotalAmount());
    return saved;
  }

  @Override
  public Sale findById(String id, String userId, Role role) {
    log.debug("Buscando venda por id={} userId={} role={}", id, userId, role);
    Sale sale =
        saleRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn("Venda não encontrada: id={}", id);
                  return new ResourceNotFoundException("Venda", "id", id);
                });

    if (role != Role.ADMIN && !sale.getUserId().equals(userId)) {
      log.warn(
          "Acesso negado à venda: id={} ownerUserId={} requestUserId={}",
          id,
          sale.getUserId(),
          userId);
      throw new ResourceNotFoundException("Venda", "id", id);
    }

    return sale;
  }

  @Override
  public List<Sale> listSalesByDay(LocalDate date, String userId, Role role) {
    log.debug("Listando vendas por dia: date={} userId={} role={}", date, userId, role);
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.atTime(LocalTime.MAX);
    if (role == Role.ADMIN) {
      List<Sale> sales = saleRepository.findByTimestampBetween(start, end);
      log.debug("Vendas encontradas (ADMIN) por dia: {}", sales.size());
      return sales;
    }
    List<Sale> sales = saleRepository.findByTimestampBetweenAndUserId(start, end, userId);
    log.debug("Vendas encontradas por dia para userId={}: {}", userId, sales.size());
    return sales;
  }

  @Override
  public List<Sale> listSalesByPeriod(
      LocalDate startDate, LocalDate endDate, String userId, Role role) {
    log.debug(
        "Listando vendas por período: {} a {} userId={} role={}", startDate, endDate, userId, role);
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(LocalTime.MAX);
    if (role == Role.ADMIN) {
      List<Sale> sales = saleRepository.findByTimestampBetween(start, end);
      log.debug("Vendas encontradas (ADMIN) por período: {}", sales.size());
      return sales;
    }
    List<Sale> sales = saleRepository.findByTimestampBetweenAndUserId(start, end, userId);
    log.debug("Vendas encontradas por período para userId={}: {}", userId, sales.size());
    return sales;
  }

  @Override
  public List<Sale> listAll(String userId, Role role) {
    log.debug("Listando todas as vendas: userId={} role={}", LogSanitizer.maskId(userId), role);
    return listAll(userId, role, DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
  }

  @Override
  public List<Sale> listAll(String userId, Role role, int page, int size) {
    int sanitizedPage = sanitizePage(page);
    int sanitizedSize = sanitizeSize(size);
    log.debug(
        "Listando vendas paginadas: userId={} role={} page={} size={}",
        LogSanitizer.maskId(userId),
        role,
        sanitizedPage,
        sanitizedSize);
    if (role == Role.ADMIN) {
      List<Sale> sales = saleRepository.findAll(sanitizedPage, sanitizedSize);
      log.debug("Total de vendas retornadas (ADMIN): {}", sales.size());
      return sales;
    }
    List<Sale> sales = saleRepository.findAllByUserId(userId, sanitizedPage, sanitizedSize);
    log.debug(
        "Total de vendas retornadas para userId={}: {}", LogSanitizer.maskId(userId), sales.size());
    return sales;
  }

  private int sanitizePage(Integer page) {
    return page == null || page < 0 ? DEFAULT_PAGE : page;
  }

  private int sanitizeSize(Integer size) {
    if (size == null || size <= 0) {
      return DEFAULT_PAGE_SIZE;
    }
    return Math.min(size, MAX_PAGE_SIZE);
  }

  @Override
  public List<Sale> listDebtors(String userId, Role role) {
    log.debug("Listando devedores: userId={} role={}", userId, role);
    if (role == Role.ADMIN) {
      List<Sale> debtors = saleRepository.findDebtors();
      log.debug("Total de débitos (ADMIN): {}", debtors.size());
      return debtors;
    }
    List<Sale> debtors = saleRepository.findDebtorsByUserId(userId);
    log.debug("Total de débitos para userId={}: {}", userId, debtors.size());
    return debtors;
  }

  @Override
  public Sale markAsPaid(String id, String userId, Role role) {
    log.info(
        "Marcando venda como paga: id={} userId={} role={}",
        LogSanitizer.maskId(id),
        LogSanitizer.maskId(userId),
        role);
    Sale sale =
        saleRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn("Venda não encontrada para marcar como paga: id={}", id);
                  return new ResourceNotFoundException("Venda", "id", id);
                });

    if (role != Role.ADMIN && !sale.getUserId().equals(userId)) {
      log.warn(
          "Sem permissão para marcar venda como paga: id={} ownerUserId={} requestUserId={}",
          id,
          sale.getUserId(),
          userId);
      throw new ResourceNotFoundException("Venda", "id", id);
    }

    sale.setPaid(true);
    Sale saved = saleRepository.save(sale);
    log.info("Venda marcada como paga com sucesso: id={}", LogSanitizer.maskId(saved.getId()));
    return saved;
  }
}
