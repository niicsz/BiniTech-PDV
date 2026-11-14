package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.StoreView;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.StoredImage;
import com.binitech.pdv.utils.enums.ImageKind;
import com.binitech.pdv.utils.enums.Role;
import java.util.Optional;

public interface ImageUseCasePort {

  StoreView uploadStoreImage(String tenantId, Role role, ImageKind kind, byte[] content);

  StoreView removeStoreImage(String tenantId, Role role, ImageKind kind);

  Product uploadProductImage(
      String productId, String userId, String tenantId, Role role, byte[] content);

  Product removeProductImage(String productId, String userId, String tenantId, Role role);

  Optional<StoredImage> publicImage(String tenantId, String file);
}
