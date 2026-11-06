package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.CashSessionUseCasePort;
import com.binitech.pdv.application.ports.outbound.CashSessionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.DeliveryOrderRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.CashMovement;
import com.binitech.pdv.domain.CashSession;
import com.binitech.pdv.domain.CashSessionSummary;
import com.binitech.pdv.domain.Money;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.CashMovementType;
import com.binitech.pdv.utils.enums.CashSessionStatus;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.Role;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CashSessionUseCaseImpl implements CashSessionUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(CashSessionUseCaseImpl.class);
  private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
  private static final int MAX_REASON = 120;
  private static final int MAX_NOTES = 500;
  private static final int MAX_LIST = 200;
  private static final int MAX_LIST_DAYS = 31;
  private static final int PENDING_ORDERS_LIMIT = 100;
  private static final List<DeliveryOrderStatus> PENDING_ORDERS =
      List.of(
          DeliveryOrderStatus.RECEIVED,
          DeliveryOrderStatus.ACCEPTED,
          DeliveryOrderStatus.DISPATCHED);

  private final CashSessionRepositoryPort sessions;
  private final SaleRepositoryPort sales;
  private final DeliveryOrderRepositoryPort deliveryOrders;
  private final Clock clock;
  private final ZoneId businessZone;

  public CashSessionUseCaseImpl(
      CashSessionRepositoryPort sessions,
      SaleRepositoryPort sales,
      DeliveryOrderRepositoryPort deliveryOrders,
      Clock clock,
      ZoneId businessZone) {
    this.sessions = sessions;
    this.sales = sales;
    this.deliveryOrders = deliveryOrders;
    this.clock = clock;
    this.businessZone = businessZone;
  }

  @Override
  public Optional<CashSessionView> current(Operator operator) {
    return sessions.findOpen(operator.tenantId(), operator.userId()).map(this::view);
  }

  @Override
  public CashSessionView open(Operator operator, Double openingAmount) {
    CashSession session = new CashSession();
    session.setId(UUID.randomUUID().toString());
    session.setTenantId(operator.tenantId());
    session.setUserId(operator.userId());
    session.setOperatorName(operator.username());
    session.setOpenedAt(now());
    session.setOpeningAmount(CashSession.requireAmount(openingAmount, true));
    session.setStatus(CashSessionStatus.OPEN);
    CashSession saved =
        sessions
            .insertOpen(session)
            .orElseThrow(
                () ->
                    new BusinessException(
                        "Você já tem um caixa aberto. Feche o caixa atual antes de abrir outro."));
    if (log.isInfoEnabled()) {
      log.info(
          "Caixa aberto: sessão={} tenantId={} troco={}",
          LogSanitizer.maskId(saved.getId()),
          LogSanitizer.maskId(operator.tenantId()),
          saved.getOpeningAmount());
    }
    return view(saved);
  }

  @Override
  public CashSessionView addMovement(
      Operator operator, String sessionId, CashMovementType type, Double amount, String reason) {
    if (type == null) {
      throw new BusinessException("Informe se é sangria ou suprimento.");
    }
    CashSession session = findAccessible(operator, sessionId);
    requireOpen(session);
    double value = CashSession.requireAmount(amount, false);
    if (type == CashMovementType.WITHDRAWAL) {
      double available = summary(session).expectedCash();
      if (Money.compare(value, available) > 0) {
        throw new BusinessException(
            String.format(
                BRAZIL,
                "Não há dinheiro suficiente no caixa para essa sangria. Disponível: R$ %.2f",
                available));
      }
    }
    CashMovement movement =
        new CashMovement(
            UUID.randomUUID().toString(),
            type,
            value,
            trimmed(reason, MAX_REASON),
            operator.userId(),
            now());
    if (!sessions.addMovement(session.getId(), operator.tenantId(), movement)) {
      throw new BusinessException("Este caixa já foi fechado.");
    }
    session.getMovements().add(movement);
    if (log.isInfoEnabled()) {
      log.info(
          "Movimento de caixa: sessão={} tipo={} valor={}",
          LogSanitizer.maskId(session.getId()),
          type,
          value);
    }
    return view(session);
  }

  @Override
  public CashSessionView close(
      Operator operator, String sessionId, Double countedAmount, String notes) {
    CashSession session = findAccessible(operator, sessionId);
    requireOpen(session);
    CashSessionSummary summary = summary(session);
    session.close(operator.userId(), now(), countedAmount, trimmed(notes, MAX_NOTES), summary);
    if (!sessions.close(session)) {
      throw new BusinessException("Este caixa já foi fechado.");
    }
    if (log.isInfoEnabled()) {
      log.info(
          "Caixa fechado: sessão={} esperado={} contado={} diferença={}",
          LogSanitizer.maskId(session.getId()),
          summary.expectedCash(),
          session.getCountedAmount(),
          session.getDifference());
    }
    return new CashSessionView(session, summary, 0);
  }

  @Override
  public CashSessionView get(Operator operator, String sessionId) {
    return view(findAccessible(operator, sessionId));
  }

  @Override
  public List<CashSessionView> list(Operator operator, LocalDate startDate, LocalDate endDate) {
    LocalDate today = LocalDate.now(clock.withZone(businessZone));
    LocalDate start = startDate == null ? today : startDate;
    LocalDate end = endDate == null ? start : endDate;
    if (end.isBefore(start)) {
      throw new BusinessException("A data final não pode ser anterior à data inicial.");
    }
    if (start.plusDays(MAX_LIST_DAYS).isBefore(end)) {
      throw new BusinessException("Escolha um período de até 31 dias.");
    }
    String userFilter = isPrivileged(operator.role()) ? null : operator.userId();
    return sessions
        .findOpenedBetween(
            operator.tenantId(), userFilter, toLocal(start), toLocal(end.plusDays(1)), MAX_LIST)
        .stream()
        .map(this::listView)
        .toList();
  }

  private CashSessionView listView(CashSession session) {
    if (!session.isOpen() && session.getClosingSummary() != null) {
      return new CashSessionView(session, session.getClosingSummary(), 0);
    }
    return new CashSessionView(session, summary(session), 0);
  }

  private CashSessionView view(CashSession session) {
    if (!session.isOpen() && session.getClosingSummary() != null) {
      return new CashSessionView(session, session.getClosingSummary(), 0);
    }
    return new CashSessionView(session, summary(session), pendingDeliveryOrders(session));
  }

  private CashSessionSummary summary(CashSession session) {
    return CashSessionSummary.of(
        session, sales.findForCashSession(session.getTenantId(), session.getId()));
  }

  private int pendingDeliveryOrders(CashSession session) {
    if (deliveryOrders == null) {
      return 0;
    }
    int pending = 0;
    for (DeliveryOrderStatus status : PENDING_ORDERS) {
      pending +=
          deliveryOrders.findRecent(session.getTenantId(), status, PENDING_ORDERS_LIMIT).size();
    }
    return pending;
  }

  private CashSession findAccessible(Operator operator, String sessionId) {
    CashSession session =
        sessions
            .findById(sessionId, operator.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Caixa", "id", sessionId));
    if (!isPrivileged(operator.role()) && !Objects.equals(session.getUserId(), operator.userId())) {
      throw new ResourceNotFoundException("Caixa", "id", sessionId);
    }
    return session;
  }

  private static void requireOpen(CashSession session) {
    if (!session.isOpen()) {
      throw new BusinessException("Este caixa já foi fechado.");
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(clock.withZone(ZoneId.systemDefault()));
  }

  private LocalDateTime toLocal(LocalDate date) {
    return LocalDateTime.ofInstant(
        date.atStartOfDay(businessZone).toInstant(), ZoneId.systemDefault());
  }

  private static String trimmed(String value, int max) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String text = value.trim();
    return text.length() > max ? text.substring(0, max) : text;
  }

  private static boolean isPrivileged(Role role) {
    return role == Role.ADMIN || role == Role.TENANT_ADMIN || role == Role.SUPER_ADMIN;
  }
}
