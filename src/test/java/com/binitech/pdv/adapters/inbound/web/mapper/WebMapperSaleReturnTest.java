package com.binitech.pdv.adapters.inbound.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.binitech.pdv.adapters.inbound.web.generated.model.CreateSaleItemDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateSaleReturnDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.RefundMethodEnum;
import com.binitech.pdv.adapters.inbound.web.generated.model.SaleDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.SaleStatusEnum;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.domain.SaleReturnRequest;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebMapperSaleReturnTest {

  private final WebMapper mapper = new WebMapperImpl();

  private CreateSaleItemDTO item(String productId, int quantity) {
    return new CreateSaleItemDTO().productId(productId).quantity(quantity);
  }

  @Test
  @DisplayName("Soma itens repetidos e repõe o estoque quando o campo não vem")
  void mapsRequest() {
    CreateSaleReturnDTO dto =
        new CreateSaleReturnDTO()
            .items(List.of(item("p1", 1), item("p1", 2), item("p2", 1)))
            .refundMethod(RefundMethodEnum.DEBIT_CARD)
            .reason("Troca")
            .restock(null);

    SaleReturnRequest request = mapper.toDomain(dto);

    assertThat(request.quantities()).containsEntry("p1", 3).containsEntry("p2", 1);
    assertThat(request.refundMethod()).isEqualTo(RefundMethod.DEBIT_CARD);
    assertThat(request.restock()).isTrue();
    assertThat(mapper.toDomain(dto.restock(false)).restock()).isFalse();
  }

  @Test
  @DisplayName("A venda devolvida sai com status, totais devolvidos e histórico")
  void mapsSaleWithReturns() {
    Sale sale = new Sale();
    sale.setItems(new ArrayList<>(List.of(new SaleItem("p1", "Arroz", 1, 10.0, 6.0))));
    sale.recalculateTotal();
    sale.setPayments(new ArrayList<>(List.of(new Payment(PaymentMethod.CASH, 10.0))));
    sale.registerReturn(
        new SaleReturnRequest(Map.of("p1", 1), RefundMethod.CASH, null, true),
        "r1",
        "u1",
        LocalDateTime.of(2026, 11, 1, 9, 0));

    SaleDTO dto = mapper.toDto(sale);

    assertThat(dto.getStatus()).isEqualTo(SaleStatusEnum.RETURNED);
    assertThat(dto.getReturnedAmount()).isEqualTo(10.0);
    assertThat(dto.getReturns()).hasSize(1);
    assertThat(dto.getReturns().get(0).getRefundMethod()).isEqualTo(RefundMethodEnum.CASH);
    assertThat(dto.getReturns().get(0).getItems().get(0).getProductDescription())
        .isEqualTo("Arroz");
    assertThat(dto.getReturns().get(0).getTimestamp()).isNotNull();
  }
}
