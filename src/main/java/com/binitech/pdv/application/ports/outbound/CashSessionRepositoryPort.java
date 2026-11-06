package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.CashMovement;
import com.binitech.pdv.domain.CashSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashSessionRepositoryPort {

  Optional<CashSession> insertOpen(CashSession session);

  Optional<CashSession> findOpen(String tenantId, String userId);

  Optional<CashSession> findById(String id, String tenantId);

  boolean addMovement(String id, String tenantId, CashMovement movement);

  boolean close(CashSession session);

  List<CashSession> findOpenedBetween(
      String tenantId, String userId, LocalDateTime start, LocalDateTime end, int limit);
}
