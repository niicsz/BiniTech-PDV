package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.SaleReturn;
import com.binitech.pdv.domain.SaleReturnRequest;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.SaleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

public class SaleUseCaseImpl implements SaleUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(SaleUseCaseImpl.class);
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_PAGE_SIZE = 100;
  private static final int MAX_PAGE_SIZE = 200;

  private final SaleRepositoryPort saleRepository;
  private final ProductRepositoryPort productRepository;
  private final ZoneId businessZone;
  private final SaleFiscalPublisher fiscalPublisher;

  public SaleUseCaseImpl(
      SaleRepositoryPort saleRepository,
      ProductRepositoryPort productRepository,
      ZoneId businessZone) {
    this(saleRepository, productRepository, businessZone, null);
  }

  public SaleUseCaseImpl(
      SaleRepositoryPort saleRepository,
      ProductRepositoryPort productRepository,
      ZoneId businessZone,
      SaleFiscalPublisher fiscalPublisher) {
    this.saleRepository = saleRepository;
    this.productRepository = productRepository;
    this.businessZone = businessZone;
    this.fiscalPublisher = fiscalPublisher;
  }

  @Override
  @Transactional
  public Sale createSale(Sale sale, String userId, String tenantId) {
    if (log.isInfoEnabled()) {
      log.info(
          "Criando venda com {} item(ns) para tenantId={}",
          sale.getItems().size(),
          LogSanitizer.maskId(tenantId));
    }

    Map<String, Product> productsById = loadProductsForSale(sale.getItems());
    validateAndEnrichItems(sale, productsById, tenantId);

    sale.recalculate();
    sale.validate();

    decreaseStockForItems(sale, productsById);
    productsById.values().forEach(productRepository::save);

    sale.setUserId(userId);
    sale.setTenantId(tenantId);
    sale.setTimestamp(LocalDateTime.now(ZoneId.systemDefault()));
    sale.setCustomerCpf(digitsOrNull(sale.getCustomerCpf()));
    sale.setFiscalPending(fiscalPublisher != null);

    Sale saved = saleRepository.save(sale);
    publishToFiscal(saved, productsById);
    if (log.isInfoEnabled()) {
      log.info(
          "Venda criada com sucesso: id={} total={}",
          LogSanitizer.maskId(saved.getId()),
          saved.getTotalAmount());
    }
    return saved;
  }

  private void publishToFiscal(Sale saved, Map<String, Product> productsById) {
    if (fiscalPublisher == null) {
      return;
    }
    try {
      fiscalPublisher.publish(saved, productsById);
    } catch (RuntimeException e) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Venda salva, envio ao serviço fiscal fica para o reenvio: id={} erro={}",
            LogSanitizer.maskId(saved.getId()),
            e.getMessage());
      }
    }
  }

  private static String digitsOrNull(String value) {
    if (value == null) {
      return null;
    }
    String digits = value.replaceAll("\\D", "");
    return digits.isEmpty() ? null : digits;
  }

  private Map<String, Product> loadProductsForSale(List<SaleItem> items) {
    Map<String, Product> productsById = new HashMap<>();
    for (SaleItem item : items) {
      if (productsById.containsKey(item.getProductId())) {
        continue;
      }
      Product product =
          productRepository
              .findById(item.getProductId())
              .orElseThrow(
                  () -> {
                    if (log.isErrorEnabled()) {
                      log.error(
                          "Produto não encontrado ao criar venda: productId={}",
                          LogSanitizer.maskId(item.getProductId()));
                    }
                    return new ResourceNotFoundException("Produto", "id", item.getProductId());
                  });
      productsById.put(product.getId(), product);
    }
    return productsById;
  }

  private void validateAndEnrichItems(
      Sale sale, Map<String, Product> productsById, String tenantId) {
    for (SaleItem item : sale.getItems()) {
      Product product = productsById.get(item.getProductId());
      validateProductTenant(product, tenantId);
      validateProductActive(product);
      validateStock(sale, item, product);
      enrichItem(item, product);
    }
  }

  private void validateProductTenant(Product product, String tenantId) {
    if (!Objects.equals(product.getTenantId(), tenantId)) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Produto não pertence ao tenant: productId={}", LogSanitizer.maskId(product.getId()));
      }
      throw new BusinessException(
          "Produto não pertence ao seu catálogo: " + product.getDescription());
    }
  }

  private void validateProductActive(Product product) {
    if (!product.isActive()) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Tentativa de venda de produto inativo: productId={}",
            LogSanitizer.maskId(product.getId()));
      }
      throw new BusinessException("Produto inativo: " + product.getDescription());
    }
  }

  private void validateStock(Sale sale, SaleItem item, Product product) {
    if (!sale.isSkipStockValidation() && product.getStockQuantity() < item.getQuantity()) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Estoque insuficiente: productId={} disponível={} solicitado={}",
            LogSanitizer.maskId(product.getId()),
            product.getStockQuantity(),
            item.getQuantity());
      }
      throw new BusinessException(
          String.format(
              "Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
              product.getDescription(), product.getStockQuantity(), item.getQuantity()));
    }
  }

  private void enrichItem(SaleItem item, Product product) {
    item.setProductDescription(product.getDescription());
    item.setUnitPrice(product.getPrice());
    item.setCostPrice(product.getCostPrice());
    item.recalculateSubtotal();
  }

  @Override
  public Sale findById(String id, String tenantId) {
    log.debug("Buscando venda por id={} tenantId={}", id, LogSanitizer.maskId(tenantId));
    return findOwnedSale(id, tenantId);
  }

  @Override
  public List<Sale> listSalesByDay(LocalDate date, String tenantId) {
    log.debug("Listando vendas por dia: date={} tenantId={}", date, LogSanitizer.maskId(tenantId));
    Instant startInclusive = date.atStartOfDay(businessZone).toInstant();
    Instant endExclusive = date.plusDays(1).atStartOfDay(businessZone).toInstant();
    return saleRepository.findByTimestampRangeAndTenantId(startInclusive, endExclusive, tenantId);
  }

  @Override
  public List<Sale> listSalesByPeriod(LocalDate startDate, LocalDate endDate, String tenantId) {
    log.debug(
        "Listando vendas por período: {} a {} tenantId={}",
        startDate,
        endDate,
        LogSanitizer.maskId(tenantId));
    if (endDate.isBefore(startDate)) {
      throw new BusinessException("A data final não pode ser anterior à data inicial.");
    }
    Instant startInclusive = startDate.atStartOfDay(businessZone).toInstant();
    Instant endExclusive = endDate.plusDays(1).atStartOfDay(businessZone).toInstant();
    return saleRepository.findByTimestampRangeAndTenantId(startInclusive, endExclusive, tenantId);
  }

  @Override
  public List<Sale> listAll(String tenantId) {
    if (log.isDebugEnabled()) {
      log.debug("Listando todas as vendas: tenantId={}", LogSanitizer.maskId(tenantId));
    }
    return listAll(tenantId, DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
  }

  @Override
  public List<Sale> listAll(String tenantId, int page, int size) {
    int sanitizedPage = sanitizePage(page);
    int sanitizedSize = sanitizeSize(size);
    if (log.isDebugEnabled()) {
      log.debug(
          "Listando vendas paginadas: tenantId={} page={} size={}",
          LogSanitizer.maskId(tenantId),
          sanitizedPage,
          sanitizedSize);
    }
    return saleRepository.findAllByTenantId(tenantId, sanitizedPage, sanitizedSize);
  }

  private void decreaseStockForItems(Sale sale, Map<String, Product> productsById) {
    for (SaleItem item : sale.getItems()) {
      Product product = productsById.get(item.getProductId());
      if (sale.isSkipStockValidation()) {
        int available = product.getStockQuantity();
        int toDecrease = Math.min(available, item.getQuantity());
        if (toDecrease > 0) {
          product.decreaseStock(toDecrease);
          if (log.isInfoEnabled()) {
            log.info(
                "Estoque decrementado (skip validation): productId={} decrementado={} restante={}",
                LogSanitizer.maskId(product.getId()),
                toDecrease,
                product.getStockQuantity());
          }
        }
      } else {
        product.decreaseStock(item.getQuantity());
        if (log.isInfoEnabled()) {
          log.info(
              "Estoque decrementado: productId={} quantidade={} restante={}",
              LogSanitizer.maskId(product.getId()),
              item.getQuantity(),
              product.getStockQuantity());
        }
      }
    }
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
  public List<Sale> listDebtors(String tenantId) {
    log.debug("Listando devedores: tenantId={}", LogSanitizer.maskId(tenantId));
    return saleRepository.findDebtorsByTenantId(tenantId);
  }

  @Override
  public Sale markAsPaid(String id, String userId, String tenantId, Role role) {
    if (log.isInfoEnabled()) {
      log.info(
          "Marcando venda como paga: id={} tenantId={} role={}",
          LogSanitizer.maskId(id),
          LogSanitizer.maskId(tenantId),
          role);
    }
    Sale sale = findOwnedSale(id, tenantId);

    if (!isPrivileged(role) && !Objects.equals(sale.getUserId(), userId)) {
      log.warn("Sem permissão para marcar venda como paga: id={}", LogSanitizer.maskId(id));
      throw new ResourceNotFoundException("Venda", "id", id);
    }

    if (sale.getStatus() == SaleStatus.RETURNED) {
      throw new BusinessException("Esta venda foi devolvida por completo e não tem débito.");
    }

    sale.setPaid(true);
    Sale saved = saleRepository.save(sale);
    if (log.isInfoEnabled()) {
      log.info("Venda marcada como paga com sucesso: id={}", LogSanitizer.maskId(saved.getId()));
    }
    return saved;
  }

  @Override
  public Sale registerReturn(
      String id, SaleReturnRequest request, String userId, String tenantId, Role role) {
    if (!isPrivileged(role)) {
      log.warn("Devolução negada para papel sem permissão: role={}", role);
      throw new AccessDeniedException("Só o administrador da loja pode registrar devoluções.");
    }
    Sale sale = findOwnedSale(id, tenantId);
    int expectedVersion = sale.getReturnsVersion();
    SaleReturn saleReturn =
        sale.registerReturn(
            request,
            UUID.randomUUID().toString(),
            userId,
            LocalDateTime.now(ZoneId.systemDefault()));

    if (!saleRepository.appendReturn(sale, expectedVersion)) {
      if (log.isWarnEnabled()) {
        log.warn("Devolução concorrente na mesma venda: id={}", LogSanitizer.maskId(id));
      }
      throw new BusinessException(
          "Esta venda acabou de receber outra devolução. Abra a venda de novo e confira.");
    }
    sale.setReturnsVersion(expectedVersion + 1);

    if (saleReturn.isRestocked()) {
      restock(saleReturn, tenantId);
    }
    if (log.isInfoEnabled()) {
      log.info(
          "Devolução registrada: venda={} valor={} forma={} estoque={}",
          LogSanitizer.maskId(id),
          saleReturn.getRefundAmount(),
          saleReturn.getRefundMethod(),
          saleReturn.isRestocked());
    }
    return sale;
  }

  private void restock(SaleReturn saleReturn, String tenantId) {
    for (SaleItem item : saleReturn.getItems()) {
      productRepository
          .findById(item.getProductId())
          .filter(product -> Objects.equals(product.getTenantId(), tenantId))
          .ifPresentOrElse(
              product -> {
                product.increaseStock(item.getQuantity());
                productRepository.save(product);
              },
              () -> {
                if (log.isWarnEnabled()) {
                  log.warn(
                      "Produto da devolução não existe mais, estoque não reposto: productId={}",
                      LogSanitizer.maskId(item.getProductId()));
                }
              });
    }
  }

  private Sale findOwnedSale(String id, String tenantId) {
    Sale sale =
        saleRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn("Venda não encontrada: id={}", LogSanitizer.maskId(id));
                  return new ResourceNotFoundException("Venda", "id", id);
                });
    if (!Objects.equals(sale.getTenantId(), tenantId)) {
      log.warn(
          "Acesso negado à venda de outro tenant: id={} tenantId={}",
          LogSanitizer.maskId(id),
          LogSanitizer.maskId(tenantId));
      throw new ResourceNotFoundException("Venda", "id", id);
    }
    return sale;
  }

  private boolean isPrivileged(Role role) {
    return role == Role.ADMIN || role == Role.TENANT_ADMIN || role == Role.SUPER_ADMIN;
  }
}
