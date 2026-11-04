package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.DeliveryOrder;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import java.util.List;
import java.util.Optional;

public interface DeliveryOrderRepositoryPort {

  DeliveryOrder save(DeliveryOrder order);

  Optional<DeliveryOrder> findByIdAndTenantId(String id, String tenantId);

  List<DeliveryOrder> findRecent(String tenantId, DeliveryOrderStatus status, int limit);

  boolean changeStatus(
      String id, String tenantId, DeliveryOrderStatus expected, DeliveryOrderStatus target);

  void attachSale(String id, String tenantId, String saleId);
}
