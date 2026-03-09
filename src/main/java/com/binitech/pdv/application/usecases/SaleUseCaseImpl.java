package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.SaleUseCasePort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class SaleUseCaseImpl implements SaleUseCasePort {

  private final SaleRepositoryPort saleRepository;
  private final ProductRepositoryPort productRepository;

  public SaleUseCaseImpl(
      SaleRepositoryPort saleRepository, ProductRepositoryPort productRepository) {
    this.saleRepository = saleRepository;
    this.productRepository = productRepository;
  }

  @Override
  public Sale createSale(Sale sale) {
    for (SaleItem item : sale.getItems()) {
      Product product =
          productRepository
              .findById(item.getProductId())
              .orElseThrow(
                  () -> new ResourceNotFoundException("Produto", "id", item.getProductId()));

      if (!product.isActive()) {
        throw new BusinessException("Produto inativo: " + product.getDescription());
      }

      if (product.getStockQuantity() < item.getQuantity()) {
        throw new BusinessException(
            String.format(
                "Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
                product.getDescription(), product.getStockQuantity(), item.getQuantity()));
      }

      item.setProductDescription(product.getDescription());
      item.setUnitPrice(product.getPrice());
      item.recalculateSubtotal();
    }

    sale.validate();

    for (SaleItem item : sale.getItems()) {
      Product product =
          productRepository
              .findById(item.getProductId())
              .orElseThrow(
                  () -> new ResourceNotFoundException("Produto", "id", item.getProductId()));
      product.decreaseStock(item.getQuantity());
      productRepository.save(product);
    }

    sale.setTimestamp(LocalDateTime.now());

    return saleRepository.save(sale);
  }

  @Override
  public Sale findById(String id) {
    return saleRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venda", "id", id));
  }

  @Override
  public List<Sale> listSalesByDay(LocalDate date) {
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.atTime(LocalTime.MAX);
    return saleRepository.findByTimestampBetween(start, end);
  }

  @Override
  public List<Sale> listSalesByPeriod(LocalDate startDate, LocalDate endDate) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(LocalTime.MAX);
    return saleRepository.findByTimestampBetween(start, end);
  }

  @Override
  public List<Sale> listAll() {
    return saleRepository.findAll();
  }
}
