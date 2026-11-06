package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.CashSession;
import com.binitech.pdv.domain.CashSessionSummary;
import com.binitech.pdv.utils.enums.CashMovementType;
import com.binitech.pdv.utils.enums.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CashSessionUseCasePort {

  record Operator(String userId, String username, String tenantId, Role role) {}

  record CashSessionView(
      CashSession session, CashSessionSummary summary, int pendingDeliveryOrders) {}

  Optional<CashSessionView> current(Operator operator);

  CashSessionView open(Operator operator, Double openingAmount);

  CashSessionView addMovement(
      Operator operator, String sessionId, CashMovementType type, Double amount, String reason);

  CashSessionView close(Operator operator, String sessionId, Double countedAmount, String notes);

  CashSessionView get(Operator operator, String sessionId);

  List<CashSessionView> list(Operator operator, LocalDate startDate, LocalDate endDate);
}
