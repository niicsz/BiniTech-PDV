package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.StoreView;
import com.binitech.pdv.application.ports.inbound.ImageUseCasePort;
import com.binitech.pdv.application.ports.outbound.DeliveryStoreRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ImageProcessorPort;
import com.binitech.pdv.application.ports.outbound.ImageStoragePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.DeliveryStore;
import com.binitech.pdv.domain.ImageKey;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.StoredImage;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.ImageKind;
import com.binitech.pdv.utils.enums.Role;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

public class ImageUseCaseImpl implements ImageUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(ImageUseCaseImpl.class);
  private static final Set<Role> MANAGERS =
      EnumSet.of(Role.SUPER_ADMIN, Role.ADMIN, Role.TENANT_ADMIN);
  private static final long MEGABYTE = 1024L * 1024L;

  private final Options options;
  private final ImageStoragePort storage;
  private final ImageProcessorPort processor;
  private final DeliveryStoreRepositoryPort stores;
  private final TenantRepositoryPort tenants;
  private final ProductRepositoryPort products;

  public record Options(boolean deliveryEnabled, long maxBytes, Clock clock) {}

  public ImageUseCaseImpl(
      Options options,
      ImageStoragePort storage,
      ImageProcessorPort processor,
      DeliveryStoreRepositoryPort stores,
      TenantRepositoryPort tenants,
      ProductRepositoryPort products) {
    this.options = options;
    this.storage = storage;
    this.processor = processor;
    this.stores = stores;
    this.tenants = tenants;
    this.products = products;
  }

  @Override
  public StoreView uploadStoreImage(String tenantId, Role role, ImageKind kind, byte[] content) {
    ensureStoreAccess(role, kind);
    Tenant tenant = tenant(tenantId);
    ensureStorage();
    StoredImage image = processor.process(validated(content), kind);
    DeliveryStore store = storeOf(tenantId);
    String previous = storeKey(store, kind);
    String key = upload(tenantId, image);
    setStoreKey(store, kind, key);
    DeliveryStore saved = saveOrDiscard(key, () -> saveStore(store));
    discard(previous);
    if (log.isInfoEnabled()) {
      log.info("Imagem {} da loja salva: tenantId={}", kind, LogSanitizer.maskId(tenantId));
    }
    return new StoreView(tenant, saved);
  }

  @Override
  public StoreView removeStoreImage(String tenantId, Role role, ImageKind kind) {
    ensureStoreAccess(role, kind);
    Tenant tenant = tenant(tenantId);
    DeliveryStore store = storeOf(tenantId);
    String previous = storeKey(store, kind);
    if (previous == null) {
      return new StoreView(tenant, store);
    }
    setStoreKey(store, kind, null);
    DeliveryStore saved = saveStore(store);
    discard(previous);
    return new StoreView(tenant, saved);
  }

  @Override
  public Product uploadProductImage(
      String productId, String userId, String tenantId, Role role, byte[] content) {
    Product product = ownedProduct(productId, userId, tenantId, role);
    ensureStorage();
    StoredImage image = processor.process(validated(content), ImageKind.PRODUCT);
    String previous = product.getImageKey();
    String key = upload(tenantId, image);
    product.setImageKey(key);
    Product saved = saveOrDiscard(key, () -> products.save(product));
    discard(previous);
    if (log.isInfoEnabled()) {
      log.info("Foto do produto salva: id={}", LogSanitizer.maskId(productId));
    }
    return saved;
  }

  @Override
  public Product removeProductImage(String productId, String userId, String tenantId, Role role) {
    Product product = ownedProduct(productId, userId, tenantId, role);
    String previous = product.getImageKey();
    if (previous == null) {
      return product;
    }
    product.setImageKey(null);
    Product saved = products.save(product);
    discard(previous);
    return saved;
  }

  @Override
  public Optional<StoredImage> publicImage(String tenantId, String file) {
    if (!storage.available()) {
      return Optional.empty();
    }
    return ImageKey.of(tenantId, file).flatMap(storage::get);
  }

  private void ensureStoreAccess(Role role, ImageKind kind) {
    if (!options.deliveryEnabled()) {
      throw new ResourceNotFoundException("Delivery indisponível.");
    }
    if (kind != ImageKind.LOGO && kind != ImageKind.COVER) {
      throw new BusinessException("Escolha logo ou capa.");
    }
    if (!MANAGERS.contains(role)) {
      throw new AccessDeniedException("Só o administrador da loja altera as imagens do delivery.");
    }
  }

  private void ensureStorage() {
    if (!storage.available()) {
      throw new BusinessException("O envio de imagens ainda não está disponível neste ambiente.");
    }
  }

  private byte[] validated(byte[] content) {
    if (content == null || content.length == 0) {
      throw new BusinessException("Escolha uma imagem para enviar.");
    }
    if (content.length > options.maxBytes()) {
      throw new BusinessException(
          "A imagem passa de "
              + Math.max(1, options.maxBytes() / MEGABYTE)
              + " MB. Envie um arquivo menor.");
    }
    return content;
  }

  private String upload(String tenantId, StoredImage image) {
    String key = ImageKey.newKey(tenantId, image.extension());
    storage.put(key, image);
    return key;
  }

  private <T> T saveOrDiscard(String key, Supplier<T> save) {
    try {
      return save.get();
    } catch (RuntimeException exception) {
      discard(key);
      throw exception;
    }
  }

  private void discard(String key) {
    if (key == null) {
      return;
    }
    try {
      storage.delete(key);
    } catch (RuntimeException exception) {
      log.warn("Não foi possível apagar a imagem antiga: {}", exception.getMessage());
    }
  }

  private DeliveryStore saveStore(DeliveryStore store) {
    store.setUpdatedAt(options.clock().instant());
    return stores.save(store);
  }

  private DeliveryStore storeOf(String tenantId) {
    return stores.findByTenantId(tenantId).orElseGet(() -> DeliveryStore.defaults(tenantId));
  }

  private Tenant tenant(String tenantId) {
    return tenants
        .findById(tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada."));
  }

  private Product ownedProduct(String productId, String userId, String tenantId, Role role) {
    Product product =
        products
            .findById(productId)
            .filter(p -> Objects.equals(p.getTenantId(), tenantId))
            .orElseThrow(() -> new ResourceNotFoundException("Produto", "id", productId));
    if (!MANAGERS.contains(role) && !Objects.equals(product.getUserId(), userId)) {
      throw new AccessDeniedException("Você não tem permissão para alterar este produto.");
    }
    return product;
  }

  private static String storeKey(DeliveryStore store, ImageKind kind) {
    return kind == ImageKind.LOGO ? store.getLogoKey() : store.getCoverKey();
  }

  private static void setStoreKey(DeliveryStore store, ImageKind kind, String key) {
    Consumer<String> setter = kind == ImageKind.LOGO ? store::setLogoKey : store::setCoverKey;
    setter.accept(key);
  }
}
