package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryImageKindDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliverySettingsDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.ProductDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapperImpl;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.StoreView;
import com.binitech.pdv.application.ports.inbound.ImageUseCasePort;
import com.binitech.pdv.domain.DeliveryStore;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.StoredImage;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.ImageKind;
import com.binitech.pdv.utils.enums.Role;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class ImageControllerTest {

  private static final String CDN = "https://cdn.binitech.example/images";
  private static final String ORIGIN = "/api/public/images";
  private static final byte[] FILE = {1, 2, 3};

  private final ImageUseCasePort images = mock(ImageUseCasePort.class);
  private final AuthenticatedUserProvider user = mock(AuthenticatedUserProvider.class);
  private ImageController controller;

  @BeforeEach
  void setUp() {
    ImageUrlResolver resolver = new ImageUrlResolver(CDN + "/", ORIGIN);
    WebMapper webMapper = new WebMapperImpl();
    ReflectionTestUtils.setField(webMapper, "imageUrlResolver", resolver);
    controller =
        new ImageController(
            images,
            new DeliveryWebMapper(resolver),
            webMapper,
            user,
            new UploadGate(4, java.time.Duration.ofMillis(10)));
    when(user.getTenantId()).thenReturn("t1");
    when(user.getUserId()).thenReturn("u1");
    when(user.getUserRole()).thenReturn(Role.TENANT_ADMIN);
  }

  private static StoreView view(String logo, String cover) {
    Tenant tenant = new Tenant();
    tenant.setId("t1");
    tenant.setSlug("loja");
    tenant.setName("Loja");
    DeliveryStore store = DeliveryStore.defaults("t1");
    store.setLogoKey(logo);
    store.setCoverKey(cover);
    return new StoreView(tenant, store);
  }

  @Test
  @DisplayName("Envio do logo devolve a configuração com a URL da CDN")
  void uploadLogo() {
    when(images.uploadStoreImage(eq("t1"), eq(Role.TENANT_ADMIN), eq(ImageKind.LOGO), any()))
        .thenReturn(view("images/t1/logo.png", null));

    DeliverySettingsDTO body =
        controller
            .uploadDeliveryImage(
                DeliveryImageKindDTO.LOGO, new MockMultipartFile("file", "l.png", null, FILE))
            .getBody();

    assertEquals(CDN + "/t1/logo.png", body.getLogoUrl());
    assertNull(body.getCoverUrl());
  }

  @Test
  @DisplayName("Remover a capa devolve a configuração sem a URL")
  void removeCover() {
    when(images.removeStoreImage("t1", Role.TENANT_ADMIN, ImageKind.COVER))
        .thenReturn(view(null, null));
    assertNull(controller.removeDeliveryImage(DeliveryImageKindDTO.COVER).getBody().getCoverUrl());
  }

  @Test
  @DisplayName("Foto do produto sai com a URL da CDN e some ao remover")
  void productImage() {
    Product product = new Product();
    product.setId("p1");
    product.setImageKey("images/t1/p.jpg");
    when(images.uploadProductImage(eq("p1"), eq("u1"), eq("t1"), eq(Role.TENANT_ADMIN), any()))
        .thenReturn(product);

    ProductDTO dto =
        controller
            .uploadProductImage("p1", new MockMultipartFile("file", "p.jpg", null, FILE))
            .getBody();
    assertEquals(CDN + "/t1/p.jpg", dto.getImageUrl());

    Product without = new Product();
    without.setId("p1");
    when(images.removeProductImage("p1", "u1", "t1", Role.TENANT_ADMIN)).thenReturn(without);
    assertNull(controller.removeProductImage("p1").getBody().getImageUrl());
  }

  @Test
  @DisplayName("Arquivo vazio ou ilegível vira erro de negócio")
  void emptyFile() throws IOException {
    MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
    assertThrows(
        BusinessException.class,
        () -> controller.uploadDeliveryImage(DeliveryImageKindDTO.LOGO, empty));
    assertThrows(
        BusinessException.class,
        () -> controller.uploadDeliveryImage(DeliveryImageKindDTO.LOGO, null));
    MultipartFile broken = mock(MultipartFile.class);
    when(broken.isEmpty()).thenReturn(false);
    when(broken.getBytes()).thenThrow(new IOException("rede"));
    assertThrows(BusinessException.class, () -> controller.uploadProductImage("p1", broken));
  }

  @Test
  @DisplayName("Imagem pública sai com cache imutável de um ano para a CDN guardar")
  void publicImage() throws IOException {
    when(images.publicImage("t1", "a.png"))
        .thenReturn(Optional.of(new StoredImage(FILE, "image/png")));

    ResponseEntity<Resource> response = controller.getPublicImage("t1", "a.png");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("max-age=31536000, public, immutable", response.getHeaders().getCacheControl());
    assertEquals("image/png", response.getHeaders().getContentType().toString());
    assertEquals("cross-origin", response.getHeaders().getFirst("Cross-Origin-Resource-Policy"));
    assertArrayEquals(FILE, response.getBody().getContentAsByteArray());
  }

  @Test
  @DisplayName("Imagem pública inexistente responde 404")
  void publicImageMissing() {
    when(images.publicImage("t1", "x.png")).thenReturn(Optional.empty());
    assertEquals(HttpStatus.NOT_FOUND, controller.getPublicImage("t1", "x.png").getStatusCode());
  }

  @Test
  @DisplayName("Sem CDN configurada a URL aponta para a origem do PDV")
  void resolverDefaultsToOrigin() {
    ImageUrlResolver origin = new ImageUrlResolver("", ORIGIN);
    assertEquals("/api/public/images/t1/a.jpg", origin.urlOf("images/t1/a.jpg"));
    assertEquals(
        "/api/public/images/t1/a.jpg", new ImageUrlResolver(null, ORIGIN).urlOf("images/t1/a.jpg"));
    assertNull(origin.urlOf(null));
  }

  @Test
  @DisplayName("Endereço de CDN sem https:// derruba a subida em vez de gerar URL quebrada")
  void resolverRejectsInvalidBase() {
    assertThrows(
        IllegalStateException.class, () -> new ImageUrlResolver("cdn.exemplo.com", ORIGIN));
  }
}
