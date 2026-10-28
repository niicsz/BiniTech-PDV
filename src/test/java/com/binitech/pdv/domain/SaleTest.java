package com.binitech.pdv.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.PaymentMethod;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaleTest {

  @Test
  @DisplayName("Construtor padrão deve inicializar listas vazias")
  void defaultConstructor_shouldInitializeFields() {
    Sale sale = new Sale();

    assertNotNull(sale.getItems());
    assertNotNull(sale.getPayments());
    assertTrue(sale.getItems().isEmpty());
    assertTrue(sale.getPayments().isEmpty());
    assertNull(sale.getTimestamp());
  }

  @Test
  @DisplayName("addItem deve adicionar item e recalcular total")
  void addItem_shouldAddItemAndRecalculateTotal() {
    Sale sale = new Sale();
    SaleItem item = new SaleItem("p1", "Produto 1", 2, 10.0, 5.0);

    sale.addItem(item);

    assertEquals(1, sale.getItems().size());
    assertEquals(20.0, sale.getTotalAmount(), 0.01);
    assertEquals(10.0, sale.getTotalCost(), 0.01);
  }

  @Test
  @DisplayName("addItem com múltiplos itens deve somar totais")
  void addItem_multipleItems_shouldSumTotals() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Produto 1", 2, 10.0, 5.0));
    sale.addItem(new SaleItem("p2", "Produto 2", 1, 30.0, 15.0));

    assertEquals(2, sale.getItems().size());
    assertEquals(50.0, sale.getTotalAmount(), 0.01);
    assertEquals(25.0, sale.getTotalCost(), 0.01);
  }

  @Test
  @DisplayName("removeItem deve remover item e recalcular total")
  void removeItem_shouldRemoveItemAndRecalculateTotal() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Produto 1", 2, 10.0, 5.0));
    sale.addItem(new SaleItem("p2", "Produto 2", 1, 30.0, 15.0));

    sale.removeItem("p1");

    assertEquals(1, sale.getItems().size());
    assertEquals(30.0, sale.getTotalAmount(), 0.01);
  }

  @Test
  @DisplayName("calculatePayment deve calcular troco corretamente")
  void calculatePayment_shouldCalculateChangeCorrectly() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Produto 1", 2, 10.0, 5.0));
    sale.setPayments(List.of(new Payment(PaymentMethod.CASH, 50.0)));

    sale.calculatePayment();

    assertEquals(50.0, sale.getTotalPaid(), 0.01);
    assertEquals(30.0, sale.getChange(), 0.01);
  }

  @Test
  @DisplayName("calculatePayment sem troco deve retornar zero")
  void calculatePayment_exactAmount_shouldReturnZeroChange() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Produto 1", 1, 10.0, 5.0));
    sale.setPayments(List.of(new Payment(PaymentMethod.PIX, 10.0)));

    sale.calculatePayment();

    assertEquals(10.0, sale.getTotalPaid(), 0.01);
    assertEquals(0.0, sale.getChange(), 0.01);
  }

  @Test
  @DisplayName("validate com itens e pagamento suficiente deve passar")
  void validate_withValidData_shouldNotThrow() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Produto 1", 2, 10.0, 5.0));
    sale.setPayments(List.of(new Payment(PaymentMethod.CASH, 20.0)));
    sale.recalculate();

    assertDoesNotThrow(() -> sale.validate());
    assertEquals(20.0, sale.getTotalPaid(), 0.01);
    assertEquals(0.0, sale.getChange(), 0.01);
  }

  @Test
  @DisplayName("validate sem itens deve lançar exceção")
  void validate_withNoItems_shouldThrowException() {
    Sale sale = new Sale();
    sale.setPayments(List.of(new Payment(PaymentMethod.CASH, 10.0)));

    BusinessException exception = assertThrows(BusinessException.class, () -> sale.validate());

    assertTrue(exception.getMessage().contains("ao menos um item"));
  }

  @Test
  @DisplayName("validate sem pagamentos deve lançar exceção")
  void validate_withNoPayments_shouldThrowException() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Produto 1", 1, 10.0, 5.0));

    BusinessException exception = assertThrows(BusinessException.class, () -> sale.validate());

    assertTrue(exception.getMessage().contains("ao menos uma forma de pagamento"));
  }

  @Test
  @DisplayName("validate com pagamento insuficiente deve lançar exceção")
  void validate_withInsufficientPayment_shouldThrowException() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Produto 1", 2, 10.0, 5.0));
    sale.setPayments(List.of(new Payment(PaymentMethod.CASH, 5.0)));
    sale.recalculate();

    BusinessException exception = assertThrows(BusinessException.class, () -> sale.validate());

    assertTrue(exception.getMessage().contains("Pagamento insuficiente"));
  }

  @Test
  @DisplayName("validate deve aceitar pagamento exato com centavos (2x 8,99 + 9,90 = 27,88)")
  void validate_exactPaymentWithCents_shouldPass() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Arroz", 2, 8.99, 5.5));
    sale.addItem(new SaleItem("p2", "Feijao", 1, 9.90, 6.0));
    sale.setPayments(List.of(new Payment(PaymentMethod.PIX, 27.88)));

    sale.recalculate();

    assertDoesNotThrow(sale::validate);
    assertEquals(27.88, sale.getTotalAmount());
    assertEquals(0.0, sale.getChange());
  }

  @Test
  @DisplayName("validate deve aceitar pagamento dividido que fecha o total exato")
  void validate_splitExactPayment_shouldPass() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Cafe", 1, 18.90, 12.0));
    sale.addItem(new SaleItem("p2", "Feijao", 1, 9.90, 6.0));
    sale.setPayments(
        List.of(new Payment(PaymentMethod.PIX, 0.1), new Payment(PaymentMethod.DEBIT_CARD, 28.7)));

    sale.recalculate();

    assertDoesNotThrow(sale::validate);
    assertEquals(28.80, sale.getTotalPaid());
  }

  @Test
  @DisplayName("validate deve recusar pagamento um centavo menor que o total")
  void validate_oneCentShort_shouldThrow() {
    Sale sale = new Sale();
    sale.addItem(new SaleItem("p1", "Arroz", 2, 8.99, 5.5));
    sale.addItem(new SaleItem("p2", "Feijao", 1, 9.90, 6.0));
    sale.setPayments(List.of(new Payment(PaymentMethod.PIX, 27.87)));

    sale.recalculate();

    assertThrows(BusinessException.class, sale::validate);
  }
}
