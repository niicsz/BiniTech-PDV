package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.DeliveryStore;
import java.util.Optional;

public interface DeliveryStoreRepositoryPort {

  Optional<DeliveryStore> findByTenantId(String tenantId);

  DeliveryStore save(DeliveryStore store);

  long nextOrderNumber(String tenantId);
}
