package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.PreSaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.PreSaleRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.PreSale;
import com.binitech.pdv.domain.PreSaleItem;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.PreSaleStatus;
import com.binitech.pdv.utils.enums.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

public class PreSaleUseCaseImpl implements PreSaleUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(PreSaleUseCaseImpl.class);
  private static final Set<Role> MANAGERS =
      EnumSet.of(Role.SUPER_ADMIN, Role.ADMIN, Role.TENANT_ADMIN);
  private static final int MAX_LINES = 100;
  private static final int MAX_QUANTITY = 999;
  private static final int MAX_NOTES = 280;
  private static final int LIST_LIMIT = 200;

  private final PreSaleRepositoryPort preSales;
  private final ProductRepositoryPort products;
  private final Clock clock;

  public PreSaleUseCaseImpl(
      PreSaleRepositoryPort preSales, ProductRepositoryPort products, Clock clock) {
    this.preSales = preSales;
    this.products = products;
    this.clock = clock;
  }

  @Override
  public PreSale create(Attendant attendant, List<ItemRequest> items, String notes) {
    String tenantId = requireTenant(attendant);
    PreSale preSale = new PreSale();
    preSale.setTenantId(tenantId);
    preSale.setItems(items(items, tenantId));
    preSale.recalculate();
    preSale.setNotes(notes(notes));
    preSale.setAttendantId(attendant.userId());
    preSale.setAttendantName(attendant.username());
    preSale.setStatus(PreSaleStatus.OPEN);
    Instant now = clock.instant();
    preSale.setId(UUID.randomUUID().toString());
    preSale.setNumber(preSales.nextNumber(tenantId));
    preSale.setCreatedAt(now);
    preSale.setUpdatedAt(now);
    PreSale saved = preSales.save(preSale);
    if (log.isInfoEnabled()) {
      log.info(
          "Comanda aberta: tenantId={} numero={} itens={}",
          LogSanitizer.maskId(tenantId),
          saved.getNumber(),
          saved.getItems().size());
    }
    return saved;
  }

  @Override
  public List<PreSale> list(Attendant attendant, PreSaleStatus status) {
    return preSales.findRecent(requireTenant(attendant), status, LIST_LIMIT);
  }

  @Override
  public PreSale findByNumber(Attendant attendant, long number) {
    return preSales
        .findByNumber(requireTenant(attendant), number)
        .orElseThrow(
            () -> new ResourceNotFoundException("Comanda #" + number + " não encontrada."));
  }

  @Override
  public PreSale cancel(Attendant attendant, String id) {
    String tenantId = requireTenant(attendant);
    PreSale preSale =
        preSales
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Comanda não encontrada."));
    if (!MANAGERS.contains(attendant.role())
        && !Objects.equals(preSale.getAttendantId(), attendant.userId())) {
      throw new AccessDeniedException("Só quem abriu a comanda ou um administrador pode cancelar.");
    }
    preSale.ensureOpen();
    if (!preSales.close(id, tenantId, PreSaleStatus.CANCELLED, clock.instant())) {
      throw new BusinessException(
          "A comanda #" + preSale.getNumber() + " mudou enquanto você cancelava. Busque de novo.");
    }
    if (log.isInfoEnabled()) {
      log.info(
          "Comanda cancelada: tenantId={} numero={}",
          LogSanitizer.maskId(tenantId),
          preSale.getNumber());
    }
    return preSales.findByIdAndTenantId(id, tenantId).orElse(preSale);
  }

  private List<PreSaleItem> items(List<ItemRequest> requested, String tenantId) {
    if (requested == null || requested.isEmpty()) {
      throw new BusinessException("Adicione ao menos um produto à comanda.");
    }
    Map<String, Integer> quantities = new LinkedHashMap<>();
    for (ItemRequest item : requested) {
      if (item == null || item.productId() == null || item.productId().isBlank()) {
        throw new BusinessException("Produto inválido na comanda.");
      }
      int quantity = item.quantity() == null ? 0 : item.quantity();
      if (quantity < 1) {
        throw new BusinessException("A quantidade de cada produto precisa ser pelo menos 1.");
      }
      quantities.merge(item.productId(), quantity, Integer::sum);
    }
    if (quantities.size() > MAX_LINES) {
      throw new BusinessException("A comanda aceita até " + MAX_LINES + " produtos diferentes.");
    }
    List<PreSaleItem> items = new ArrayList<>();
    quantities.forEach(
        (productId, quantity) -> {
          Product product = product(productId, tenantId);
          if (quantity > MAX_QUANTITY) {
            throw new BusinessException(
                "Quantidade acima do limite de "
                    + MAX_QUANTITY
                    + " para "
                    + product.getDescription());
          }
          items.add(
              new PreSaleItem(
                  product.getId(), product.getDescription(), product.getPrice(), quantity));
        });
    return items;
  }

  private Product product(String productId, String tenantId) {
    Product product =
        products
            .findById(productId)
            .filter(p -> Objects.equals(p.getTenantId(), tenantId))
            .orElseThrow(() -> new BusinessException("Produto não encontrado no seu catálogo."));
    if (!product.isActive()) {
      throw new BusinessException("Produto inativo: " + product.getDescription());
    }
    return product;
  }

  private static String notes(String notes) {
    if (notes == null || notes.isBlank()) {
      return null;
    }
    String trimmed = notes.strip();
    if (trimmed.length() > MAX_NOTES) {
      throw new BusinessException("A observação pode ter até " + MAX_NOTES + " caracteres.");
    }
    return trimmed;
  }

  private static String requireTenant(Attendant attendant) {
    if (attendant == null || attendant.tenantId() == null) {
      throw new AccessDeniedException("Comandas exigem uma loja.");
    }
    return attendant.tenantId();
  }
}
