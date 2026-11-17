package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.AccessLogDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.CashSessionDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.CheckoutSessionDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.DeliveryOrderDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.DeliveryStoreDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.PixChargeDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.PixSettingsDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.ProductDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.ProductImportReceiptDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.SaleDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.UserDocument;
import com.binitech.pdv.application.ports.outbound.TenantDataPurgePort;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class MongoTenantDataPurgeAdapter implements TenantDataPurgePort {

  private static final List<Class<?>> TENANT_COLLECTIONS =
      List.of(
          CheckoutSessionDocument.class,
          SaleDocument.class,
          ProductDocument.class,
          ProductImportReceiptDocument.class,
          UserDocument.class,
          DeliveryOrderDocument.class,
          DeliveryStoreDocument.class,
          CashSessionDocument.class,
          AccessLogDocument.class,
          PixChargeDocument.class,
          PixSettingsDocument.class);

  private final MongoTemplate mongo;

  public MongoTenantDataPurgeAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public void purgeTenantData(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("tenantId obrigatório");
    }
    Query byTenant = Query.query(Criteria.where("tenantId").is(tenantId));
    TENANT_COLLECTIONS.forEach(type -> mongo.remove(byTenant, type));
  }
}
