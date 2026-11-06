package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.CashSessionDocument;
import com.binitech.pdv.application.ports.outbound.CashSessionRepositoryPort;
import com.binitech.pdv.domain.CashMovement;
import com.binitech.pdv.domain.CashSession;
import com.binitech.pdv.domain.CashSessionSummary;
import com.binitech.pdv.utils.enums.CashMovementType;
import com.binitech.pdv.utils.enums.CashSessionStatus;
import com.binitech.pdv.utils.enums.PaymentMethod;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class CashSessionRepositoryAdapter implements CashSessionRepositoryPort {

  private static final String TENANT_ID = "tenantId";
  private static final String STATUS = "status";

  private final MongoTemplate mongo;

  public CashSessionRepositoryAdapter(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public Optional<CashSession> insertOpen(CashSession session) {
    try {
      return Optional.of(toDomain(mongo.insert(toDocument(session))));
    } catch (DuplicateKeyException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<CashSession> findOpen(String tenantId, String userId) {
    Query query =
        Query.query(
            Criteria.where(TENANT_ID)
                .is(tenantId)
                .and("userId")
                .is(userId)
                .and(STATUS)
                .is(CashSessionStatus.OPEN.name()));
    return Optional.ofNullable(mongo.findOne(query, CashSessionDocument.class))
        .map(CashSessionRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<CashSession> findById(String id, String tenantId) {
    Query query = Query.query(Criteria.where("_id").is(id).and(TENANT_ID).is(tenantId));
    return Optional.ofNullable(mongo.findOne(query, CashSessionDocument.class))
        .map(CashSessionRepositoryAdapter::toDomain);
  }

  @Override
  public boolean addMovement(String id, String tenantId, CashMovement movement) {
    Query query =
        Query.query(
            Criteria.where("_id")
                .is(id)
                .and(TENANT_ID)
                .is(tenantId)
                .and(STATUS)
                .is(CashSessionStatus.OPEN.name()));
    Update update = new Update().push("movements", toDocument(movement));
    return mongo.updateFirst(query, update, CashSessionDocument.class).getModifiedCount() == 1;
  }

  @Override
  public boolean close(CashSession session) {
    Query query =
        Query.query(
            Criteria.where("_id")
                .is(session.getId())
                .and(TENANT_ID)
                .is(session.getTenantId())
                .and(STATUS)
                .is(CashSessionStatus.OPEN.name()));
    Update update =
        new Update()
            .set(STATUS, CashSessionStatus.CLOSED.name())
            .set("closedAt", session.getClosedAt())
            .set("closedByUserId", session.getClosedByUserId())
            .set("countedAmount", session.getCountedAmount())
            .set("difference", session.getDifference())
            .set("notes", session.getNotes())
            .set("closingSummary", toDocument(session.getClosingSummary()));
    return mongo.updateFirst(query, update, CashSessionDocument.class).getModifiedCount() == 1;
  }

  @Override
  public List<CashSession> findOpenedBetween(
      String tenantId, String userId, LocalDateTime start, LocalDateTime end, int limit) {
    Criteria criteria = Criteria.where(TENANT_ID).is(tenantId).and("openedAt").gte(start).lt(end);
    if (userId != null) {
      criteria = criteria.and("userId").is(userId);
    }
    Query query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "openedAt")).limit(limit);
    return mongo.find(query, CashSessionDocument.class).stream()
        .map(CashSessionRepositoryAdapter::toDomain)
        .toList();
  }

  private static CashSessionDocument toDocument(CashSession session) {
    return CashSessionDocument.builder()
        .id(session.getId())
        .tenantId(session.getTenantId())
        .userId(session.getUserId())
        .operatorName(session.getOperatorName())
        .openedAt(session.getOpenedAt())
        .openingAmount(session.getOpeningAmount())
        .status(session.getStatus().name())
        .movements(
            session.getMovements().stream().map(CashSessionRepositoryAdapter::toDocument).toList())
        .closedAt(session.getClosedAt())
        .closedByUserId(session.getClosedByUserId())
        .countedAmount(session.getCountedAmount())
        .difference(session.getDifference())
        .notes(session.getNotes())
        .closingSummary(toDocument(session.getClosingSummary()))
        .build();
  }

  private static CashSessionDocument.Movement toDocument(CashMovement movement) {
    return CashSessionDocument.Movement.builder()
        .id(movement.id())
        .type(movement.type().name())
        .amount(movement.amount())
        .reason(movement.reason())
        .userId(movement.userId())
        .timestamp(movement.timestamp())
        .build();
  }

  private static CashSessionDocument.Summary toDocument(CashSessionSummary summary) {
    if (summary == null) {
      return null;
    }
    Map<String, Double> received = new LinkedHashMap<>();
    summary.receivedByMethod().forEach((method, amount) -> received.put(method.name(), amount));
    return CashSessionDocument.Summary.builder()
        .salesCount(summary.salesCount())
        .salesTotal(summary.salesTotal())
        .discountTotal(summary.discountTotal())
        .surchargeTotal(summary.surchargeTotal())
        .receivedByMethod(received)
        .cashSales(summary.cashSales())
        .cashRefunds(summary.cashRefunds())
        .supplies(summary.supplies())
        .withdrawals(summary.withdrawals())
        .expectedCash(summary.expectedCash())
        .build();
  }

  private static CashSession toDomain(CashSessionDocument document) {
    CashSession session = new CashSession();
    session.setId(document.getId());
    session.setTenantId(document.getTenantId());
    session.setUserId(document.getUserId());
    session.setOperatorName(document.getOperatorName());
    session.setOpenedAt(document.getOpenedAt());
    session.setOpeningAmount(document.getOpeningAmount());
    session.setStatus(CashSessionStatus.valueOf(document.getStatus()));
    session.setMovements(
        document.getMovements() == null
            ? List.of()
            : document.getMovements().stream()
                .map(CashSessionRepositoryAdapter::toDomain)
                .toList());
    session.setClosedAt(document.getClosedAt());
    session.setClosedByUserId(document.getClosedByUserId());
    session.setCountedAmount(document.getCountedAmount());
    session.setDifference(document.getDifference());
    session.setNotes(document.getNotes());
    session.setClosingSummary(toDomain(document.getClosingSummary()));
    return session;
  }

  private static CashMovement toDomain(CashSessionDocument.Movement movement) {
    return new CashMovement(
        movement.getId(),
        CashMovementType.valueOf(movement.getType()),
        movement.getAmount(),
        movement.getReason(),
        movement.getUserId(),
        movement.getTimestamp());
  }

  private static CashSessionSummary toDomain(CashSessionDocument.Summary summary) {
    if (summary == null) {
      return null;
    }
    Map<PaymentMethod, Double> received = new EnumMap<>(PaymentMethod.class);
    if (summary.getReceivedByMethod() != null) {
      summary
          .getReceivedByMethod()
          .forEach((method, amount) -> received.put(PaymentMethod.valueOf(method), amount));
    }
    return new CashSessionSummary(
        summary.getSalesCount(),
        summary.getSalesTotal(),
        summary.getDiscountTotal(),
        summary.getSurchargeTotal(),
        received,
        summary.getCashSales(),
        summary.getCashRefunds(),
        summary.getSupplies(),
        summary.getWithdrawals(),
        summary.getExpectedCash());
  }
}
