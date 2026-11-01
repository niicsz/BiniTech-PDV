package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleEventPublisherPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleCompletedEvent;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.utils.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleFiscalPublisherTest {

  @Mock private SaleEventPublisherPort eventPublisher;
  @Mock private SaleRepositoryPort saleRepository;
  @Mock private ProductRepositoryPort productRepository;
  private SaleFiscalPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher =
        new SaleFiscalPublisher(
            eventPublisher, saleRepository, productRepository, ZoneId.of("America/Sao_Paulo"));
  }

  private Sale sale() {
    SaleItem item = new SaleItem();
    item.setProductId("p1");
    item.setProductDescription("Refrigerante");
    item.setQuantity(2);
    item.setUnitPrice(5.5);
    item.setSubtotal(11.0);
    Payment payment = new Payment();
    payment.setMethod(PaymentMethod.PIX);
    payment.setAmount(11.0);
    Sale sale = new Sale();
    sale.setId("s1");
    sale.setTenantId("t1");
    sale.setItems(List.of(item));
    sale.setPayments(List.of(payment));
    sale.setTimestamp(LocalDateTime.of(2026, 10, 31, 12, 0));
    sale.setCustomerCpf("52998224725");
    sale.setFiscalPending(true);
    return sale;
  }

  private Product product() {
    Product product = new Product();
    product.setId("p1");
    product.setBarcode("7894900011517");
    product.setNcm("22021000");
    product.setCfop("5102");
    product.setCsosn("102");
    return product;
  }

  @Test
  @DisplayName("Publica a venda com os dados fiscais do produto e marca como enviada")
  void publishesAndMarks() {
    Sale sale = sale();
    when(eventPublisher.publishCompleted(any())).thenReturn(true);

    publisher.publish(sale, Map.of("p1", product()));

    ArgumentCaptor<SaleCompletedEvent> event = ArgumentCaptor.forClass(SaleCompletedEvent.class);
    verify(eventPublisher).publishCompleted(event.capture());
    SaleCompletedEvent sent = event.getValue();
    assertEquals("t1", sent.tenantId());
    assertEquals("52998224725", sent.consumerCpf());
    assertEquals("2026-10-31T15:00:00Z", sent.occurredAt().toString());
    assertEquals("22021000", sent.items().get(0).ncm());
    assertEquals("7894900011517", sent.items().get(0).barcode());
    assertEquals(new BigDecimal("11.00"), sent.items().get(0).total());
    assertEquals("PIX", sent.payments().get(0).method());
    verify(saleRepository).markFiscalPublished("s1");
    assertFalse(sale.isFiscalPending());
  }

  @Test
  @DisplayName("Se o RabbitMQ falhar, a venda continua pendente para o reenvio")
  void failureKeepsPending() {
    Sale sale = sale();
    when(eventPublisher.publishCompleted(any())).thenReturn(false);
    when(productRepository.findById("p1")).thenReturn(Optional.empty());

    publisher.publish(sale, Map.of());

    verify(saleRepository, never()).markFiscalPublished(any());
    assertTrue(sale.isFiscalPending());
  }

  @Test
  @DisplayName("O reenvio busca as vendas pendentes e completa os dados do produto no banco")
  void republishesPending() {
    Sale pending = sale();
    when(saleRepository.findFiscalPending(200)).thenReturn(List.of(pending));
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(eventPublisher.publishCompleted(any())).thenReturn(true);

    assertEquals(1, publisher.publishPending(200));
    verify(saleRepository).markFiscalPublished("s1");
  }

  @Test
  @DisplayName("Produto apagado não impede o envio: NCM fica vazio para o padrão da loja")
  void missingProduct() {
    when(productRepository.findById("p1")).thenReturn(Optional.empty());

    SaleCompletedEvent event = publisher.toEvent(sale(), Map.of());

    assertNull(event.items().get(0).ncm());
    assertEquals("Refrigerante", event.items().get(0).description());
  }
}
