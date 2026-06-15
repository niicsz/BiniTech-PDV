package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Product;
import com.binitech.pdv.utils.Enum.Role;
import java.util.List;

public interface ProductUseCasePort {

  Product createProduct(Product product, String userId, String tenantId);

  Product updateProduct(
      String id,
      Product product,
      String userId,
      String tenantId,
      Role role,
      Boolean activeOverride);

  void deactivateProduct(String id, String userId, String tenantId, Role role);

  Product findById(String id, String tenantId);

  Product findByBarcode(String barcode, String tenantId);

  List<Product> listAll(String tenantId);

  List<Product> listAll(String tenantId, int page, int size);
}
