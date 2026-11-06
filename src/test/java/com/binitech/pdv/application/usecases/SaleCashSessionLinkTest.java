package com.binitech.pdv.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.outbound.CashSessionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.CashSession;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.SaleReturnRequest;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import com.binitech.pdv.utils.enums.Role;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleCashSessionLinkTest {

  @Mock private SaleRepositoryPort saleRepository;
  @Mock private ProductRepositoryPort productRepository;
  @Mock private CashSessionRepositoryPort cashSessions;

  private SaleUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new SaleUseCaseImpl(
            saleRepository, productRepository, ZoneId.of("America/Sao_Paulo"), null, cashSessions);
  }

  private static CashSession session() {
    CashSession session = new CashSession();
    session.setId("cx1");
    return session;
  }

  private static Product product() {
    Product product = new Product();
    product.setId("p1");
    product.setDescription("Refrigerante");
    product.setPrice(10.0);
    product.setCostPrice(6.0);
    product.setStockQuantity(10);
    product.setActive(true);
    product.setTenantId("t1");
    return product;
  }

  private static Sale saleRequest(double discount) {
    Sale sale = new Sale();
    SaleItem item = new SaleItem();
    item.setProductId("p1");
    item.setQuantity(2);
    sale.setItems(new ArrayList<>(List.of(item)));
    sale.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.CASH, 20.0))));
    sale.setDiscount(discount);
    return sale;
  }

  @Test
  @DisplayName("Venda feita com caixa aberto fica ligada ao caixa do operador e aplica o desconto")
  void linksSaleToOpenSession() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(productRepository.decreaseStock(eq("p1"), eq("t1"), anyInt())).thenReturn(true);
    when(cashSessions.findOpen("t1", "u1")).thenReturn(Optional.of(session()));
    when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Sale saved = useCase.createSale(saleRequest(2.0), "u1", "t1");

    assertThat(saved.getCashSessionId()).isEqualTo("cx1");
    assertThat(saved.getItemsTotal()).isEqualTo(20.0);
    assertThat(saved.getTotalAmount()).isEqualTo(18.0);
    assertThat(saved.getChange()).isEqualTo(2.0);
    assertThat(saved.getItems().get(0).getDiscount()).isEqualTo(2.0);
  }

  @Test
  @DisplayName("Venda sem caixa aberto continua funcionando, só fica sem caixa")
  void saleWithoutOpenSession() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(productRepository.decreaseStock(eq("p1"), eq("t1"), anyInt())).thenReturn(true);
    when(cashSessions.findOpen(anyString(), anyString())).thenReturn(Optional.empty());
    when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Sale saved = useCase.createSale(saleRequest(0), "u1", "t1");

    assertThat(saved.getCashSessionId()).isNull();
    assertThat(saved.getTotalAmount()).isEqualTo(20.0);
  }

  @Test
  @DisplayName("Devolução em dinheiro sai do caixa aberto de quem devolveu")
  void cashRefundLinksToSession() {
    Sale sale = saleRequest(0);
    sale.setId("v1");
    sale.setTenantId("t1");
    sale.getItems().get(0).setUnitPrice(10.0);
    sale.recalculate();
    when(saleRepository.findById("v1")).thenReturn(Optional.of(sale));
    when(saleRepository.appendReturn(any(), anyInt())).thenReturn(true);
    when(cashSessions.findOpen("t1", "admin")).thenReturn(Optional.of(session()));

    Sale updated =
        useCase.registerReturn(
            "v1",
            new SaleReturnRequest(Map.of("p1", 1), RefundMethod.CASH, null, false),
            "admin",
            "t1",
            Role.TENANT_ADMIN);

    assertThat(updated.getReturns().get(0).getCashSessionId()).isEqualTo("cx1");
  }
}
