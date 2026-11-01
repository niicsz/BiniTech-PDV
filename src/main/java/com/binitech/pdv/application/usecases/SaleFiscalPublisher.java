package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleEventPublisherPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleCompletedEvent;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.SaleReturn;
import com.binitech.pdv.domain.SaleReturnedEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SaleFiscalPublisher {

  private final SaleEventPublisherPort publisher;
  private final SaleRepositoryPort saleRepository;
  private final ProductRepositoryPort productRepository;
  private final ZoneId businessZone;

  public SaleFiscalPublisher(
      SaleEventPublisherPort publisher,
      SaleRepositoryPort saleRepository,
      ProductRepositoryPort productRepository,
      ZoneId businessZone) {
    this.publisher = publisher;
    this.saleRepository = saleRepository;
    this.productRepository = productRepository;
    this.businessZone = businessZone;
  }

  public void publish(Sale sale, Map<String, Product> knownProducts) {
    if (sale.getId() == null || !sale.isFiscalPending()) {
      return;
    }
    if (publisher.publishCompleted(toEvent(sale, knownProducts))) {
      saleRepository.markFiscalPublished(sale.getId());
      sale.setFiscalPending(false);
    }
  }

  public int publishPending(int limit) {
    int published = 0;
    for (Sale sale : saleRepository.findFiscalPending(limit)) {
      publish(sale, Map.of());
      if (!sale.isFiscalPending()) {
        published++;
      }
    }
    return published;
  }

  public void publishReturn(Sale sale, SaleReturn saleReturn) {
    if (sale.getId() == null || !saleReturn.isFiscalPending()) {
      return;
    }
    if (publisher.publishReturned(toReturnEvent(sale, saleReturn))) {
      saleRepository.markReturnFiscalPublished(sale.getId(), saleReturn.getId());
      saleReturn.setFiscalPending(false);
    }
  }

  public int publishPendingReturns(int limit) {
    int published = 0;
    for (Sale sale : saleRepository.findReturnFiscalPending(limit)) {
      for (SaleReturn saleReturn : sale.getReturns()) {
        if (!saleReturn.isFiscalPending()) {
          continue;
        }
        publishReturn(sale, saleReturn);
        if (!saleReturn.isFiscalPending()) {
          published++;
        }
      }
    }
    return published;
  }

  SaleReturnedEvent toReturnEvent(Sale sale, SaleReturn saleReturn) {
    List<SaleCompletedEvent.Item> items =
        saleReturn.getItems().stream().map(item -> toItem(item, Map.of())).toList();
    return new SaleReturnedEvent(
        sale.getTenantId(),
        sale.getId(),
        saleReturn.getId(),
        saleReturn.getTimestamp() == null
            ? null
            : saleReturn.getTimestamp().atZone(businessZone).toInstant(),
        isFullAfter(sale, saleReturn),
        saleReturn.getReason(),
        sale.getCustomerCpf(),
        sale.getCustomerName(),
        items);
  }

  private static boolean isFullAfter(Sale sale, SaleReturn saleReturn) {
    int position = sale.getReturns().indexOf(saleReturn);
    List<SaleReturn> upToThis =
        position < 0 ? sale.getReturns() : sale.getReturns().subList(0, position + 1);
    return sale.getItems().stream()
        .map(SaleItem::getProductId)
        .distinct()
        .allMatch(
            productId ->
                upToThis.stream()
                        .flatMap(r -> r.getItems().stream())
                        .filter(i -> productId.equals(i.getProductId()))
                        .mapToInt(SaleItem::getQuantity)
                        .sum()
                    >= sale.soldQuantity(productId));
  }

  SaleCompletedEvent toEvent(Sale sale, Map<String, Product> knownProducts) {
    List<SaleCompletedEvent.Item> items =
        sale.getItems().stream().map(item -> toItem(item, knownProducts)).toList();
    List<SaleCompletedEvent.Payment> payments =
        sale.getPayments() == null
            ? List.of()
            : sale.getPayments().stream().map(SaleFiscalPublisher::toPayment).toList();
    return new SaleCompletedEvent(
        sale.getTenantId(),
        sale.getId(),
        sale.getTimestamp() == null ? null : sale.getTimestamp().atZone(businessZone).toInstant(),
        sale.getCustomerCpf(),
        sale.getCustomerName(),
        items,
        payments);
  }

  private SaleCompletedEvent.Item toItem(SaleItem item, Map<String, Product> knownProducts) {
    Product product =
        Optional.ofNullable(knownProducts.get(item.getProductId()))
            .or(() -> productRepository.findById(item.getProductId()))
            .orElse(null);
    return new SaleCompletedEvent.Item(
        item.getProductId(),
        item.getProductDescription(),
        product == null ? null : product.getBarcode(),
        product == null ? null : product.getNcm(),
        product == null ? null : product.getCfop(),
        product == null ? null : product.getCsosn(),
        "UN",
        BigDecimal.valueOf(item.getQuantity()),
        money(item.getUnitPrice()),
        money(item.getSubtotal()));
  }

  private static SaleCompletedEvent.Payment toPayment(Payment payment) {
    return new SaleCompletedEvent.Payment(
        payment.getMethod() == null ? null : payment.getMethod().name(),
        money(payment.getAmount()));
  }

  private static BigDecimal money(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }
}
