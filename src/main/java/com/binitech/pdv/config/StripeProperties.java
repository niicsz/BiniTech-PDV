package com.binitech.pdv.config;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeProperties {

  private final String secretKey;
  private final String webhookSecret;
  private final String priceStarter;
  private final String pricePro;
  private final String priceEnterprise;

  public StripeProperties(
      @Value("${stripe.secret-key:}") String secretKey,
      @Value("${stripe.webhook-secret:}") String webhookSecret,
      @Value("${stripe.price-starter:}") String priceStarter,
      @Value("${stripe.price-pro:}") String pricePro,
      @Value("${stripe.price-enterprise:}") String priceEnterprise) {
    this.secretKey = secretKey;
    this.webhookSecret = webhookSecret;
    this.priceStarter = priceStarter;
    this.pricePro = pricePro;
    this.priceEnterprise = priceEnterprise;
  }

  public boolean isConfigured() {
    return secretKey != null && !secretKey.isBlank();
  }

  public String getSecretKey() {
    return secretKey;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public String priceForTier(String tier) {
    if (tier == null) {
      return null;
    }
    return switch (tier.toLowerCase(Locale.ROOT).trim()) {
      case "starter" -> priceStarter;
      case "pro" -> pricePro;
      case "enterprise" -> priceEnterprise;
      default -> null;
    };
  }

  public String tierForPrice(String priceId) {
    if (priceId == null || priceId.isBlank()) {
      return null;
    }
    if (priceId.equals(priceStarter)) {
      return "starter";
    }
    if (priceId.equals(pricePro)) {
      return "pro";
    }
    if (priceId.equals(priceEnterprise)) {
      return "enterprise";
    }
    return null;
  }
}
