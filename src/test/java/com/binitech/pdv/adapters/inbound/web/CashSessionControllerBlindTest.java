package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.inbound.web.generated.model.CashSessionDTO;
import com.binitech.pdv.application.ports.inbound.CashSessionUseCasePort;
import com.binitech.pdv.application.ports.inbound.CashSessionUseCasePort.CashSessionView;
import com.binitech.pdv.domain.CashSession;
import com.binitech.pdv.domain.CashSessionSummary;
import com.binitech.pdv.utils.enums.CashSessionStatus;
import com.binitech.pdv.utils.enums.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CashSessionControllerBlindTest {

  private final CashSessionUseCasePort useCase = mock(CashSessionUseCasePort.class);
  private final AuthenticatedUserProvider user = mock(AuthenticatedUserProvider.class);
  private final CashSessionController controller = new CashSessionController(useCase, user);

  @Test
  void operatorDoesNotReceiveExpectedCashWhileTheSessionIsOpen() {
    when(user.getUserRole()).thenReturn(Role.OPERATOR);
    when(useCase.current(any())).thenReturn(Optional.of(view(CashSessionStatus.OPEN)));

    CashSessionDTO dto = controller.getCurrentCashSession().getBody();

    assertNull(dto.getOpeningAmount());
    assertTrue(dto.getMovements().isEmpty());
    assertNull(dto.getSummary().getExpectedCash());
    assertNull(dto.getSummary().getSupplies());
    assertEquals(50.0, dto.getSummary().getSalesTotal());
  }

  @Test
  void managerReceivesTheFullConference() {
    when(user.getUserRole()).thenReturn(Role.TENANT_ADMIN);
    when(useCase.current(any())).thenReturn(Optional.of(view(CashSessionStatus.OPEN)));

    CashSessionDTO dto = controller.getCurrentCashSession().getBody();

    assertEquals(100.0, dto.getOpeningAmount());
    assertEquals(130.0, dto.getSummary().getExpectedCash());
  }

  private static CashSessionView view(CashSessionStatus status) {
    CashSession session = new CashSession();
    session.setId("c1");
    session.setStatus(status);
    session.setOpeningAmount(100.0);
    session.setOpenedAt(LocalDateTime.now());
    session.setMovements(List.of());
    CashSessionSummary summary =
        new CashSessionSummary(2, 50.0, 0, 0, Map.of(), 30.0, 0, 0, 0, 130.0);
    return new CashSessionView(session, summary, 0);
  }
}
