package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.DeliveryOrderDocument;
import com.binitech.pdv.application.ports.outbound.DeliveryOrderRepositoryPort;
import com.binitech.pdv.domain.DeliveryAddress;
import com.binitech.pdv.domain.DeliveryOrder;
import com.binitech.pdv.domain.DeliveryOrderItem;
import com.binitech.pdv.utils.enums.DeliveryFulfillment;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class DeliveryOrderRepositoryAdapter implements DeliveryOrderRepositoryPort {

  private static final String TENANT_ID = "tenantId";
  private static final String STATUS = "status";
  private static final String UPDATED_AT = "updatedAt";

  private final MongoTemplate mongo;

  public DeliveryOrderRepositoryAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public DeliveryOrder save(DeliveryOrder order) {
    return toDomain(mongo.save(toDocument(order)));
  }

  @Override
  public Optional<DeliveryOrder> findByIdAndTenantId(String id, String tenantId) {
    return Optional.ofNullable(
            mongo.findOne(byIdAndTenant(id, tenantId), DeliveryOrderDocument.class))
        .map(DeliveryOrderRepositoryAdapter::toDomain);
  }

  @Override
  public List<DeliveryOrder> findRecent(String tenantId, DeliveryOrderStatus status, int limit) {
    Criteria criteria = Criteria.where(TENANT_ID).is(tenantId);
    if (status != null) {
      criteria = criteria.and(STATUS).is(status.name());
    } else {
      criteria = criteria.and(STATUS).ne(DeliveryOrderStatus.AWAITING_PAYMENT.name());
    }
    Query query =
        Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(limit);
    return mongo.find(query, DeliveryOrderDocument.class).stream()
        .map(DeliveryOrderRepositoryAdapter::toDomain)
        .toList();
  }

  @Override
  public boolean changeStatus(
      String id, String tenantId, DeliveryOrderStatus expected, DeliveryOrderStatus target) {
    Query query =
        byIdAndTenant(id, tenantId).addCriteria(Criteria.where(STATUS).is(expected.name()));
    Update update = new Update().set(STATUS, target.name()).set(UPDATED_AT, Instant.now());
    return mongo.updateFirst(query, update, DeliveryOrderDocument.class).getModifiedCount() == 1;
  }

  @Override
  public void attachSale(String id, String tenantId, String saleId) {
    mongo.updateFirst(
        byIdAndTenant(id, tenantId),
        new Update().set("saleId", saleId).set(UPDATED_AT, Instant.now()),
        DeliveryOrderDocument.class);
  }

  @Override
  public void attachPixCharge(String id, String tenantId, String chargeId) {
    mongo.updateFirst(
        byIdAndTenant(id, tenantId),
        new Update().set("pixChargeId", chargeId).set(UPDATED_AT, Instant.now()),
        DeliveryOrderDocument.class);
  }

  @Override
  public boolean confirmPayment(String id, String tenantId, Instant paidAt) {
    Query query =
        byIdAndTenant(id, tenantId)
            .addCriteria(Criteria.where(STATUS).is(DeliveryOrderStatus.AWAITING_PAYMENT.name()));
    Update update =
        new Update()
            .set(STATUS, DeliveryOrderStatus.RECEIVED.name())
            .set("paidAt", paidAt)
            .set(UPDATED_AT, paidAt);
    return mongo.updateFirst(query, update, DeliveryOrderDocument.class).getModifiedCount() == 1;
  }

  private static Query byIdAndTenant(String id, String tenantId) {
    return Query.query(Criteria.where("_id").is(id).and(TENANT_ID).is(tenantId));
  }

  private static DeliveryOrderDocument toDocument(DeliveryOrder order) {
    DeliveryOrderDocument document = new DeliveryOrderDocument();
    document.setId(order.getId());
    document.setTenantId(order.getTenantId());
    document.setNumber(order.getNumber());
    document.setStatus(order.getStatus().name());
    document.setFulfillment(order.getFulfillment().name());
    document.setCustomerName(order.getCustomerName());
    document.setCustomerPhone(order.getCustomerPhone());
    DeliveryAddress address = order.getAddress();
    if (address != null) {
      document.setAddress(
          new DeliveryOrderDocument.Address(
              address.street(),
              address.number(),
              address.complement(),
              address.district(),
              address.reference()));
    }
    document.setItems(
        order.getItems().stream()
            .map(
                item ->
                    new DeliveryOrderDocument.Item(
                        item.productId(),
                        item.description(),
                        item.unitPrice(),
                        item.quantity(),
                        item.notes()))
            .toList());
    document.setSubtotal(order.getSubtotal());
    document.setDeliveryFee(order.getDeliveryFee());
    document.setTotal(order.getTotal());
    document.setPaymentMethod(order.getPaymentMethod().name());
    document.setChangeFor(order.getChangeFor());
    document.setNotes(order.getNotes());
    document.setSaleId(order.getSaleId());
    document.setPixChargeId(order.getPixChargeId());
    document.setPaidAt(order.getPaidAt());
    document.setCreatedAt(order.getCreatedAt());
    document.setUpdatedAt(order.getUpdatedAt());
    return document;
  }

  private static DeliveryOrder toDomain(DeliveryOrderDocument document) {
    DeliveryOrder order = new DeliveryOrder();
    order.setId(document.getId());
    order.setTenantId(document.getTenantId());
    order.setNumber(document.getNumber());
    order.setStatus(DeliveryOrderStatus.valueOf(document.getStatus()));
    order.setFulfillment(DeliveryFulfillment.valueOf(document.getFulfillment()));
    order.setCustomerName(document.getCustomerName());
    order.setCustomerPhone(document.getCustomerPhone());
    DeliveryOrderDocument.Address address = document.getAddress();
    if (address != null) {
      order.setAddress(
          new DeliveryAddress(
              address.getStreet(),
              address.getNumber(),
              address.getComplement(),
              address.getDistrict(),
              address.getReference()));
    }
    order.setItems(
        document.getItems() == null
            ? List.of()
            : document.getItems().stream()
                .map(
                    item ->
                        new DeliveryOrderItem(
                            item.getProductId(),
                            item.getDescription(),
                            item.getUnitPrice(),
                            item.getQuantity(),
                            item.getNotes()))
                .toList());
    order.setSubtotal(document.getSubtotal());
    order.setDeliveryFee(document.getDeliveryFee());
    order.setTotal(document.getTotal());
    order.setPaymentMethod(DeliveryPaymentMethod.valueOf(document.getPaymentMethod()));
    order.setChangeFor(document.getChangeFor());
    order.setNotes(document.getNotes());
    order.setSaleId(document.getSaleId());
    order.setPixChargeId(document.getPixChargeId());
    order.setPaidAt(document.getPaidAt());
    order.setCreatedAt(document.getCreatedAt());
    order.setUpdatedAt(document.getUpdatedAt());
    return order;
  }
}
