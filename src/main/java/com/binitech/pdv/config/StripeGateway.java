package com.binitech.pdv.config;

import com.binitech.pdv.domain.Tenant;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StripeGateway {

  private static final Logger log = LoggerFactory.getLogger(StripeGateway.class);

  private final StripeProperties properties;

  public StripeGateway(StripeProperties properties) {
    this.properties = properties;
    if (properties.isConfigured()) {
      Stripe.apiKey = properties.getSecretKey();
    } else {
      log.warn(
          "STRIPE_SECRET_KEY ausente; checkout/portal desabilitados "
              + "(use a ativação manual no painel).");
    }
  }

  public boolean isConfigured() {
    return properties.isConfigured();
  }

  public String createSubscriptionCheckoutSession(
      Tenant tenant, String priceId, String successUrl, String cancelUrl) throws StripeException {
    com.stripe.param.checkout.SessionCreateParams params =
        com.stripe.param.checkout.SessionCreateParams.builder()
            .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION)
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl)
            .setClientReferenceId(tenant.getId())
            .setCustomerEmail(tenant.getBillingEmail())
            .addLineItem(
                com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                    .setPrice(priceId)
                    .setQuantity(1L)
                    .build())
            .setSubscriptionData(
                com.stripe.param.checkout.SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("tenantId", tenant.getId())
                    .build())
            .build();
    return com.stripe.model.checkout.Session.create(params).getUrl();
  }

  public String createBillingPortalSession(String customerId, String returnUrl)
      throws StripeException {
    com.stripe.param.billingportal.SessionCreateParams params =
        com.stripe.param.billingportal.SessionCreateParams.builder()
            .setCustomer(customerId)
            .setReturnUrl(returnUrl)
            .build();
    return com.stripe.model.billingportal.Session.create(params).getUrl();
  }

  public Event constructEvent(String payload, String signatureHeader) throws StripeException {
    return Webhook.constructEvent(payload, signatureHeader, properties.getWebhookSecret());
  }

  public String getPriceIdForSubscription(String stripeSubscriptionId) throws StripeException {
    com.stripe.model.Subscription subscription =
        com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
    if (subscription.getItems() == null
        || subscription.getItems().getData() == null
        || subscription.getItems().getData().isEmpty()) {
      return null;
    }
    com.stripe.model.Price price = subscription.getItems().getData().get(0).getPrice();
    return price != null ? price.getId() : null;
  }
}
