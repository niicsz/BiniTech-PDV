package com.binitech.pdv.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import com.binitech.pdv.utils.enums.SaleStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaleReturnTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 1, 10, 0);

  private Sale sale(PaymentMethod method, boolean paid) {
    Sale sale = new Sale();
    sale.setItems(
        new ArrayList<>(
            List.of(
                new SaleItem("p1", "Arroz", 3, 10.0, 6.0),
                new SaleItem("p2", "Café", 1, 19.9, 12.0))));
    sale.recalculateTotal();
    sale.setPayments(new ArrayList<>(List.of(new Payment(method, sale.getTotalAmount()))));
    sale.setPaid(paid);
    return sale;
  }

  private SaleReturnRequest request(Map<String, Integer> quantities, RefundMethod method) {
    return new SaleReturnRequest(quantities, method, "  Produto com defeito ", true);
  }

  private SaleReturn register(Sale sale, Map<String, Integer> quantities, RefundMethod method) {
    return sale.registerReturn(request(quantities, method), "r1", "u1", NOW);
  }

  @Test
  @DisplayName("Devolução parcial calcula reembolso e custo pelo preço da venda")
  void partialReturn() {
    Sale sale = sale(PaymentMethod.CASH, false);

    SaleReturn saleReturn = register(sale, Map.of("p1", 2), RefundMethod.CASH);

    assertEquals(20.0, saleReturn.getRefundAmount());
    assertEquals(12.0, saleReturn.getRefundCost());
    assertEquals("Produto com defeito", saleReturn.getReason());
    assertEquals("Arroz", saleReturn.getItems().get(0).getProductDescription());
    assertEquals(SaleStatus.PARTIALLY_RETURNED, sale.getStatus());
    assertEquals(20.0, sale.getReturnedAmount());
    assertEquals(2, sale.returnedQuantity("p1"));
  }

  @Test
  @DisplayName("Devolver tudo, em uma ou mais vezes, marca a venda como devolvida")
  void fullReturnAcrossReturns() {
    Sale sale = sale(PaymentMethod.PIX, false);
    register(sale, Map.of("p1", 2), RefundMethod.PIX);

    Map<String, Integer> rest = new LinkedHashMap<>();
    rest.put("p1", 1);
    rest.put("p2", 1);
    register(sale, rest, RefundMethod.PIX);

    assertEquals(SaleStatus.RETURNED, sale.getStatus());
    assertEquals(49.9, sale.getReturnedAmount());
    assertEquals(30.0, sale.getReturnedCost());
    assertEquals(2, sale.getReturns().size());
    SaleReturnRequest afterFullReturn = request(Map.of("p1", 1), RefundMethod.PIX);
    BusinessException again =
        assertThrows(
            BusinessException.class, () -> sale.registerReturn(afterFullReturn, "r", "u", NOW));
    assertTrue(again.getMessage().contains("devolvida por completo"));
  }

  @Test
  @DisplayName("Não deixa devolver mais do que foi vendido menos o já devolvido")
  void rejectsOverQuantity() {
    Sale sale = sale(PaymentMethod.CASH, false);
    register(sale, Map.of("p1", 2), RefundMethod.CASH);

    SaleReturnRequest tooMany = request(Map.of("p1", 2), RefundMethod.CASH);
    BusinessException error =
        assertThrows(BusinessException.class, () -> sale.registerReturn(tooMany, "r", "u", NOW));

    assertTrue(error.getMessage().contains("Disponível: 1"));
    assertEquals(1, sale.getReturns().size());
  }

  @Test
  @DisplayName("Rejeita produto fora da venda, lista vazia e forma de reembolso ausente")
  void rejectsInvalidRequests() {
    Sale sale = sale(PaymentMethod.CASH, false);

    SaleReturnRequest notInSale = request(Map.of("outro", 1), RefundMethod.CASH);
    assertThrows(BusinessException.class, () -> sale.registerReturn(notInSale, "r", "u", NOW));
    SaleReturnRequest zeroQuantity = request(Map.of("p1", 0), RefundMethod.CASH);
    assertThrows(BusinessException.class, () -> sale.registerReturn(zeroQuantity, "r", "u", NOW));
    SaleReturnRequest noItems = request(Map.of(), RefundMethod.CASH);
    assertThrows(BusinessException.class, () -> sale.registerReturn(noItems, "r", "u", NOW));
    SaleReturnRequest noRefundMethod = request(Map.of("p1", 1), null);
    assertThrows(BusinessException.class, () -> sale.registerReturn(noRefundMethod, "r", "u", NOW));
    assertEquals(SaleStatus.COMPLETED, sale.getStatus());
  }

  @Test
  @DisplayName("Fiado em aberto só aceita abater o débito; venda paga não aceita abatimento")
  void crediarioRules() {
    Sale open = sale(PaymentMethod.CREDIARIO, false);
    SaleReturnRequest cashOnOpenDebt = request(Map.of("p1", 1), RefundMethod.CASH);
    assertThrows(BusinessException.class, () -> open.registerReturn(cashOnOpenDebt, "r", "u", NOW));
    SaleReturn deducted = register(open, Map.of("p1", 1), RefundMethod.DEBT_DEDUCTION);
    assertEquals(RefundMethod.DEBT_DEDUCTION, deducted.getRefundMethod());

    Sale paid = sale(PaymentMethod.CREDIARIO, true);
    SaleReturnRequest deductionOnPaidSale = request(Map.of("p1", 1), RefundMethod.DEBT_DEDUCTION);
    assertThrows(
        BusinessException.class, () -> paid.registerReturn(deductionOnPaidSale, "r", "u", NOW));
    Sale cash = sale(PaymentMethod.CASH, false);
    SaleReturnRequest deductionOnCashSale = request(Map.of("p1", 1), RefundMethod.DEBT_DEDUCTION);
    assertThrows(
        BusinessException.class, () -> cash.registerReturn(deductionOnCashSale, "r", "u", NOW));
  }

  @Test
  @DisplayName("Venda antiga, sem status gravado, conta como concluída e sem devoluções")
  void legacySale() {
    Sale sale = new Sale();
    sale.setStatus(null);
    sale.setReturns(null);

    assertEquals(SaleStatus.COMPLETED, sale.getStatus());
    assertTrue(sale.getReturns().isEmpty());
    SaleReturn blankReason =
        sale(PaymentMethod.CASH, false)
            .registerReturn(
                new SaleReturnRequest(Map.of("p2", 1), RefundMethod.CASH, " ", false),
                "r2",
                "u1",
                NOW);
    assertNull(blankReason.getReason());
  }
}
