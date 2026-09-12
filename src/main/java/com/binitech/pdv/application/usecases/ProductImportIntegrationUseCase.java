package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.ProductImportIntegrationUseCasePort;
import com.binitech.pdv.application.ports.outbound.*;
import com.binitech.pdv.application.ports.outbound.ProductImportReceiptRepositoryPort.Receipt;
import com.binitech.pdv.domain.*;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;

public class ProductImportIntegrationUseCase implements ProductImportIntegrationUseCasePort {
  private final ProductRepositoryPort products;
  private final UserRepositoryPort users;
  private final TenantRepositoryPort tenants;
  private final ProductImportReceiptRepositoryPort receipts;

  public ProductImportIntegrationUseCase(
      ProductRepositoryPort products,
      UserRepositoryPort users,
      TenantRepositoryPort tenants,
      ProductImportReceiptRepositoryPort receipts) {
    this.products = products;
    this.users = users;
    this.tenants = tenants;
    this.receipts = receipts;
  }

  @Override
  public void authorize(ImportIdentity identity) {
    authorizedUser(identity);
  }

  private User authorizedUser(ImportIdentity identity) {
    if (identity == null || identity.userId() == null || identity.tenantId() == null)
      throw denied();
    User user =
        users
            .findByIdAndTenantId(identity.userId(), identity.tenantId())
            .orElseThrow(ProductImportIntegrationUseCase::denied);
    if (!user.isActive()) throw denied();
    Tenant tenant =
        tenants.findById(identity.tenantId()).orElseThrow(ProductImportIntegrationUseCase::denied);
    if (tenant.getStatus() != TenantStatus.ACTIVE) throw denied();
    return user;
  }

  @Override
  public List<ImportProduct> lookup(ImportIdentity identity, Set<String> barcodes) {
    authorize(identity);
    return products.findAllByBarcodesAndTenantId(barcodes, identity.tenantId()).stream()
        .map(this::toImportProduct)
        .toList();
  }

  @Override
  public List<ImportResult> apply(
      ImportIdentity identity,
      ImportMode mode,
      StockMode stockMode,
      Set<UpdateField> updateFields,
      List<ImportCommand> commands) {
    User actor = authorizedUser(identity);
    BatchKeys keys = validateAndCollectKeys(commands);
    ProcessingState state =
        new ProcessingState(
            loadReceipts(identity.tenantId(), keys.operationIds()),
            loadProducts(identity.tenantId(), keys.barcodes()),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>());
    Set<UpdateField> fields = updateFields == null ? Set.of() : updateFields;
    commands.forEach(
        command -> process(command, identity, actor.getRole(), mode, stockMode, fields, state));
    return persist(identity.tenantId(), state);
  }

  private BatchKeys validateAndCollectKeys(List<ImportCommand> commands) {
    if (commands == null || commands.isEmpty() || commands.size() > 500)
      throw new BusinessException("Lote deve conter de 1 a 500 produtos.");
    Set<String> operationIds = new HashSet<>();
    Set<String> barcodes = new HashSet<>();
    for (ImportCommand command : commands) {
      if (!operationIds.add(command.operationId()))
        throw new BusinessException("operationId duplicado no lote.");
      if (!barcodes.add(command.barcode()))
        throw new BusinessException("Código de barras duplicado no lote.");
    }
    return new BatchKeys(operationIds, barcodes);
  }

  private Map<String, Receipt> loadReceipts(String tenantId, Set<String> operationIds) {
    Map<String, Receipt> prior = new HashMap<>();
    receipts
        .findByTenantIdAndOperationIds(tenantId, operationIds)
        .forEach(r -> prior.put(r.operationId(), r));
    return prior;
  }

  private Map<String, Product> loadProducts(String tenantId, Set<String> barcodes) {
    Map<String, Product> existing = new HashMap<>();
    products
        .findAllByBarcodesAndTenantId(barcodes, tenantId)
        .forEach(p -> existing.put(p.getBarcode(), p));
    return existing;
  }

  private void process(
      ImportCommand command,
      ImportIdentity identity,
      Role actorRole,
      ImportMode mode,
      StockMode stockMode,
      Set<UpdateField> fields,
      ProcessingState state) {
    Receipt receipt = state.receipts().get(command.operationId());
    if (receipt != null) {
      state
          .results()
          .add(
              new ImportResult(
                  command.lineNumber(),
                  ResultAction.valueOf(receipt.action()),
                  receipt.productId(),
                  null));
      return;
    }
    try {
      processProduct(command, identity, actorRole, mode, stockMode, fields, state);
    } catch (RuntimeException exception) {
      state
          .results()
          .add(
              new ImportResult(
                  command.lineNumber(), ResultAction.ERROR, null, safeMessage(exception)));
    }
  }

