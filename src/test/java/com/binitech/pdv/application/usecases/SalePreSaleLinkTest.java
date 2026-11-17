package com.binitech.pdv.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.outbound.PreSaleRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.PreSale;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.PreSaleStatus;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalePreSaleLinkTest {

  @Mock private SaleRepositoryPort saleRepository;
  @Mock private ProductRepositoryPort productRepository;
  @Mock private PreSaleRepositoryPort preSales;

  private SaleUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new SaleUseCaseImpl(
            saleRepository,
            productRepository,
            ZoneId.of("America/Sao_Paulo"),
            null,
            null,
            preSales);
  }

  private static Product product() {
    Product product = new Product();
    product.setId("p1");
    product.setDescription("Pão francês");
    product.setPrice(1.5);
    product.setCostPrice(0.8);
    product.setStockQuantity(100);
    product.setActive(true);
    product.setTenantId("t1");
    return product;
  }

  private static PreSale preSale(PreSaleStatus status) {
    PreSale preSale = new PreSale();
    preSale.setId("c1");
    preSale.setTenantId("t1");
    preSale.setNumber(42);
    preSale.setStatus(status);
    return preSale;
  }

  private static Sale saleRequest(String preSaleId) {
    Sale sale = new Sale();
    SaleItem item = new SaleItem();
    item.setProductId("p1");
    item.setQuantity(10);
    sale.setItems(new ArrayList<>(List.of(item)));
    sale.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.CASH, 15.0))));
    sale.setPreSaleId(preSaleId);
    return sale;
  }

  @Test
  @DisplayName("Venda com comanda aberta baixa a comanda e guarda o vínculo nos dois lados")
  void paysOpenPreSale() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(productRepository.decreaseStock(eq("p1"), eq("t1"), anyInt())).thenReturn(true);
    when(preSales.findByIdAndTenantId("c1", "t1"))
        .thenReturn(Optional.of(preSale(PreSaleStatus.OPEN)));
    when(preSales.close(eq("c1"), eq("t1"), eq(PreSaleStatus.PAID), any())).thenReturn(true);
    when(saleRepository.save(any()))
        .thenAnswer(
            inv -> {
              Sale sale = inv.getArgument(0);
              sale.setId("v1");
              return sale;
            });

    Sale saved = useCase.createSale(saleRequest("c1"), "u1", "t1");

    assertThat(saved.getPreSaleId()).isEqualTo("c1");
    verify(preSales).attachSale("c1", "t1", "v1");
  }

  @Test
  @DisplayName("Comanda já paga não pode ser paga de novo no caixa")
  void rejectsPaidPreSale() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(preSales.findByIdAndTenantId("c1", "t1"))
        .thenReturn(Optional.of(preSale(PreSaleStatus.PAID)));

    Sale request = saleRequest("c1");

    assertThatThrownBy(() -> useCase.createSale(request, "u1", "t1"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("#42 já foi paga");
    verify(saleRepository, never()).save(any());
    verify(productRepository, never()).decreaseStock(any(), any(), anyInt());
  }

  @Test
  @DisplayName("Se outro caixa pagou a comanda no mesmo instante, a venda é recusada")
  void rejectsConcurrentPayment() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(preSales.findByIdAndTenantId("c1", "t1"))
        .thenReturn(Optional.of(preSale(PreSaleStatus.OPEN)));
    when(preSales.close(eq("c1"), eq("t1"), eq(PreSaleStatus.PAID), any())).thenReturn(false);

    Sale request = saleRequest("c1");

    assertThatThrownBy(() -> useCase.createSale(request, "u1", "t1"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("acabou de mudar");
    verify(saleRepository, never()).save(any());
  }

  @Test
  @DisplayName("Sem estoque, a comanda volta a ficar aberta para tentar de novo")
  void reopensPreSaleWhenStockFails() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(productRepository.decreaseStock(eq("p1"), eq("t1"), anyInt())).thenReturn(false);
    when(preSales.findByIdAndTenantId("c1", "t1"))
        .thenReturn(Optional.of(preSale(PreSaleStatus.OPEN)));
    when(preSales.close(eq("c1"), eq("t1"), eq(PreSaleStatus.PAID), any())).thenReturn(true);

    Sale request = saleRequest("c1");

    assertThatThrownBy(() -> useCase.createSale(request, "u1", "t1"))
        .isInstanceOf(BusinessException.class);
    verify(preSales).reopen("c1", "t1");
    verify(saleRepository, never()).save(any());
  }

  @Test
  @DisplayName("Se salvar a venda falhar, devolve o estoque e reabre a comanda")
  void reopensPreSaleWhenSaveFails() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(productRepository.decreaseStock(eq("p1"), eq("t1"), anyInt())).thenReturn(true);
    when(preSales.findByIdAndTenantId("c1", "t1"))
        .thenReturn(Optional.of(preSale(PreSaleStatus.OPEN)));
    when(preSales.close(eq("c1"), eq("t1"), eq(PreSaleStatus.PAID), any())).thenReturn(true);
    when(saleRepository.save(any())).thenThrow(new IllegalStateException("banco fora"));

    Sale request = saleRequest("c1");

    assertThatThrownBy(() -> useCase.createSale(request, "u1", "t1"))
        .isInstanceOf(IllegalStateException.class);
    verify(productRepository).increaseStock("p1", "t1", 10);
    verify(preSales).reopen("c1", "t1");
  }

  @Test
  @DisplayName("Pix já recebido registra a venda mesmo se a comanda tiver sido paga em outro caixa")
  void confirmedPaymentStillRegistersSale() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(productRepository.decreaseStock(eq("p1"), eq("t1"), anyInt())).thenReturn(true);
    when(preSales.findByIdAndTenantId("c1", "t1"))
        .thenReturn(Optional.of(preSale(PreSaleStatus.PAID)));
    when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    Sale request = saleRequest("c1");
    request.setPaymentConfirmed(true);

    Sale saved = useCase.createSale(request, "u1", "t1");

    assertThat(saved.getPreSaleId()).isNull();
    verify(preSales, never()).attachSale(any(), any(), any());
  }

  @Test
  @DisplayName("Prévia do Pix recusa comanda cancelada antes de gerar o QR")
  void previewRejectsCancelledPreSale() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(preSales.findByIdAndTenantId("c1", "t1"))
        .thenReturn(Optional.of(preSale(PreSaleStatus.CANCELLED)));

    Sale request = saleRequest("c1");

    assertThatThrownBy(() -> useCase.previewSale(request, "t1"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("cancelada");
  }

  @Test
  @DisplayName("Venda comum, sem comanda, não consulta comandas")
  void saleWithoutPreSale() {
    when(productRepository.findById("p1")).thenReturn(Optional.of(product()));
    when(productRepository.decreaseStock(eq("p1"), eq("t1"), anyInt())).thenReturn(true);
    when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Sale saved = useCase.createSale(saleRequest(" "), "u1", "t1");

    assertThat(saved.getPreSaleId()).isNull();
    verify(preSales, never()).findByIdAndTenantId(any(), any());
  }
}
