package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.CashSessionsApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.CashMovementDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CashMovementTypeDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CashSessionDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CashSessionStatusDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CashSessionSummaryDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CloseCashSessionDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateCashMovementDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.OpenCashSessionDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PaymentMethodEnum;
import com.binitech.pdv.adapters.inbound.web.generated.model.PaymentTotalDTO;
import com.binitech.pdv.application.ports.inbound.CashSessionUseCasePort;
import com.binitech.pdv.application.ports.inbound.CashSessionUseCasePort.CashSessionView;
import com.binitech.pdv.application.ports.inbound.CashSessionUseCasePort.Operator;
import com.binitech.pdv.domain.CashMovement;
import com.binitech.pdv.domain.CashSession;
import com.binitech.pdv.domain.CashSessionSummary;
import com.binitech.pdv.utils.enums.CashMovementType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CashSessionController implements CashSessionsApi {

  private final CashSessionUseCasePort cashSessions;
  private final AuthenticatedUserProvider userProvider;

  public CashSessionController(
      CashSessionUseCasePort cashSessions, AuthenticatedUserProvider userProvider) {
    this.cashSessions = cashSessions;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<List<CashSessionDTO>> listCashSessions(
      LocalDate startDate, LocalDate endDate) {
    return ResponseEntity.ok(
        cashSessions.list(operator(), startDate, endDate).stream()
            .map(CashSessionController::toDto)
            .toList());
  }

  @Override
  public ResponseEntity<CashSessionDTO> openCashSession(OpenCashSessionDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(toDto(cashSessions.open(operator(), dto.getOpeningAmount())));
  }

  @Override
  public ResponseEntity<CashSessionDTO> getCurrentCashSession() {
    return cashSessions
        .current(operator())
        .map(view -> ResponseEntity.ok(toDto(view)))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @Override
  public ResponseEntity<CashSessionDTO> getCashSession(String id) {
    return ResponseEntity.ok(toDto(cashSessions.get(operator(), id)));
  }

  @Override
  public ResponseEntity<CashSessionDTO> addCashMovement(String id, CreateCashMovementDTO dto) {
    CashMovementType type =
        dto.getType() == null ? null : CashMovementType.valueOf(dto.getType().name());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            toDto(
                cashSessions.addMovement(operator(), id, type, dto.getAmount(), dto.getReason())));
  }

  @Override
  public ResponseEntity<CashSessionDTO> closeCashSession(String id, CloseCashSessionDTO dto) {
    return ResponseEntity.ok(
        toDto(cashSessions.close(operator(), id, dto.getCountedAmount(), dto.getNotes())));
  }

  private Operator operator() {
    return new Operator(
        userProvider.getUserId(),
        userProvider.getUsername(),
        userProvider.getTenantId(),
        userProvider.getUserRole());
  }

  private static CashSessionDTO toDto(CashSessionView view) {
    CashSession session = view.session();
    return new CashSessionDTO()
        .id(session.getId())
        .userId(session.getUserId())
        .operatorName(session.getOperatorName())
        .status(CashSessionStatusDTO.valueOf(session.getStatus().name()))
        .openedAt(time(session.getOpenedAt()))
        .openingAmount(session.getOpeningAmount())
        .movements(session.getMovements().stream().map(CashSessionController::toDto).toList())
        .closedAt(time(session.getClosedAt()))
        .closedByUserId(session.getClosedByUserId())
        .countedAmount(session.getCountedAmount())
        .difference(session.getDifference())
        .notes(session.getNotes())
        .summary(toDto(view.summary()))
        .pendingDeliveryOrders(view.pendingDeliveryOrders());
  }

  private static CashMovementDTO toDto(CashMovement movement) {
    return new CashMovementDTO()
        .id(movement.id())
        .type(CashMovementTypeDTO.valueOf(movement.type().name()))
        .amount(movement.amount())
        .reason(movement.reason())
        .userId(movement.userId())
        .timestamp(time(movement.timestamp()));
  }

  private static CashSessionSummaryDTO toDto(CashSessionSummary summary) {
    if (summary == null) {
      return null;
    }
    return new CashSessionSummaryDTO()
        .salesCount(summary.salesCount())
        .salesTotal(summary.salesTotal())
        .discountTotal(summary.discountTotal())
        .surchargeTotal(summary.surchargeTotal())
        .receivedByMethod(
            summary.receivedByMethod().entrySet().stream()
                .map(
                    entry ->
                        new PaymentTotalDTO()
                            .method(PaymentMethodEnum.valueOf(entry.getKey().name()))
                            .amount(entry.getValue()))
                .toList())
        .cashSales(summary.cashSales())
        .cashRefunds(summary.cashRefunds())
        .supplies(summary.supplies())
        .withdrawals(summary.withdrawals())
        .expectedCash(summary.expectedCash());
  }

  private static OffsetDateTime time(LocalDateTime value) {
    return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
  }
}
