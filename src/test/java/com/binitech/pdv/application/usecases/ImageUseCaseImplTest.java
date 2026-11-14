package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.StoreView;
import com.binitech.pdv.application.ports.outbound.DeliveryStoreRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ImageProcessorPort;
import com.binitech.pdv.application.ports.outbound.ImageStoragePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.DeliveryStore;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.StoredImage;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.ImageKind;
import com.binitech.pdv.utils.enums.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageUseCaseImplTest {

  private static final String TENANT = "tenant-1";
  private static final byte[] UPLOAD = {1, 2, 3};
  private static final StoredImage JPEG = new StoredImage(new byte[] {9, 9}, "image/jpeg");
  private static final StoredImage PNG = new StoredImage(new byte[] {8, 8}, "image/png");

  @Mock private ImageStoragePort storage;
  @Mock private ImageProcessorPort processor;
  @Mock private DeliveryStoreRepositoryPort stores;
  @Mock private TenantRepositoryPort tenants;
  @Mock private ProductRepositoryPort products;

  private ImageUseCaseImpl useCase;
  private Tenant tenant;

  @BeforeEach
  void setUp() {
    useCase = create(true);
    tenant = new Tenant();
    tenant.setId(TENANT);
    when(tenants.findById(TENANT)).thenReturn(Optional.of(tenant));
    when(storage.available()).thenReturn(true);
    when(processor.process(any(), eq(ImageKind.LOGO))).thenReturn(PNG);
    when(processor.process(any(), eq(ImageKind.COVER))).thenReturn(JPEG);
    when(processor.process(any(), eq(ImageKind.PRODUCT))).thenReturn(JPEG);
    when(stores.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(products.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  private ImageUseCaseImpl create(boolean deliveryEnabled) {
    return new ImageUseCaseImpl(
        new ImageUseCaseImpl.Options(
            deliveryEnabled, 8L * 1024 * 1024, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)),
        storage,
        processor,
        stores,
        tenants,
        products);
  }

  private Product product(String owner) {
    Product product = new Product();
    product.setId("p1");
    product.setTenantId(TENANT);
    product.setUserId(owner);
    when(products.findById("p1")).thenReturn(Optional.of(product));
    return product;
  }

  @Test
  @DisplayName("Logo novo vai para o bucket como PNG, entra na loja e o antigo é apagado")
  void uploadLogoReplacesPrevious() {
    DeliveryStore store = DeliveryStore.defaults(TENANT);
    store.setLogoKey("images/tenant-1/old.png");
    when(stores.findByTenantId(TENANT)).thenReturn(Optional.of(store));

    StoreView view = useCase.uploadStoreImage(TENANT, Role.TENANT_ADMIN, ImageKind.LOGO, UPLOAD);

    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    verify(storage).put(key.capture(), eq(PNG));
    assertTrue(key.getValue().matches("images/tenant-1/[0-9a-f]{32}\\.png"));
    assertEquals(key.getValue(), view.store().getLogoKey());
    assertEquals(Instant.EPOCH, view.store().getUpdatedAt());
    assertSame(tenant, view.tenant());
    verify(storage).delete("images/tenant-1/old.png");
  }

  @Test
  @DisplayName("Capa sem loja configurada cria a configuração padrão com a imagem")
  void uploadCoverWithoutStore() {
    when(stores.findByTenantId(TENANT)).thenReturn(Optional.empty());

    StoreView view = useCase.uploadStoreImage(TENANT, Role.ADMIN, ImageKind.COVER, UPLOAD);

    assertTrue(view.store().getCoverKey().endsWith(".jpg"));
    verify(storage, never()).delete(anyString());
  }

  @Test
  @DisplayName("Operador não troca imagens da loja")
  void storeImageNeedsManager() {
    assertThrows(
        AccessDeniedException.class,
        () -> useCase.uploadStoreImage(TENANT, Role.OPERATOR, ImageKind.LOGO, UPLOAD));
    verify(storage, never()).put(anyString(), any());
  }

  @Test
  @DisplayName("Com o delivery desligado as imagens da loja respondem 404")
  void storeImageNeedsDeliveryFeature() {
    ImageUseCaseImpl disabled = create(false);
    assertThrows(
        ResourceNotFoundException.class,
        () -> disabled.uploadStoreImage(TENANT, Role.ADMIN, ImageKind.LOGO, UPLOAD));
  }

  @Test
  @DisplayName("Loja só aceita logo ou capa")
  void storeImageRejectsProductKind() {
    assertThrows(
        BusinessException.class,
        () -> useCase.uploadStoreImage(TENANT, Role.ADMIN, ImageKind.PRODUCT, UPLOAD));
  }

  @Test
  @DisplayName("Sem armazenamento configurado o envio é recusado com mensagem clara")
  void uploadNeedsStorage() {
    when(storage.available()).thenReturn(false);
    BusinessException error =
        assertThrows(
            BusinessException.class,
            () -> useCase.uploadStoreImage(TENANT, Role.ADMIN, ImageKind.LOGO, UPLOAD));
    assertTrue(error.getMessage().contains("não está disponível"));
  }

  @Test
  @DisplayName("Arquivo vazio ou acima do limite é recusado antes de processar")
  void validatesSize() {
    assertThrows(
        BusinessException.class,
        () -> useCase.uploadStoreImage(TENANT, Role.ADMIN, ImageKind.LOGO, new byte[0]));
    ImageUseCaseImpl small =
        new ImageUseCaseImpl(
            new ImageUseCaseImpl.Options(true, 2, Clock.systemUTC()),
            storage,
            processor,
            stores,
            tenants,
            products);
    BusinessException error =
        assertThrows(
            BusinessException.class,
            () -> small.uploadStoreImage(TENANT, Role.ADMIN, ImageKind.LOGO, UPLOAD));
    assertTrue(error.getMessage().contains("1 MB"));
    verify(processor, never()).process(any(), any());
  }

  @Test
  @DisplayName("Se salvar a loja falhar, a imagem recém-enviada é apagada")
  void discardsUploadWhenSaveFails() {
    when(stores.findByTenantId(TENANT)).thenReturn(Optional.empty());
    when(stores.save(any())).thenThrow(new IllegalStateException("mongo fora"));

    assertThrows(
        IllegalStateException.class,
        () -> useCase.uploadStoreImage(TENANT, Role.ADMIN, ImageKind.LOGO, UPLOAD));

    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    verify(storage).put(key.capture(), any());
    verify(storage).delete(key.getValue());
  }

  @Test
  @DisplayName("Remover a capa limpa o campo e apaga o arquivo")
  void removeCover() {
    DeliveryStore store = DeliveryStore.defaults(TENANT);
    store.setCoverKey("images/tenant-1/cover.jpg");
    when(stores.findByTenantId(TENANT)).thenReturn(Optional.of(store));

    StoreView view = useCase.removeStoreImage(TENANT, Role.ADMIN, ImageKind.COVER);

    assertNull(view.store().getCoverKey());
    verify(storage).delete("images/tenant-1/cover.jpg");
  }

  @Test
  @DisplayName("Remover imagem que não existe não grava nada")
  void removeMissingStoreImage() {
    when(stores.findByTenantId(TENANT)).thenReturn(Optional.of(DeliveryStore.defaults(TENANT)));

    useCase.removeStoreImage(TENANT, Role.ADMIN, ImageKind.LOGO);

    verify(stores, never()).save(any());
    verify(storage, never()).delete(anyString());
  }

  @Test
  @DisplayName("Falha ao apagar a imagem antiga não derruba o envio")
  void deleteFailureIsIgnored() {
    DeliveryStore store = DeliveryStore.defaults(TENANT);
    store.setLogoKey("images/tenant-1/old.png");
    when(stores.findByTenantId(TENANT)).thenReturn(Optional.of(store));
    doThrow(new IllegalStateException("bucket fora")).when(storage).delete(anyString());

    StoreView view = useCase.uploadStoreImage(TENANT, Role.ADMIN, ImageKind.LOGO, UPLOAD);

    assertTrue(view.store().getLogoKey().endsWith(".png"));
  }

  @Test
  @DisplayName("Dono do produto envia a foto e a antiga é apagada")
  void ownerUploadsProductImage() {
    Product product = product("u1");
    product.setImageKey("images/tenant-1/old.jpg");

    Product saved = useCase.uploadProductImage("p1", "u1", TENANT, Role.OPERATOR, UPLOAD);

    assertTrue(saved.getImageKey().matches("images/tenant-1/[0-9a-f]{32}\\.jpg"));
    verify(storage).delete("images/tenant-1/old.jpg");
  }

  @Test
  @DisplayName("Operador não troca a foto de produto de outra pessoa; admin troca")
  void productImagePermission() {
    product("outro");
    assertThrows(
        AccessDeniedException.class,
        () -> useCase.uploadProductImage("p1", "u1", TENANT, Role.OPERATOR, UPLOAD));
    Product saved = useCase.uploadProductImage("p1", "u1", TENANT, Role.TENANT_ADMIN, UPLOAD);
    assertTrue(saved.getImageKey().endsWith(".jpg"));
  }

  @Test
  @DisplayName("Produto de outra loja aparece como não encontrado")
  void productFromOtherTenant() {
    product("u1").setTenantId("tenant-2");
    assertThrows(
        ResourceNotFoundException.class,
        () -> useCase.uploadProductImage("p1", "u1", TENANT, Role.ADMIN, UPLOAD));
  }

  @Test
  @DisplayName("Remover a foto do produto apaga o arquivo; sem foto não faz nada")
  void removeProductImage() {
    Product product = product("u1");
    product.setImageKey("images/tenant-1/photo.jpg");

    Product saved = useCase.removeProductImage("p1", "u1", TENANT, Role.OPERATOR);

    assertNull(saved.getImageKey());
    verify(storage).delete("images/tenant-1/photo.jpg");
    useCase.removeProductImage("p1", "u1", TENANT, Role.OPERATOR);
    verify(products).save(any());
  }

  @Test
  @DisplayName("Imagem pública só é buscada com nome válido e armazenamento ligado")
  void publicImage() {
    when(storage.get("images/tenant-1/" + "a".repeat(32) + ".jpg")).thenReturn(Optional.of(JPEG));

    assertEquals(Optional.of(JPEG), useCase.publicImage(TENANT, "a".repeat(32) + ".jpg"));
    assertTrue(useCase.publicImage(TENANT, "../segredo.jpg").isEmpty());
    assertTrue(useCase.publicImage("../x", "a".repeat(32) + ".jpg").isEmpty());
    when(storage.available()).thenReturn(false);
    assertTrue(useCase.publicImage(TENANT, "a".repeat(32) + ".jpg").isEmpty());
  }
}
