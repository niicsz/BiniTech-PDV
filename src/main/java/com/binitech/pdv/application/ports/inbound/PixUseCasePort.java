package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.PixSettings;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.utils.enums.Role;
import java.util.Optional;

public interface PixUseCasePort {

  record Operator(String userId, String tenantId, Role role) {}

  record SettingsUpdate(Boolean enabled, String accessToken, String webhookSecret) {}

  record Notification(String chargeId, String dataId, String requestId, String signature) {}

  record OrderCharge(String tenantId, String orderId, double amount, String description) {}

  PixSettings getSettings(Operator operator);

  PixSettings updateSettings(Operator operator, SettingsUpdate update);

  PixCharge createCharge(Operator operator, Sale saleRequest);

  PixCharge getCharge(Operator operator, String chargeId);

  PixCharge cancelCharge(Operator operator, String chargeId);

  void handleNotification(Notification notification);

  boolean isActive(String tenantId);

  PixCharge createOrderCharge(OrderCharge request);

  Optional<PixCharge> checkOrderCharge(String tenantId, String chargeId);
}
