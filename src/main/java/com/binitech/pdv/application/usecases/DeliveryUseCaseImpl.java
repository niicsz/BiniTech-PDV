package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort;
import com.binitech.pdv.application.ports.inbound.PixUseCasePort;
import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.DeliveryOrderRepositoryPort;
import com.binitech.pdv.application.ports.outbound.DeliveryStoreRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.DeliveryAddress;
import com.binitech.pdv.domain.DeliveryOrder;
import com.binitech.pdv.domain.DeliveryOrderItem;
import com.binitech.pdv.domain.DeliveryStore;
import com.binitech.pdv.domain.DeliveryWhatsappMessage;
import com.binitech.pdv.domain.Money;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.DeliveryFulfillment;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

public class DeliveryUseCaseImpl implements DeliveryUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(DeliveryUseCaseImpl.class);
  private static final Set<Role> MANAGERS =
      EnumSet.of(Role.SUPER_ADMIN, Role.ADMIN, Role.TENANT_ADMIN);
  private static final int ORDER_LIST_LIMIT = 200;
  private static final int MAX_QUANTITY = 99;
  private static final String UNAVAILABLE = "Delivery indisponível.";

  private final boolean featureEnabled;
  private final DeliveryStoreRepositoryPort stores;
  private final DeliveryOrderRepositoryPort orders;
  private final TenantRepositoryPort tenants;
  private final ProductRepositoryPort products;
  private final SaleUseCasePort sales;
  private final PixUseCasePort pix;
  private final String frontendUrl;
  private final Clock clock;

  public record Options(boolean featureEnabled, String frontendUrl, Clock clock) {}

  public DeliveryUseCaseImpl(
      Options options,
      DeliveryStoreRepositoryPort stores,
      DeliveryOrderRepositoryPort orders,
      TenantRepositoryPort tenants,
      ProductRepositoryPort products,
      SaleUseCasePort sales,
      PixUseCasePort pix) {
    this.featureEnabled = options.featureEnabled();
    this.stores = stores;
    this.orders = orders;
    this.tenants = tenants;
    this.products = products;
    this.sales = sales;
    this.pix = pix;
    this.frontendUrl = withoutTrailingSlash(options.frontendUrl());
    this.clock = options.clock();
  }

  private static String withoutTrailingSlash(String url) {
    int end = url.length();
    while (end > 0 && url.charAt(end - 1) == '/') {
      end--;
    }
    return url.substring(0, end);
  }

  @Override
  public StoreView getSettings(String tenantId) {
    ensureFeature();
    Tenant tenant = tenant(tenantId);
    return new StoreView(tenant, storeOf(tenantId));
  }

  @Override
  public StoreView updateSettings(String tenantId, DeliveryStore changes, Role role) {
    ensureFeature();
    if (!MANAGERS.contains(role)) {
      throw new AccessDeniedException("Só o administrador da loja altera o delivery.");
    }
    Tenant tenant = tenant(tenantId);
    String whatsapp = normalizePhone(changes.getWhatsapp());
    if (changes.isEnabled() && whatsapp == null) {
      throw new BusinessException("Informe o WhatsApp da loja com DDD para receber os pedidos.");
    }
    if (!changes.isDeliveryAvailable() && !changes.isPickupAvailable()) {
      throw new BusinessException("Ative entrega, retirada ou as duas.");
    }
    if (changes.getAcceptedPayments() == null || changes.getAcceptedPayments().isEmpty()) {
      throw new BusinessException("Escolha ao menos uma forma de pagamento.");
    }
    DeliveryStore store = storeOf(tenantId);
    store.setEnabled(changes.isEnabled());
    store.setOpen(changes.isOpen());
    store.setWhatsapp(whatsapp);
    store.setMessage(trimToNull(changes.getMessage()));
    store.setDeliveryFee(Money.round(changes.getDeliveryFee()));
    store.setMinimumOrder(Money.round(changes.getMinimumOrder()));
    store.setEstimatedMinutes(changes.getEstimatedMinutes());
    store.setDeliveryAvailable(changes.isDeliveryAvailable());
    store.setPickupAvailable(changes.isPickupAvailable());
    store.setPickupAddress(trimToNull(changes.getPickupAddress()));
    store.setAcceptedPayments(EnumSet.copyOf(changes.getAcceptedPayments()));
    store.setPixKey(trimToNull(changes.getPixKey()));
    store.setUpdatedAt(clock.instant());
    if (log.isInfoEnabled()) {
      log.info(
          "Delivery atualizado: tenantId={} ativo={} aberto={}",
          LogSanitizer.maskId(tenantId),
          store.isEnabled(),
          store.isOpen());
    }
    return new StoreView(tenant, stores.save(store));
  }

  @Override
  public List<DeliveryOrder> listOrders(String tenantId, DeliveryOrderStatus status) {
    ensureFeature();
    return orders.findRecent(tenantId, status, ORDER_LIST_LIMIT);
  }

  @Override
  public DeliveryOrder updateStatus(String orderId, String tenantId, DeliveryOrderStatus target) {
    ensureFeature();
    DeliveryOrder order = order(orderId, tenantId);
    order.ensureCanMoveTo(target);
    if (!orders.changeStatus(orderId, tenantId, order.getStatus(), target)) {
      throw new BusinessException("O pedido mudou enquanto você atualizava. Recarregue a lista.");
    }
    return order(orderId, tenantId);
  }

  @Override
  public DeliveryOrder complete(
      String orderId, PaymentMethod method, String userId, String tenantId) {
    ensureFeature();
    DeliveryOrder order = order(orderId, tenantId);
    PaymentMethod paidWith = order.isPaid() ? PaymentMethod.PIX : method;
    if (paidWith == null || paidWith == PaymentMethod.CREDIARIO) {
      throw new BusinessException("Escolha como o cliente pagou: dinheiro, cartão ou Pix.");
    }
    order.ensureCanComplete();
    DeliveryOrderStatus previous = order.getStatus();
    if (!orders.changeStatus(orderId, tenantId, previous, DeliveryOrderStatus.COMPLETED)) {
      throw new BusinessException("O pedido mudou enquanto você atualizava. Recarregue a lista.");
    }
    Sale created;
    try {
      created = sales.createSale(saleFrom(order, paidWith), userId, tenantId);
    } catch (RuntimeException e) {
      orders.changeStatus(orderId, tenantId, DeliveryOrderStatus.COMPLETED, previous);
      throw e;
    }
    orders.attachSale(orderId, tenantId, created.getId());
    if (log.isInfoEnabled()) {
      log.info(
          "Pedido de delivery concluído: pedido={} venda={}",
          LogSanitizer.maskId(orderId),
          LogSanitizer.maskId(created.getId()));
    }
    return order(orderId, tenantId);
  }

  @Override
  public PublicStore getPublicStore(String slug) {
    ensureFeature();
    Tenant tenant = publicTenant(slug);
    DeliveryStore store = enabledStore(tenant);
    List<Product> catalog =
        products.findDeliveryCatalog(tenant.getId()).stream()
            .sorted(
                Comparator.comparing((Product p) -> categoryOf(p).toLowerCase())
                    .thenComparing(p -> p.getDescription().toLowerCase()))
            .toList();
    boolean pixIntegrated =
        store.accepts(DeliveryPaymentMethod.PIX) && pix.isActive(tenant.getId());
    return new PublicStore(tenant, store, catalog, pixIntegrated);
  }

  @Override
  public PlacedOrder placeOrder(String slug, OrderRequest request) {
    ensureFeature();
    Tenant tenant = publicTenant(slug);
    DeliveryStore store = enabledStore(tenant);
    if (!store.isOpen()) {
      throw new BusinessException("A loja está fechada agora. Tente mais tarde.");
    }
    DeliveryOrder order = new DeliveryOrder();
    order.setTenantId(tenant.getId());
    order.setCustomerName(requireText(request.customerName(), "Informe seu nome."));
    order.setCustomerPhone(customerPhone(request.customerPhone()));
    order.setFulfillment(fulfillment(request.fulfillment(), store));
    order.setAddress(addressFor(order.getFulfillment(), request.address()));
    order.setPaymentMethod(payment(request.paymentMethod(), store));
    order.setNotes(trimToNull(request.notes()));
    order.setItems(items(request.items(), tenant.getId()));
    order.recalculate(store.getDeliveryFee());
    if (Money.compare(order.getSubtotal(), store.getMinimumOrder()) < 0) {
      throw new BusinessException(
          "O pedido mínimo é "
              + DeliveryWhatsappMessage.money(store.getMinimumOrder())
              + " sem a taxa de entrega.");
    }
    order.setChangeFor(changeFor(order, request.changeFor()));
    Instant now = clock.instant();
    order.setId(UUID.randomUUID().toString());
    order.setNumber(stores.nextOrderNumber(tenant.getId()));
    boolean payNow =
        order.getPaymentMethod() == DeliveryPaymentMethod.PIX && pix.isActive(tenant.getId());
    order.setStatus(payNow ? DeliveryOrderStatus.AWAITING_PAYMENT : DeliveryOrderStatus.RECEIVED);
    order.setCreatedAt(now);
    order.setUpdatedAt(now);
    DeliveryOrder saved = orders.save(order);
    PixCharge charge = payNow ? openCharge(saved, tenant) : null;
    if (log.isInfoEnabled()) {
      log.info(
          "Pedido de delivery recebido: tenantId={} numero={} itens={}",
          LogSanitizer.maskId(tenant.getId()),
          saved.getNumber(),
          saved.getItems().size());
    }
    return placed(saved, store, tenant, charge);
  }

  @Override
  public PlacedOrder trackOrder(String slug, String orderId) {
    ensureFeature();
    Tenant tenant = publicTenant(slug);
    DeliveryStore store = enabledStore(tenant);
    DeliveryOrder order =
        orders
            .findByIdAndTenantId(orderId, tenant.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
    if (!order.isAwaitingPayment() || order.getPixChargeId() == null) {
      return placed(order, store, tenant, null);
    }
    Optional<PixCharge> charge = pix.checkOrderCharge(tenant.getId(), order.getPixChargeId());
    DeliveryOrder current = order(orderId, tenant.getId());
    return placed(
        current,
        store,
        tenant,
        current.isAwaitingPayment() ? charge.filter(PixCharge::isPending).orElse(null) : null);
  }

  private PixCharge openCharge(DeliveryOrder order, Tenant tenant) {
    try {
      PixCharge charge =
          pix.createOrderCharge(
              new PixUseCasePort.OrderCharge(
                  tenant.getId(),
                  order.getId(),
                  order.getTotal(),
                  "Pedido #" + order.getNumber() + " - " + tenant.getName()));
      orders.attachPixCharge(order.getId(), tenant.getId(), charge.getId());
      order.setPixChargeId(charge.getId());
      return charge;
    } catch (RuntimeException e) {
      orders.changeStatus(
          order.getId(),
          tenant.getId(),
          DeliveryOrderStatus.AWAITING_PAYMENT,
          DeliveryOrderStatus.CANCELLED);
      if (log.isWarnEnabled()) {
        log.warn(
            "Não foi possível gerar o Pix do pedido: pedido={} erro={}",
            LogSanitizer.maskId(order.getId()),
            e.getMessage());
      }
      throw new BusinessException(
          "Não foi possível gerar o Pix agora. Tente de novo ou escolha outra forma de pagamento.");
    }
  }

  private PlacedOrder placed(
      DeliveryOrder order, DeliveryStore store, Tenant tenant, PixCharge charge) {
    if (order.isAwaitingPayment() || expiredWithoutPayment(order)) {
      return new PlacedOrder(order, store, null, charge);
    }
    String trackUrl = frontendUrl + "/pedir/" + tenant.getSlug() + "/pedido/" + order.getId();
    String text = DeliveryWhatsappMessage.text(order, store, tenant.getName(), trackUrl);
    return new PlacedOrder(
        order, store, DeliveryWhatsappMessage.url(store.getWhatsapp(), text), null);
  }

  private static boolean expiredWithoutPayment(DeliveryOrder order) {
    return order.getStatus() == DeliveryOrderStatus.CANCELLED
        && order.getPixChargeId() != null
        && !order.isPaid();
  }

  private Sale saleFrom(DeliveryOrder order, PaymentMethod method) {
    Sale sale = new Sale();
    List<SaleItem> saleItems = new ArrayList<>();
    for (DeliveryOrderItem item : order.getItems()) {
      SaleItem saleItem = new SaleItem();
      saleItem.setProductId(item.productId());
      saleItem.setQuantity(item.quantity());
      saleItems.add(saleItem);
    }
    sale.setItems(saleItems);
    Payment payment = new Payment();
    payment.setMethod(method);
    payment.setAmount(order.getSubtotal());
    sale.setPayments(List.of(payment));
    sale.setCustomerName(order.getCustomerName());
    sale.setCustomerPhone(order.getCustomerPhone());
    return sale;
  }

  private List<DeliveryOrderItem> items(List<OrderLine> lines, String tenantId) {
    if (lines == null || lines.isEmpty()) {
      throw new BusinessException("Adicione ao menos um produto ao pedido.");
    }
    Map<String, Integer> totals = new LinkedHashMap<>();
    for (OrderLine line : lines) {
      if (line.quantity() < 1 || line.quantity() > MAX_QUANTITY) {
        throw new BusinessException("Quantidade inválida no pedido.");
      }
      totals.merge(line.productId(), line.quantity(), Integer::sum);
    }
    Map<String, Product> catalog = new LinkedHashMap<>();
    for (Map.Entry<String, Integer> total : totals.entrySet()) {
      String productId = total.getKey();
      Product product =
          products
              .findById(productId)
              .filter(p -> tenantId.equals(p.getTenantId()))
              .filter(Product::isActive)
              .filter(Product::isDeliveryVisible)
              .orElseThrow(
                  () -> new BusinessException("Um dos produtos não está mais disponível."));
      if (product.getStockQuantity() < total.getValue()) {
        throw new BusinessException(
            "Temos só "
                + Math.max(0, product.getStockQuantity())
                + " de "
                + product.getDescription()
                + ".");
      }
      catalog.put(productId, product);
    }
    return lines.stream()
        .map(
            line -> {
              Product product = catalog.get(line.productId());
              return new DeliveryOrderItem(
                  product.getId(),
                  product.getDescription(),
                  Money.round(product.getPrice()),
                  line.quantity(),
                  trimToNull(line.notes()));
            })
        .toList();
  }

  private static DeliveryFulfillment fulfillment(
      DeliveryFulfillment requested, DeliveryStore store) {
    if (requested == DeliveryFulfillment.DELIVERY && store.isDeliveryAvailable()) {
      return requested;
    }
    if (requested == DeliveryFulfillment.PICKUP && store.isPickupAvailable()) {
      return requested;
    }
    throw new BusinessException("Essa forma de receber o pedido não está disponível.");
  }

  private static DeliveryAddress addressFor(
      DeliveryFulfillment fulfillment, DeliveryAddress address) {
    if (fulfillment == DeliveryFulfillment.PICKUP) {
      return null;
    }
    if (address == null) {
      throw new BusinessException("Informe o endereço de entrega.");
    }
    return new DeliveryAddress(
        requireText(address.street(), "Informe a rua."),
        requireText(address.number(), "Informe o número."),
        trimToNull(address.complement()),
        requireText(address.district(), "Informe o bairro."),
        trimToNull(address.reference()));
  }

  private static DeliveryPaymentMethod payment(DeliveryPaymentMethod method, DeliveryStore store) {
    if (method == null || !store.accepts(method)) {
      throw new BusinessException("Essa forma de pagamento não é aceita pela loja.");
    }
    return method;
  }

  private static Double changeFor(DeliveryOrder order, Double requested) {
    if (order.getPaymentMethod() != DeliveryPaymentMethod.CASH
        || requested == null
        || requested <= 0) {
      return null;
    }
    if (Money.compare(requested, order.getTotal()) < 0) {
      throw new BusinessException(
          "O troco precisa ser para um valor maior que o total de "
              + DeliveryWhatsappMessage.money(order.getTotal())
              + ".");
    }
    return Money.round(requested);
  }

  private static String customerPhone(String raw) {
    String digits = raw == null ? "" : raw.replaceAll("\\D", "");
    if (digits.startsWith("55") && digits.length() > 11) {
      digits = digits.substring(2);
    }
    if (digits.length() != 10 && digits.length() != 11) {
      throw new BusinessException("Informe um telefone com DDD.");
    }
    return "55" + digits;
  }

  static String normalizePhone(String raw) {
    if (raw == null) {
      return null;
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.isEmpty()) {
      return null;
    }
    if (digits.length() == 10 || digits.length() == 11) {
      return "55" + digits;
    }
    if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
      return digits;
    }
    throw new BusinessException("WhatsApp inválido. Use DDD + número, ex.: (11) 91234-5678.");
  }

  private DeliveryStore storeOf(String tenantId) {
    return stores.findByTenantId(tenantId).orElseGet(() -> DeliveryStore.defaults(tenantId));
  }

  private DeliveryStore enabledStore(Tenant tenant) {
    return stores
        .findByTenantId(tenant.getId())
        .filter(DeliveryStore::isEnabled)
        .filter(s -> s.getWhatsapp() != null)
        .orElseThrow(() -> new ResourceNotFoundException("Esta loja não tem delivery."));
  }

  private Tenant tenant(String tenantId) {
    return tenants
        .findById(tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada."));
  }

  private Tenant publicTenant(String slug) {
    Optional<Tenant> tenant = slug == null ? Optional.empty() : tenants.findBySlug(slug);
    return tenant
        .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
        .orElseThrow(() -> new ResourceNotFoundException("Esta loja não tem delivery."));
  }

  private DeliveryOrder order(String orderId, String tenantId) {
    return orders
        .findByIdAndTenantId(orderId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
  }

  private void ensureFeature() {
    if (!featureEnabled) {
      throw new ResourceNotFoundException(UNAVAILABLE);
    }
  }

  private static String categoryOf(Product product) {
    String category = product.getCategory();
    return category == null || category.isBlank() ? "Outros" : category.strip();
  }

  private static String requireText(String value, String message) {
    String trimmed = trimToNull(value);
    if (trimmed == null) {
      throw new BusinessException(message);
    }
    return trimmed;
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.strip();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