  private void processProduct(
      ImportCommand command,
      ImportIdentity identity,
      Role actorRole,
      ImportMode mode,
      StockMode stockMode,
      Set<UpdateField> fields,
      ProcessingState state) {
    Product product = state.products().get(command.barcode());
    if (product == null) {
      Product created = create(command, identity, stockMode);
      state.changed().add(created);
      state.products().put(created.getBarcode(), created);
      state.pending().add(new PendingResult(command, created, ResultAction.CREATED));
      return;
    }
    if (mode == ImportMode.CREATE_ONLY) {
      state.pending().add(new PendingResult(command, product, ResultAction.IGNORED));
      return;
    }
    authorizeUpdate(identity.userId(), actorRole, product);
    update(product, command, stockMode, fields);
    state.changed().add(product);
    state.pending().add(new PendingResult(command, product, ResultAction.UPDATED));
  }

  private static void authorizeUpdate(String userId, Role actorRole, Product product) {
    if (!privileged(actorRole) && !Objects.equals(product.getUserId(), userId)) throw denied();
  }

  private List<ImportResult> persist(String tenantId, ProcessingState state) {
    Map<String, Product> savedByBarcode = new HashMap<>();
    products
        .saveAll(state.changed())
        .forEach(product -> savedByBarcode.put(product.getBarcode(), product));
    List<Receipt> newReceipts = new ArrayList<>();
    for (PendingResult item : state.pending()) {
      Product saved = savedByBarcode.getOrDefault(item.product().getBarcode(), item.product());
      state
          .results()
          .add(new ImportResult(item.command().lineNumber(), item.action(), saved.getId(), null));
      newReceipts.add(
          new Receipt(
              item.command().operationId(),
              item.command().lineNumber(),
              item.action().name(),
              saved.getId()));
    }
    if (!newReceipts.isEmpty()) receipts.saveAll(tenantId, newReceipts);
    state.results().sort(Comparator.comparingInt(ImportResult::lineNumber));
    return state.results();
  }

  private Product create(ImportCommand command, ImportIdentity identity, StockMode stockMode) {
    Product product = new Product();
    product.setBarcode(command.barcode());
    product.setDescription(command.name());
    product.setPrice(command.price().doubleValue());
    product.setCostPrice(command.cost() == null ? 0 : command.cost().doubleValue());
    product.setStockQuantity(stockMode == StockMode.IGNORE ? 0 : command.stockQuantity());
    product.setCategory(command.category());
    product.setActive(command.active() == null || command.active());
    product.setUserId(identity.userId());
    product.setTenantId(identity.tenantId());
    return product;
  }

  private void update(
      Product product, ImportCommand command, StockMode stockMode, Set<UpdateField> fields) {
    if (fields.contains(UpdateField.NAME)) product.setDescription(command.name());
    if (fields.contains(UpdateField.PRICE)) product.setPrice(command.price().doubleValue());
    if (fields.contains(UpdateField.COST) && command.cost() != null)
      product.setCostPrice(command.cost().doubleValue());
    if (fields.contains(UpdateField.CATEGORY)) product.setCategory(command.category());
    if (fields.contains(UpdateField.ACTIVE) && command.active() != null)
      product.setActive(command.active());
    if (stockMode == StockMode.REPLACE) product.setStockQuantity(command.stockQuantity());
    else if (stockMode == StockMode.INCREMENT) product.increaseStock(command.stockQuantity());
  }

  private ImportProduct toImportProduct(Product p) {
    return new ImportProduct(
        p.getId(),
        p.getBarcode(),
        p.getDescription(),
        BigDecimal.valueOf(p.getPrice()),
        BigDecimal.valueOf(p.getCostPrice()),
        p.getStockQuantity(),
        p.getCategory(),
        p.isActive(),
        p.getUserId());
  }

  private static boolean privileged(Role role) {
    return role == Role.ADMIN || role == Role.TENANT_ADMIN || role == Role.SUPER_ADMIN;
  }

  private static AccessDeniedException denied() {
    return new AccessDeniedException("Usuário ou tenant sem permissão para importar.");
  }

  private static String safeMessage(RuntimeException e) {
    return e.getMessage() == null ? "Produto recusado." : e.getMessage();
  }

  private record BatchKeys(Set<String> operationIds, Set<String> barcodes) {}

  private record ProcessingState(
      Map<String, Receipt> receipts,
      Map<String, Product> products,
      List<Product> changed,
      List<PendingResult> pending,
      List<ImportResult> results) {}

  private record PendingResult(ImportCommand command, Product product, ResultAction action) {}
}
