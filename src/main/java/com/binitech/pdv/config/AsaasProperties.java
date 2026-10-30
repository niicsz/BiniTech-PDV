package com.binitech.pdv.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AsaasProperties {

  private static final String PRODUCTION_CHECKOUT_URL = "https://asaas.com/checkoutSession/show";
  private static final String SANDBOX_CHECKOUT_URL =
      "https://sandbox.asaas.com/checkoutSession/show";

  private final String apiKey;
  private final String webhookToken;
  private final String baseUrl;

  public AsaasProperties(
      @Value("${asaas.api-key:}") String apiKey,
      @Value("${asaas.webhook-token:}") String webhookToken,
      @Value("${asaas.base-url:https://api.asaas.com/v3}") String baseUrl) {
    this.apiKey = apiKey;
    this.webhookToken = webhookToken;
    this.baseUrl = baseUrl;
  }

  public boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String checkoutUrl(String checkoutId) {
    String base = isSandbox() ? SANDBOX_CHECKOUT_URL : PRODUCTION_CHECKOUT_URL;
    return base + "?id=" + checkoutId;
  }

  public boolean isValidWebhookToken(String token) {
    if (webhookToken == null || webhookToken.isBlank() || token == null) {
      return false;
    }
    return MessageDigest.isEqual(
        webhookToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
  }

  private boolean isSandbox() {
    return baseUrl != null && baseUrl.contains("sandbox");
  }
}
