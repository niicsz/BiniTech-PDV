package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Product;
import java.util.List;

public interface ProductUseCasePort {

  Product createProduct(Product product);

  Product updateProduct(String id, Product product);

  void deleteProduct(String id);

  Product findById(String id);

  Product findByBarcode(String barcode);

  List<Product> listAll();
}
