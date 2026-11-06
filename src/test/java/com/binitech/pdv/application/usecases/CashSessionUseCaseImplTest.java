package com.binitech.pdv.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.inbound.CashSessionUseCasePort.CashSessionView;
import com.binitech.pdv.application.ports.inbound.CashSessionUseCasePort.Operator;
import com.binitech.pdv.application.ports.outbound.CashSessionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.DeliveryOrderRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.CashMovement;
import com.binitech.pdv.domain.CashSession;
import com.binitech.pdv.domain.DeliveryOrder;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleReturn;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.CashMovementType;
import com.binitech.pdv.utils.enums.CashSessionStatus;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import com.binitech.pdv.utils.enums.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashSessionUseCaseImplTest {

  private static final Operator OPERATOR = new Operator("u1", "maria", "t1", Role.OPERATOR);
  private static final Operator ADMIN = new Operator("u9", "dono", "t1", Role.TENANT_ADMIN);

  @Mock private CashSessionRepositoryPort sessions;
  @Mock private SaleRepositoryPort sales;
  @Mock private DeliveryOrderRepositoryPort deliveryOrders;

  private CashSessionUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new CashSessionUseCaseImpl(
            sessions,
            sales,
            deliveryOrders,
            Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC),
            ZoneId.of("America/Sao_Paulo"));
  }

  private static CashSession openSession(String userId, double opening) {
    CashSession session = new CashSession();
    session.setId("s1");
    session.setTenantId("t1");
    session.setUserId(userId);
    session.setOpeningAmount(opening);
    session.setOpenedAt(LocalDateTime.now());
    session.setStatus(CashSessionStatus.OPEN);
    return session;
  }

  private static Sale cashSale(double total, double paid) {
    Sale sale = new Sale();
    sale.setCashSessionId("s1");
    sale.setTotalAmount(total);
    sale.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.CASH, paid))));
    sale.setChange(paid - total);
    return sale;
  }

  @Test
  @DisplayName("Abre o caixa com o troco inicial do operador")
  void opensSession() {
    when(sessions.insertOpen(any())).thenAnswer(inv -> Optional.of(inv.getArgument(0)));

    CashSessionView view = useCase.open(OPERATOR, 100.0);

    assertThat(view.session().getOpeningAmount()).isEqualTo(100.0);
    assertThat(view.session().getUserId()).isEqualTo("u1");
    assertThat(view.session().getOperatorName()).isEqualTo("maria");
    assertThat(view.summary().expectedCash()).isEqualTo(100.0);
  }

  @Test
  @DisplayName("Não deixa abrir um segundo caixa para o mesmo operador")
  void rejectsSecondOpenSession() {
    when(sessions.insertOpen(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.open(OPERATOR, 50.0))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("já tem um caixa aberto");
  }

  @Test
  @DisplayName("Troco inicial negativo é recusado")
  void rejectsNegativeOpening() {
    assertThatThrownBy(() -> useCase.open(OPERATOR, -1.0)).isInstanceOf(BusinessException.class);
    verify(sessions, never()).insertOpen(any());
  }

  @Test
  @DisplayName(
      "Dinheiro esperado soma vendas em dinheiro menos troco, suprimento e tira sangria e devolução")
  void summaryComputesExpectedCash() {
    CashSession session = openSession("u1", 100);
    session
        .getMovements()
        .add(new CashMovement("m1", CashMovementType.SUPPLY, 50, null, "u1", null));
    session
        .getMovements()
        .add(new CashMovement("m2", CashMovementType.WITHDRAWAL, 30, "banco", "u1", null));
    Sale pix = new Sale();
    pix.setCashSessionId("s1");
    pix.setTotalAmount(20);
    pix.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.PIX, 20))));
    Sale returned = new Sale();
    returned.setCashSessionId("other");
    SaleReturn refund = new SaleReturn();
    refund.setCashSessionId("s1");
    refund.setRefundMethod(RefundMethod.CASH);
    refund.setRefundAmount(5);
    returned.setReturns(List.of(refund));
    when(sessions.findOpen("t1", "u1")).thenReturn(Optional.of(session));
    when(sales.findForCashSession("t1", "s1"))
        .thenReturn(List.of(cashSale(18.5, 20), pix, returned));

    CashSessionView view = useCase.current(OPERATOR).orElseThrow();

    assertThat(view.summary().salesCount()).isEqualTo(2);
    assertThat(view.summary().salesTotal()).isEqualTo(38.5);
    assertThat(view.summary().cashSales()).isEqualTo(18.5);
    assertThat(view.summary().receivedByMethod()).containsEntry(PaymentMethod.PIX, 20.0);
    assertThat(view.summary().cashRefunds()).isEqualTo(5.0);
    assertThat(view.summary().expectedCash()).isEqualTo(133.5);
  }

  @Test
  @DisplayName("Mostra quantos pedidos de delivery ainda estão em andamento")
  void countsPendingDeliveryOrders() {
    when(sessions.findOpen("t1", "u1")).thenReturn(Optional.of(openSession("u1", 0)));
    when(sales.findForCashSession("t1", "s1")).thenReturn(List.of());
    when(deliveryOrders.findRecent(eq("t1"), any(DeliveryOrderStatus.class), anyInt()))
        .thenReturn(List.of());
    when(deliveryOrders.findRecent(eq("t1"), eq(DeliveryOrderStatus.ACCEPTED), anyInt()))
        .thenReturn(List.of(new DeliveryOrder(), new DeliveryOrder()));

    assertThat(useCase.current(OPERATOR).orElseThrow().pendingDeliveryOrders()).isEqualTo(2);
  }

  @Test
  @DisplayName("Sangria maior que o dinheiro do caixa é recusada")
  void rejectsWithdrawalAboveCash() {
    when(sessions.findById("s1", "t1")).thenReturn(Optional.of(openSession("u1", 40)));
    when(sales.findForCashSession("t1", "s1")).thenReturn(List.of());

    assertThatThrownBy(
            () -> useCase.addMovement(OPERATOR, "s1", CashMovementType.WITHDRAWAL, 50.0, "banco"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Disponível: R$ 40,00");
    verify(sessions, never()).addMovement(any(), any(), any());
  }

  @Test
  @DisplayName("Registra suprimento no caixa aberto")
  void registersSupply() {
    when(sessions.findById("s1", "t1")).thenReturn(Optional.of(openSession("u1", 40)));
    when(sales.findForCashSession("t1", "s1")).thenReturn(List.of());
    when(sessions.addMovement(eq("s1"), eq("t1"), any())).thenReturn(true);

    CashSessionView view =
        useCase.addMovement(OPERATOR, "s1", CashMovementType.SUPPLY, 25.0, "  troco extra ");

    assertThat(view.session().getMovements()).hasSize(1);
    assertThat(view.session().getMovements().get(0).reason()).isEqualTo("troco extra");
    assertThat(view.summary().expectedCash()).isEqualTo(65.0);
  }

  @Test
  @DisplayName("Operador não mexe no caixa de outra pessoa")
  void operatorCannotTouchOtherSession() {
    when(sessions.findById("s1", "t1")).thenReturn(Optional.of(openSession("u2", 40)));

    assertThatThrownBy(() -> useCase.close(OPERATOR, "s1", 40.0, null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("Fecha o caixa com a diferença entre o contado e o esperado")
  void closesWithDifference() {
    when(sessions.findById("s1", "t1")).thenReturn(Optional.of(openSession("u2", 100)));
    when(sales.findForCashSession("t1", "s1")).thenReturn(List.of(cashSale(30, 50)));
    when(sessions.close(any())).thenReturn(true);

    CashSessionView view = useCase.close(ADMIN, "s1", 125.0, " faltou moeda ");

    ArgumentCaptor<CashSession> saved = ArgumentCaptor.forClass(CashSession.class);
    verify(sessions).close(saved.capture());
    assertThat(saved.getValue().getStatus()).isEqualTo(CashSessionStatus.CLOSED);
    assertThat(saved.getValue().getClosedByUserId()).isEqualTo("u9");
    assertThat(view.summary().expectedCash()).isEqualTo(130.0);
    assertThat(view.session().getDifference()).isEqualTo(-5.0);
    assertThat(view.session().getNotes()).isEqualTo("faltou moeda");
  }

  @Test
  @DisplayName("Fechar sem contar deixa a diferença vazia")
  void closesWithoutCount() {
    when(sessions.findById("s1", "t1")).thenReturn(Optional.of(openSession("u1", 10)));
    when(sales.findForCashSession("t1", "s1")).thenReturn(List.of());
    when(sessions.close(any())).thenReturn(true);

    CashSessionView view = useCase.close(OPERATOR, "s1", null, null);

    assertThat(view.session().getCountedAmount()).isNull();
    assertThat(view.session().getDifference()).isNull();
  }

  @Test
  @DisplayName("Caixa já fechado não aceita movimento nem novo fechamento")
  void closedSessionIsReadOnly() {
    CashSession closed = openSession("u1", 10);
    closed.setStatus(CashSessionStatus.CLOSED);
    when(sessions.findById("s1", "t1")).thenReturn(Optional.of(closed));

    assertThatThrownBy(() -> useCase.close(OPERATOR, "s1", 10.0, null))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () -> useCase.addMovement(OPERATOR, "s1", CashMovementType.SUPPLY, 10.0, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("Operador lista só os próprios caixas; o dono vê todos")
  void listFiltersByRole() {
    when(sessions.findOpenedBetween(eq("t1"), any(), any(), any(), anyInt())).thenReturn(List.of());

    useCase.list(OPERATOR, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 6));
    useCase.list(ADMIN, null, null);

    verify(sessions).findOpenedBetween(eq("t1"), eq("u1"), any(), any(), anyInt());
    verify(sessions).findOpenedBetween(eq("t1"), isNull(), any(), any(), anyInt());
  }

  @Test
  @DisplayName("Período maior que 31 dias é recusado")
  void rejectsLongPeriod() {
    assertThatThrownBy(
            () -> useCase.list(ADMIN, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1)))
        .isInstanceOf(BusinessException.class);
  }
}
