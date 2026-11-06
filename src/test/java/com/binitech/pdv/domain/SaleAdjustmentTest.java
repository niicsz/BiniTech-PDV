package com.binitech.pdv.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaleAdjustmentTest {

  private static Sale sale(double discount, double surcharge, SaleItem... items) {
    Sale sale = new Sale();
    sale.setItems(new ArrayList<>(List.of(items)));
    sale.setDiscount(discount);
    sale.setSurcharge(surcharge);
    sale.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.CASH, 1000))));
    sale.recalculate();
    return sale;
  }

  private static SaleItem item(String id, int quantity, double price) {
    return new SaleItem(id, "Produto " + id, quantity, price, price / 2);
  }

  @Test
  @DisplayName("Desconto e acréscimo mudam o total e são rateados pelos itens sem sobrar centavo")
  void allocatesAdjustmentsAcrossItems() {
    Sale sale = sale(10.00, 3.33, item("a", 1, 10.00), item("b", 2, 15.00), item("c", 3, 3.33));

    assertThat(sale.getItemsTotal()).isEqualTo(49.99);
    assertThat(sale.getTotalAmount()).isEqualTo(43.32);
    double discounts = sale.getItems().stream().mapToDouble(SaleItem::getDiscount).sum();
    double surcharges = sale.getItems().stream().mapToDouble(SaleItem::getSurcharge).sum();
    assertThat(Money.round(discounts)).isEqualTo(10.00);
    assertThat(Money.round(surcharges)).isEqualTo(3.33);
    assertThat(Money.sum(sale.getItems().stream().map(SaleItem::netSubtotal))).isEqualTo(43.32);
    assertThat(sale.getItems().get(1).getDiscount())
        .isGreaterThan(sale.getItems().get(0).getDiscount());
  }

  @Test
  @DisplayName("Sem desconto e acréscimo o total continua igual à soma dos itens")
  void noAdjustmentsKeepsTotal() {
    Sale sale = sale(0, 0, item("a", 2, 4.50));

    assertThat(sale.getTotalAmount()).isEqualTo(9.00);
    assertThat(sale.getItems().get(0).getDiscount()).isZero();
    assertThat(sale.getItems().get(0).netSubtotal()).isEqualTo(9.00);
  }

  @Test
  @DisplayName("Desconto igual ou maior que o total dos produtos é recusado")
  void rejectsDiscountCoveringEverything() {
    Sale sale = sale(20.00, 0, item("a", 2, 10.00));

    assertThatThrownBy(sale::validate)
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("desconto precisa ser menor");
  }

  @Test
  @DisplayName("Desconto negativo é recusado")
  void rejectsNegativeDiscount() {
    Sale sale = sale(-1, 0, item("a", 1, 10.00));

    assertThatThrownBy(sale::validate).isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("Devolução reembolsa o valor líquido e a última parte leva o restante do desconto")
  void returnsRefundNetValue() {
    Sale sale = sale(1.00, 0, item("a", 3, 10.00));

    SaleReturn first =
        sale.registerReturn(
            new SaleReturnRequest(Map.of("a", 1), RefundMethod.CASH, null, true),
            "r1",
            "u1",
            LocalDateTime.now());
    SaleReturn second =
        sale.registerReturn(
            new SaleReturnRequest(Map.of("a", 2), RefundMethod.CASH, null, true),
            "r2",
            "u1",
            LocalDateTime.now());

    assertThat(first.getRefundAmount()).isEqualTo(9.67);
    assertThat(second.getRefundAmount()).isEqualTo(19.33);
    assertThat(Money.round(first.getRefundAmount() + second.getRefundAmount()))
        .isEqualTo(sale.getTotalAmount());
  }
}
