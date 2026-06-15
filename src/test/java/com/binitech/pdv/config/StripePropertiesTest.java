package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StripePropertiesTest {

  private StripeProperties props(String secret) {
    return new StripeProperties(
        secret, "whsec_test", "price_starter", "price_pro", "price_enterprise");
  }

  @Test
  @DisplayName("priceForTier resolve o price de cada plano pago e null p/ cortesia/desconhecido")
  void priceForTier_shouldResolve() {
    StripeProperties props = props("sk_test");
    assertEquals("price_starter", props.priceForTier("starter"));
    assertEquals("price_pro", props.priceForTier("PRO"));
    assertEquals("price_enterprise", props.priceForTier("enterprise"));
    assertNull(props.priceForTier("free"));
    assertNull(props.priceForTier(null));
  }

  @Test
  @DisplayName("tierForPrice faz o caminho inverso e ignora ids desconhecidos/vazios")
  void tierForPrice_shouldReverse() {
    StripeProperties props = props("sk_test");
    assertEquals("starter", props.tierForPrice("price_starter"));
    assertEquals("pro", props.tierForPrice("price_pro"));
    assertEquals("enterprise", props.tierForPrice("price_enterprise"));
    assertNull(props.tierForPrice("price_x"));
    assertNull(props.tierForPrice(""));
    assertNull(props.tierForPrice(null));
  }

  @Test
  @DisplayName("Round-trip tier -> price -> tier é consistente")
  void roundTrip_shouldBeConsistent() {
    StripeProperties props = props("sk_test");
    for (String tier : new String[] {"starter", "pro", "enterprise"}) {
      assertEquals(tier, props.tierForPrice(props.priceForTier(tier)));
    }
  }

  @Test
  @DisplayName("isConfigured reflete a presença da chave secreta")
  void isConfigured_shouldReflectKey() {
    assertTrue(props("sk_test").isConfigured());
    assertFalse(props("").isConfigured());
    assertFalse(props(null).isConfigured());
  }
}
