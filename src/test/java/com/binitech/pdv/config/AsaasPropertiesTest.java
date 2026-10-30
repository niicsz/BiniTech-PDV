package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AsaasPropertiesTest {

  private static final String TOKEN = "token-seguro-com-mais-de-32-caracteres";

  @Test
  void isConfigured_requiresApiKey() {
    assertFalse(new AsaasProperties("", TOKEN, "https://api.asaas.com/v3").isConfigured());
    assertTrue(new AsaasProperties("$aact_x", TOKEN, "https://api.asaas.com/v3").isConfigured());
  }

  @Test
  void webhookToken_mustMatchConfiguredValue() {
    AsaasProperties properties = new AsaasProperties("$aact_x", TOKEN, "https://api.asaas.com/v3");

    assertTrue(properties.isValidWebhookToken(TOKEN));
    assertFalse(properties.isValidWebhookToken("outro"));
    assertFalse(properties.isValidWebhookToken(null));
  }

  @Test
  void webhookToken_withoutConfiguredValue_rejectsEverything() {
    AsaasProperties properties = new AsaasProperties("$aact_x", "", "https://api.asaas.com/v3");

    assertFalse(properties.isValidWebhookToken(""));
    assertFalse(properties.isValidWebhookToken(TOKEN));
  }

  @Test
  void checkoutUrl_followsEnvironment() {
    assertEquals(
        "https://asaas.com/checkoutSession/show?id=abc",
        new AsaasProperties("k", TOKEN, "https://api.asaas.com/v3").checkoutUrl("abc"));
    assertEquals(
        "https://sandbox.asaas.com/checkoutSession/show?id=abc",
        new AsaasProperties("k", TOKEN, "https://api-sandbox.asaas.com/v3").checkoutUrl("abc"));
  }
}
