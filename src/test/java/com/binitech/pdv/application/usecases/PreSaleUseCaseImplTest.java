package com.binitech.pdv.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.inbound.PreSaleUseCasePort.Attendant;
import com.binitech.pdv.application.ports.inbound.PreSaleUseCasePort.ItemRequest;
import com.binitech.pdv.application.ports.outbound.PreSaleRepositoryPort;
import com.binitech.pdv.application.ports.outbound.ProductRepositoryPort;
import com.binitech.pdv.domain.PreSale;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.PreSaleStatus;
import com.binitech.pdv.utils.enums.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PreSaleUseCaseImplTest {

  private static final Instant NOW = Instant.parse("2026-11-17T12:00:00Z");
  private static final Attendant BALCONISTA = new Attendant("u1", "ana", "t1", Role.OPERATOR);
  private static final Attendant OUTRO = new Attendant("u2", "bia", "t1", Role.OPERATOR);
  private static final Attendant DONO = new Attendant("u9", "dono", "t1", Role.TENANT_ADMIN);

  @Mock private PreSaleRepositoryPort preSales;
  @Mock private ProductRepositoryPort products;

  private PreSaleUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new PreSaleUseCaseImpl(preSales, products, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static Product product(String id, String description, double price, String tenantId) {
    Product product = new Product();
    product.setId(id);
    product.setDescription(description);
    product.setPrice(price);
    product.setActive(true);
    product.setTenantId(tenantId);
    return product;
  }

  private static PreSale open(String attendantId) {
    PreSale preSale = new PreSale();
    preSale.setId("c1");
    preSale.setTenantId("t1");
    preSale.setNumber(7);
    preSale.setStatus(PreSaleStatus.OPEN);
    preSale.setAttendantId(attendantId);
    preSale.setItems(List.of());
    return preSale;
  }

  @Test
  @DisplayName("Abre a comanda com número da loja, preço do cadastro e linhas repetidas somadas")
  void createsPreSale() {
    when(products.findById("p1")).thenReturn(Optional.of(product("p1", "Pão francês", 0.75, "t1")));
    when(products.findById("p2"))
        .thenReturn(Optional.of(product("p2", "Mortadela 200 g", 8.9, "t1")));
    when(preSales.nextNumber("t1")).thenReturn(15L);
    when(preSales.save(any())).thenAnswer(inv -> inv.getArgument(0));

    PreSale saved =
        useCase.create(
            BALCONISTA,
            List.of(new ItemRequest("p1", 6), new ItemRequest("p2", 1), new ItemRequest("p1", 4)),
            "  sem cebola  ");

    assertThat(saved.getNumber()).isEqualTo(15);
    assertThat(saved.getStatus()).isEqualTo(PreSaleStatus.OPEN);
    assertThat(saved.getItems()).hasSize(2);
    assertThat(saved.getItems().get(0).quantity()).isEqualTo(10);
    assertThat(saved.getTotal()).isEqualTo(16.4);
    assertThat(saved.getNotes()).isEqualTo("sem cebola");
    assertThat(saved.getAttendantId()).isEqualTo("u1");
    assertThat(saved.getAttendantName()).isEqualTo("ana");
    assertThat(saved.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("Produto de outra loja não entra na comanda")
  void rejectsProductFromOtherTenant() {
    when(products.findById("p1")).thenReturn(Optional.of(product("p1", "Pão", 1, "t2")));

    assertThatThrownBy(() -> useCase.create(BALCONISTA, List.of(new ItemRequest("p1", 1)), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("catálogo");
    verify(preSales, never()).nextNumber(any());
  }

  @Test
  @DisplayName("Produto inativo não entra na comanda")
  void rejectsInactiveProduct() {
    Product inactive = product("p1", "Sonho", 4, "t1");
    inactive.setActive(false);
    when(products.findById("p1")).thenReturn(Optional.of(inactive));

    assertThatThrownBy(() -> useCase.create(BALCONISTA, List.of(new ItemRequest("p1", 1)), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("inativo");
  }

  @Test
  @DisplayName("Comanda vazia, quantidade zero ou acima do limite é recusada")
  void validatesItems() {
    assertThatThrownBy(() -> useCase.create(BALCONISTA, List.of(), null))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> useCase.create(BALCONISTA, List.of(new ItemRequest("p1", 0)), null))
        .isInstanceOf(BusinessException.class);
    when(products.findById("p1")).thenReturn(Optional.of(product("p1", "Pão", 1, "t1")));
    assertThatThrownBy(() -> useCase.create(BALCONISTA, List.of(new ItemRequest("p1", 1000)), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("limite");
    List<ItemRequest> many =
        IntStream.range(0, 101).mapToObj(i -> new ItemRequest("p" + i, 1)).toList();
    assertThatThrownBy(() -> useCase.create(BALCONISTA, many, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("100");
  }

  @Test
  @DisplayName("Observação longa demais é recusada")
  void rejectsLongNotes() {
    when(products.findById("p1")).thenReturn(Optional.of(product("p1", "Pão", 1, "t1")));
    String notes = String.join("", Collections.nCopies(281, "a"));

    assertThatThrownBy(() -> useCase.create(BALCONISTA, List.of(new ItemRequest("p1", 1)), notes))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("Busca pelo número dentro da loja e avisa quando não existe")
  void findsByNumber() {
    when(preSales.findByNumber("t1", 7)).thenReturn(Optional.of(open("u1")));
    when(preSales.findByNumber("t1", 8)).thenReturn(Optional.empty());

    assertThat(useCase.findByNumber(OUTRO, 7).getId()).isEqualTo("c1");
    assertThatThrownBy(() -> useCase.findByNumber(OUTRO, 8))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("#8");
  }

  @Test
  @DisplayName("Quem abriu a comanda pode cancelar")
  void attendantCancels() {
    when(preSales.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(open("u1")));
    when(preSales.close(eq("c1"), eq("t1"), eq(PreSaleStatus.CANCELLED), eq(NOW))).thenReturn(true);

    useCase.cancel(BALCONISTA, "c1");

    verify(preSales).close("c1", "t1", PreSaleStatus.CANCELLED, NOW);
  }

  @Test
  @DisplayName("O dono pode cancelar a comanda de qualquer atendente")
  void managerCancels() {
    when(preSales.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(open("u1")));
    when(preSales.close(any(), any(), any(), any())).thenReturn(true);

    useCase.cancel(DONO, "c1");

    verify(preSales).close("c1", "t1", PreSaleStatus.CANCELLED, NOW);
  }

  @Test
  @DisplayName("Outro atendente não cancela a comanda de um colega")
  void otherOperatorCannotCancel() {
    when(preSales.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(open("u1")));

    assertThatThrownBy(() -> useCase.cancel(OUTRO, "c1")).isInstanceOf(AccessDeniedException.class);
    verify(preSales, never()).close(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Comanda já paga não pode ser cancelada")
  void cannotCancelPaid() {
    PreSale paid = open("u1");
    paid.setStatus(PreSaleStatus.PAID);
    when(preSales.findByIdAndTenantId("c1", "t1")).thenReturn(Optional.of(paid));

    assertThatThrownBy(() -> useCase.cancel(DONO, "c1"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("já foi paga");
  }

  @Test
  @DisplayName("Usuário sem loja não usa comandas")
  void requiresTenant() {
    Attendant platform = new Attendant("admin", "admin", null, Role.SUPER_ADMIN);

    assertThatThrownBy(() -> useCase.list(platform, null))
        .isInstanceOf(AccessDeniedException.class);
  }
}
