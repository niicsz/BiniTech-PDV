package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.CashMovementType;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record CashSessionSummary(
    int salesCount,
    double salesTotal,
    double discountTotal,
    double surchargeTotal,
    Map<PaymentMethod, Double> receivedByMethod,
    double cashSales,
    double cashRefunds,
    double supplies,
    double withdrawals,
    double expectedCash) {

  public static CashSessionSummary of(CashSession session, List<Sale> sales) {
    int count = 0;
    BigDecimal total = BigDecimal.ZERO;
    BigDecimal discount = BigDecimal.ZERO;
    BigDecimal surcharge = BigDecimal.ZERO;
    BigDecimal refunds = BigDecimal.ZERO;
    Map<PaymentMethod, BigDecimal> byMethod = new EnumMap<>(PaymentMethod.class);
    for (Sale sale : sales) {
      if (Objects.equals(sale.getCashSessionId(), session.getId())) {
        count++;
        total = total.add(Money.of(sale.getTotalAmount()));
        discount = discount.add(Money.of(sale.getDiscount()));
        surcharge = surcharge.add(Money.of(sale.getSurcharge()));
        addPayments(sale, byMethod);
      }
      for (SaleReturn saleReturn : sale.getReturns()) {
        if (Objects.equals(saleReturn.getCashSessionId(), session.getId())
            && saleReturn.getRefundMethod() == RefundMethod.CASH) {
          refunds = refunds.add(Money.of(saleReturn.getRefundAmount()));
        }
      }
    }
    BigDecimal cash = byMethod.getOrDefault(PaymentMethod.CASH, BigDecimal.ZERO);
    BigDecimal supplies = movements(session, CashMovementType.SUPPLY);
    BigDecimal withdrawals = movements(session, CashMovementType.WITHDRAWAL);
    BigDecimal expected =
        Money.of(session.getOpeningAmount())
            .add(cash)
            .add(supplies)
            .subtract(withdrawals)
            .subtract(refunds);
    Map<PaymentMethod, Double> received = new EnumMap<>(PaymentMethod.class);
    byMethod.forEach((method, amount) -> received.put(method, amount.doubleValue()));
    return new CashSessionSummary(
        count,
        total.doubleValue(),
        discount.doubleValue(),
        surcharge.doubleValue(),
        received,
        cash.doubleValue(),
        refunds.doubleValue(),
        supplies.doubleValue(),
        withdrawals.doubleValue(),
        expected.doubleValue());
  }

  private static void addPayments(Sale sale, Map<PaymentMethod, BigDecimal> byMethod) {
    for (Payment payment : sale.getPayments()) {
      if (payment.getMethod() != null) {
        byMethod.merge(payment.getMethod(), Money.of(payment.getAmount()), BigDecimal::add);
      }
    }
    BigDecimal change = Money.of(sale.getChange());
    if (change.signum() > 0) {
      byMethod.merge(PaymentMethod.CASH, change.negate(), BigDecimal::add);
    }
  }

  private static BigDecimal movements(CashSession session, CashMovementType type) {
    return session.getMovements().stream()
        .filter(m -> m.type() == type)
        .map(m -> Money.of(m.amount()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
