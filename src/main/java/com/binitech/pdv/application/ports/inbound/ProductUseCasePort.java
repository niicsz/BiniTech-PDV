package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Product;
import com.binitech.pdv.utils.Enum.Role;
import java.util.List;

public interface ProductUseCasePort {

  Product createProduct(Product product, String userId);

  Product updateProduct(
      String id, Product product, String userId, Role role, Boolean activeOverride);

  void deactivateProduct(String id, String userId, Role role);

  Product findById(String id, String userId, Role role);

  Product findByBarcode(String barcode, String userId);

  List<Product> listAll(String userId, Role role);

  List<Product> listAll(String userId, Role role, int page, int size);
}
