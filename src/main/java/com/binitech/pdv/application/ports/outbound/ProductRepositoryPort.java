package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {

  Product save(Product product);

  Optional<Product> findById(String id);

  Optional<Product> findByBarcode(String barcode);

  List<Product> findAll();

  void deleteById(String id);

  boolean existsByBarcode(String barcode);

  List<Product> findAllByUserId(String userId);

  Optional<Product> findByBarcodeAndUserId(String barcode, String userId);

  boolean existsByBarcodeAndUserId(String barcode, String userId);
}
