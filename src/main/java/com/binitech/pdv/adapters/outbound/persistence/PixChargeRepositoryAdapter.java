package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.PaymentDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.PixChargeDocument;
import com.binitech.pdv.application.ports.outbound.PixChargeRepositoryPort;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.PixChargeStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class PixChargeRepositoryAdapter implements PixChargeRepositoryPort {

  private static final String PROVIDER = "MERCADO_PAGO";

  private final MongoTemplate mongo;

  public PixChargeRepositoryAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public PixCharge insert(PixCharge charge) {
    return toDomain(mongo.insert(toDocument(charge)));
  }

  @Override
  public PixCharge save(PixCharge charge) {
    return toDomain(mongo.save(toDocument(charge)));
  }

  @Override
  public Optional<PixCharge> findById(String id) {
    return Optional.ofNullable(mongo.findById(id, PixChargeDocument.class)).map(this::toDomain);
  }

  @Override
  public Optional<PixCharge> findByIdAndTenantId(String id, String tenantId) {
    Query query = Query.query(Criteria.where("_id").is(id).and("tenantId").is(tenantId));
    return Optional.ofNullable(mongo.findOne(query, PixChargeDocument.class)).map(this::toDomain);
  }

  @Override
  public boolean transition(String id, PixChargeStatus expected, PixChargeStatus target) {
    Query query = Query.query(Criteria.where("_id").is(id).and("status").is(expected.name()));
    return mongo
            .updateFirst(query, Update.update("status", target.name()), PixChargeDocument.class)
            .getModifiedCount()
        == 1;
  }

  @Override
  public void markChecked(String id, LocalDateTime checkedAt) {
    mongo.updateFirst(
        Query.query(Criteria.where("_id").is(id)),
        Update.update("lastCheckedAt", checkedAt),
        PixChargeDocument.class);
  }

  private PixChargeDocument toDocument(PixCharge charge) {
    return PixChargeDocument.builder()
        .id(charge.getId())
        .tenantId(charge.getTenantId())
        .userId(charge.getUserId())
        .provider(PROVIDER)
        .providerPaymentId(charge.getProviderPaymentId())
        .amount(charge.getAmount())
        .status(charge.getStatus().name())
        .qrCode(charge.getQrCode())
        .qrCodeBase64(charge.getQrCodeBase64())
        .createdAt(charge.getCreatedAt())
        .expiresAt(charge.getExpiresAt())
        .lastCheckedAt(charge.getLastCheckedAt())
        .paidAt(charge.getPaidAt())
        .saleId(charge.getSaleId())
        .deliveryOrderId(charge.getDeliveryOrderId())
        .failureReason(charge.getFailureReason())
        .saleRequest(toDocument(charge.getSaleRequest()))
        .build();
  }

  private static PixChargeDocument.SaleRequest toDocument(Sale sale) {
    if (sale == null) {
      return null;
    }
    List<PixChargeDocument.Item> items = new ArrayList<>();
    for (SaleItem item : sale.getItems()) {
      items.add(new PixChargeDocument.Item(item.getProductId(), item.getQuantity()));
    }
    List<PaymentDocument> payments = new ArrayList<>();
    for (Payment payment : sale.getPayments()) {
      payments.add(
          new PaymentDocument(
              payment.getMethod() == null ? null : payment.getMethod().name(),
              payment.getAmount()));
    }
    return PixChargeDocument.SaleRequest.builder()
        .items(items)
        .payments(payments)
        .customerName(sale.getCustomerName())
        .customerPhone(sale.getCustomerPhone())
        .customerCpf(sale.getCustomerCpf())
        .discount(sale.getDiscount())
        .surcharge(sale.getSurcharge())
        .skipStockValidation(sale.isSkipStockValidation())
        .build();
  }

  private PixCharge toDomain(PixChargeDocument document) {
    PixCharge charge = new PixCharge();
    charge.setId(document.getId());
    charge.setTenantId(document.getTenantId());
    charge.setUserId(document.getUserId());
    charge.setProviderPaymentId(document.getProviderPaymentId());
    charge.setAmount(document.getAmount());
    charge.setStatus(PixChargeStatus.valueOf(document.getStatus()));
    charge.setQrCode(document.getQrCode());
    charge.setQrCodeBase64(document.getQrCodeBase64());
    charge.setCreatedAt(document.getCreatedAt());
    charge.setExpiresAt(document.getExpiresAt());
    charge.setLastCheckedAt(document.getLastCheckedAt());
    charge.setPaidAt(document.getPaidAt());
    charge.setSaleId(document.getSaleId());
    charge.setDeliveryOrderId(document.getDeliveryOrderId());
    charge.setFailureReason(document.getFailureReason());
    charge.setSaleRequest(toDomain(document.getSaleRequest()));
    return charge;
  }

  private static Sale toDomain(PixChargeDocument.SaleRequest request) {
    if (request == null) {
      return null;
    }
    Sale sale = new Sale();
    List<SaleItem> items = new ArrayList<>();
    if (request.getItems() != null) {
      for (PixChargeDocument.Item item : request.getItems()) {
        SaleItem saleItem = new SaleItem();
        saleItem.setProductId(item.getProductId());
        saleItem.setQuantity(item.getQuantity());
        items.add(saleItem);
      }
    }
    sale.setItems(items);
    List<Payment> payments = new ArrayList<>();
    if (request.getPayments() != null) {
      for (PaymentDocument payment : request.getPayments()) {
        payments.add(
            new Payment(
                payment.getMethod() == null ? null : PaymentMethod.valueOf(payment.getMethod()),
                payment.getAmount()));
      }
    }
    sale.setPayments(payments);
    sale.setCustomerName(request.getCustomerName());
    sale.setCustomerPhone(request.getCustomerPhone());
    sale.setCustomerCpf(request.getCustomerCpf());
    sale.setDiscount(request.getDiscount());
    sale.setSurcharge(request.getSurcharge());
    sale.setSkipStockValidation(request.isSkipStockValidation());
    return sale;
  }
}
