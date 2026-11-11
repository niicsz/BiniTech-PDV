package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.DeliveryApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CompleteDeliveryOrderDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryOrderDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryOrderStatusDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliverySettingsDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.UpdateDeliveryOrderStatusDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.UpdateDeliverySettingsDTO;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort;
import com.binitech.pdv.utils.enums.PaymentMethod;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeliveryController implements DeliveryApi {

  private final DeliveryUseCasePort delivery;
  private final DeliveryWebMapper mapper;
  private final AuthenticatedUserProvider userProvider;

  public DeliveryController(
      DeliveryUseCasePort delivery,
      DeliveryWebMapper mapper,
      AuthenticatedUserProvider userProvider) {
    this.delivery = delivery;
    this.mapper = mapper;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<DeliverySettingsDTO> getDeliverySettings() {
    return ResponseEntity.ok(mapper.toDto(delivery.getSettings(userProvider.getTenantId())));
  }

  @Override
  public ResponseEntity<DeliverySettingsDTO> updateDeliverySettings(UpdateDeliverySettingsDTO dto) {
    return ResponseEntity.ok(
        mapper.toDto(
            delivery.updateSettings(
                userProvider.getTenantId(), mapper.toDomain(dto), userProvider.getUserRole())));
  }

  @Override
  public ResponseEntity<List<DeliveryOrderDTO>> listDeliveryOrders(DeliveryOrderStatusDTO status) {
    return ResponseEntity.ok(
        mapper.toOrderDtos(
            delivery.listOrders(userProvider.getTenantId(), mapper.toStatus(status))));
  }

  @Override
  public ResponseEntity<DeliveryOrderDTO> updateDeliveryOrderStatus(
      String id, UpdateDeliveryOrderStatusDTO dto) {
    return ResponseEntity.ok(
        mapper.toDto(
            delivery.updateStatus(
                id, userProvider.getTenantId(), mapper.toStatus(dto.getStatus()))));
  }

  @Override
  public ResponseEntity<DeliveryOrderDTO> completeDeliveryOrder(
      String id, CompleteDeliveryOrderDTO dto) {
    PaymentMethod method =
        dto == null || dto.getPaymentMethod() == null
            ? null
            : PaymentMethod.valueOf(dto.getPaymentMethod().getValue());
    return ResponseEntity.ok(
        mapper.toDto(
            delivery.complete(id, method, userProvider.getUserId(), userProvider.getTenantId())));
  }
}
