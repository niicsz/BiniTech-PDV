package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.ImagesApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryImageKindDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliverySettingsDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.ProductDTO;
import com.binitech.pdv.adapters.inbound.web.mapper.WebMapper;
import com.binitech.pdv.application.ports.inbound.ImageUseCasePort;
import com.binitech.pdv.domain.StoredImage;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.ImageKind;
import java.io.IOException;
import java.time.Duration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImageController implements ImagesApi {

  private static final CacheControl IMMUTABLE =
      CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

  private final ImageUseCasePort images;
  private final DeliveryWebMapper deliveryMapper;
  private final WebMapper webMapper;
  private final AuthenticatedUserProvider userProvider;

  public ImageController(
      ImageUseCasePort images,
      DeliveryWebMapper deliveryMapper,
      WebMapper webMapper,
      AuthenticatedUserProvider userProvider) {
    this.images = images;
    this.deliveryMapper = deliveryMapper;
    this.webMapper = webMapper;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<DeliverySettingsDTO> uploadDeliveryImage(
      DeliveryImageKindDTO kind, MultipartFile file) {
    return ResponseEntity.ok(
        deliveryMapper.toDto(
            images.uploadStoreImage(
                userProvider.getTenantId(),
                userProvider.getUserRole(),
                ImageKind.valueOf(kind.getValue()),
                bytes(file))));
  }

  @Override
  public ResponseEntity<DeliverySettingsDTO> removeDeliveryImage(DeliveryImageKindDTO kind) {
    return ResponseEntity.ok(
        deliveryMapper.toDto(
            images.removeStoreImage(
                userProvider.getTenantId(),
                userProvider.getUserRole(),
                ImageKind.valueOf(kind.getValue()))));
  }

  @Override
  public ResponseEntity<ProductDTO> uploadProductImage(String id, MultipartFile file) {
    return ResponseEntity.ok(
        webMapper.toDto(
            images.uploadProductImage(
                id,
                userProvider.getUserId(),
                userProvider.getTenantId(),
                userProvider.getUserRole(),
                bytes(file))));
  }

  @Override
  public ResponseEntity<ProductDTO> removeProductImage(String id) {
    return ResponseEntity.ok(
        webMapper.toDto(
            images.removeProductImage(
                id,
                userProvider.getUserId(),
                userProvider.getTenantId(),
                userProvider.getUserRole())));
  }

  @Override
  public ResponseEntity<Resource> getPublicImage(String tenantId, String file) {
    return images
        .publicImage(tenantId, file)
        .map(ImageController::imageResponse)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  private static ResponseEntity<Resource> imageResponse(StoredImage image) {
    return ResponseEntity.ok()
        .cacheControl(IMMUTABLE)
        .contentType(MediaType.parseMediaType(image.contentType()))
        .contentLength(image.size())
        .header("Cross-Origin-Resource-Policy", "cross-origin")
        .header("X-Content-Type-Options", "nosniff")
        .body(new ByteArrayResource(image.content()));
  }

  private static byte[] bytes(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("Escolha uma imagem para enviar.");
    }
    try {
      return file.getBytes();
    } catch (IOException _) {
      throw new BusinessException("Não foi possível receber o arquivo. Tente de novo.");
    }
  }
}
