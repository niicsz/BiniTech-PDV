package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.DeliveryStoreDocument;
import com.binitech.pdv.application.ports.outbound.DeliveryStoreRepositoryPort;
import com.binitech.pdv.domain.DeliveryStore;
import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class DeliveryStoreRepositoryAdapter implements DeliveryStoreRepositoryPort {

  private final MongoTemplate mongo;

  public DeliveryStoreRepositoryAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public Optional<DeliveryStore> findByTenantId(String tenantId) {
    return Optional.ofNullable(mongo.findById(tenantId, DeliveryStoreDocument.class))
        .map(DeliveryStoreRepositoryAdapter::toDomain);
  }

  @Override
  public DeliveryStore save(DeliveryStore store) {
    Update update =
        new Update()
            .set("enabled", store.isEnabled())
            .set("open", store.isOpen())
            .set("whatsapp", store.getWhatsapp())
            .set("message", store.getMessage())
            .set("deliveryFee", store.getDeliveryFee())
            .set("minimumOrder", store.getMinimumOrder())
            .set("estimatedMinutes", store.getEstimatedMinutes())
            .set("deliveryAvailable", store.isDeliveryAvailable())
            .set("pickupAvailable", store.isPickupAvailable())
            .set("pickupAddress", store.getPickupAddress())
            .set(
                "acceptedPayments",
                store.getAcceptedPayments().stream().map(Enum::name).sorted().toList())
            .set("pixKey", store.getPixKey())
            .set("updatedAt", store.getUpdatedAt());
    DeliveryStoreDocument saved =
        mongo.findAndModify(
            byTenant(store.getTenantId()),
            update,
            FindAndModifyOptions.options().upsert(true).returnNew(true),
            DeliveryStoreDocument.class);
    return toDomain(saved);
  }

  @Override
  public long nextOrderNumber(String tenantId) {
    DeliveryStoreDocument updated =
        mongo.findAndModify(
            byTenant(tenantId),
            new Update().inc("lastOrderNumber", 1),
            FindAndModifyOptions.options().upsert(true).returnNew(true),
            DeliveryStoreDocument.class);
    return updated.getLastOrderNumber();
  }

  private static Query byTenant(String tenantId) {
    return Query.query(Criteria.where("_id").is(tenantId));
  }

  private static DeliveryStore toDomain(DeliveryStoreDocument document) {
    DeliveryStore store = new DeliveryStore();
    store.setTenantId(document.getTenantId());
    store.setEnabled(document.isEnabled());
    store.setOpen(document.isOpen());
    store.setWhatsapp(document.getWhatsapp());
    store.setMessage(document.getMessage());
    store.setDeliveryFee(document.getDeliveryFee());
    store.setMinimumOrder(document.getMinimumOrder());
    store.setEstimatedMinutes(document.getEstimatedMinutes());
    store.setDeliveryAvailable(document.isDeliveryAvailable());
    store.setPickupAvailable(document.isPickupAvailable());
    store.setPickupAddress(document.getPickupAddress());
    store.setAcceptedPayments(payments(document.getAcceptedPayments()));
    store.setPixKey(document.getPixKey());
    store.setUpdatedAt(document.getUpdatedAt());
    return store;
  }

  private static EnumSet<DeliveryPaymentMethod> payments(List<String> names) {
    EnumSet<DeliveryPaymentMethod> methods = EnumSet.noneOf(DeliveryPaymentMethod.class);
    if (names != null) {
      for (String name : names) {
        for (DeliveryPaymentMethod method : DeliveryPaymentMethod.values()) {
          if (method.name().equals(name)) {
            methods.add(method);
          }
        }
      }
    }
    return methods;
  }
}
