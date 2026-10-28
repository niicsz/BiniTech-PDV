package com.binitech.pdv.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BarcodeTest {

  @ParameterizedTest
  @ValueSource(
      strings = {"7890000000017", "7891000315507", "96385074", "036000291452", "17891000315504"})
  @DisplayName("GTINs com dígito verificador correto são válidos")
  void validGtins(String code) {
    assertTrue(Barcode.isValid(code));
  }

  @ParameterizedTest
  @ValueSource(strings = {"7890000000011", "7891000315500", "96385075", "036000291453"})
  @DisplayName("GTINs com dígito verificador errado são inválidos")
  void invalidGtins(String code) {
    assertFalse(Barcode.isValid(code));
  }

  @ParameterizedTest
  @ValueSource(strings = {"111", "PAO-FRANCES", "12345", "000123456789012345"})
  @DisplayName("Códigos internos (não GTIN) são aceitos")
  void internalCodes(String code) {
    assertTrue(Barcode.isValid(code));
  }

  @Test
  @DisplayName("Código vazio ou nulo é inválido")
  void blank() {
    assertFalse(Barcode.isValid(null));
    assertFalse(Barcode.isValid(" "));
  }
}
