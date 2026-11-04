package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.binitech.pdv.adapters.inbound.web.generated.model.CreateProductDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateSaleDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.CreateSaleItemDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.LoginRequestDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PaymentDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PaymentMethodEnum;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestLimitsTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  private static CreateProductDTO product() {
    return new CreateProductDTO()
        .barcode("7891000100103")
        .description("Arroz tipo 1 5kg")
        .price(27.9)
        .stockQuantity(10);
  }

  @Test
  @DisplayName("Produto válido passa e textos ou valores exagerados são recusados")
  void productLimits() {
    assertTrue(validator.validate(product()).isEmpty());
    assertFalse(validator.validate(product().description("x".repeat(201))).isEmpty());
    assertFalse(validator.validate(product().category("x".repeat(101))).isEmpty());
    assertFalse(validator.validate(product().price(1_000_001.0)).isEmpty());
    assertFalse(validator.validate(product().stockQuantity(10_000_001)).isEmpty());
  }

  @Test
  @DisplayName("Venda recusa lista de itens enorme, quantidade absurda e nome de cliente gigante")
  void saleLimits() {
    PaymentDTO payment = new PaymentDTO().method(PaymentMethodEnum.CASH).amount(10.0);
    List<CreateSaleItemDTO> many = new ArrayList<>();
    for (int i = 0; i < 501; i++) {
      many.add(new CreateSaleItemDTO().productId("p" + i).quantity(1));
    }
    CreateSaleDTO ok =
        new CreateSaleDTO()
            .items(List.of(new CreateSaleItemDTO().productId("p1").quantity(1)))
            .payments(List.of(payment));
    assertTrue(validator.validate(ok).isEmpty());
    assertFalse(
        validator.validate(new CreateSaleDTO().items(many).payments(List.of(payment))).isEmpty());
    assertFalse(
        validator
            .validate(
                new CreateSaleDTO()
                    .items(List.of(new CreateSaleItemDTO().productId("p1").quantity(100_001)))
                    .payments(List.of(payment)))
            .isEmpty());
    assertFalse(validator.validate(ok.customerName("x".repeat(121))).isEmpty());
  }

  @Test
  @DisplayName("Login recusa senha gigante antes de chegar ao serviço de autenticação")
  void loginLimits() {
    assertTrue(
        validator.validate(new LoginRequestDTO().username("loja").password("senha123")).isEmpty());
    assertFalse(
        validator
            .validate(new LoginRequestDTO().username("loja").password("x".repeat(1025)))
            .isEmpty());
  }
}
