package com.binitech.pdv.config;

import com.binitech.pdv.domain.Tenant;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AsaasGateway {

  private static final Logger log = LoggerFactory.getLogger(AsaasGateway.class);
  private static final DateTimeFormatter DUE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final int CHECKOUT_MINUTES_TO_EXPIRE = 60;

  private final AsaasProperties properties;
  private final RestClient client;
  private final ZoneId businessZone;

  public AsaasGateway(
      AsaasProperties properties,
      RestClient.Builder restClientBuilder,
      @Value("${app.business-time-zone:America/Sao_Paulo}") String businessTimeZone) {
    this.properties = properties;
    this.businessZone = ZoneId.of(businessTimeZone);
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(15));
    this.client =
        restClientBuilder
            .baseUrl(properties.getBaseUrl())
            .defaultHeader("access_token", properties.getApiKey())
            .defaultHeader(HttpHeaders.USER_AGENT, "BiniTech-PDV/1.0")
            .requestFactory(factory)
            .build();
    if (!properties.isConfigured()) {
      log.warn(
          "ASAAS_API_KEY ausente; checkout/faturas online desabilitados "
              + "(use a ativação manual no painel).");
    }
  }

  public boolean isConfigured() {
    return properties.isConfigured();
  }

  public boolean isValidWebhookToken(String token) {
    return properties.isValidWebhookToken(token);
  }

  public CheckoutSession createSubscriptionCheckout(
      Tenant tenant, String planName, BigDecimal value, String successUrl, String cancelUrl) {
    String firstDueDate = LocalDateTime.now(businessZone).plusMinutes(5).format(DUE_DATE_FORMAT);
    return createCheckout(
        new CheckoutRequest(
            List.of("CREDIT_CARD"),
            List.of("RECURRENT"),
            CHECKOUT_MINUTES_TO_EXPIRE,
            tenant.getId(),
            new Callback(successUrl, cancelUrl, cancelUrl),
            List.of(
                new Item(
                    "BiniTech PDV " + planName,
                    "Assinatura mensal do plano " + planName + " — " + tenant.getName(),
                    1,
                    value)),
            new SubscriptionRule("MONTHLY", firstDueDate)));
  }

  public CheckoutSession createPixCheckout(
      Tenant tenant, String planName, BigDecimal value, String successUrl, String cancelUrl) {
    return createCheckout(
        new CheckoutRequest(
            List.of("PIX"),
            List.of("DETACHED"),
            CHECKOUT_MINUTES_TO_EXPIRE,
            tenant.getId(),
            new Callback(successUrl, cancelUrl, cancelUrl),
            List.of(
                new Item(
                    "BiniTech PDV " + planName,
                    "Mensalidade do plano " + planName + " (Pix) — " + tenant.getName(),
                    1,
                    value)),
            null));
  }

  private CheckoutSession createCheckout(CheckoutRequest request) {
    try {
      CheckoutResponse response =
          client
              .post()
              .uri("/checkouts")
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .body(CheckoutResponse.class);
      if (response == null || response.id() == null) {
        throw new AsaasException("Asaas não retornou o id do checkout", null);
      }
      String url =
          response.link() != null && !response.link().isBlank()
              ? response.link()
              : properties.checkoutUrl(response.id());
      return new CheckoutSession(response.id(), url);
    } catch (RestClientResponseException e) {
      throw new AsaasException(
          "Asaas recusou o checkout: HTTP "
              + e.getStatusCode().value()
              + " "
              + truncate(e.getResponseBodyAsString()),
          e);
    } catch (RestClientException e) {
      throw new AsaasException("Falha ao criar checkout no Asaas", e);
    }
  }

  private String truncate(String value) {
    if (value == null) {
      return "";
    }
    return value.length() > 500 ? value.substring(0, 500) : value;
  }

  public String findActiveSubscriptionId(String customerId) {
    try {
      Page<IdOnly> page =
          client
              .get()
              .uri(
                  uri ->
                      uri.path("/subscriptions")
                          .queryParam("customer", customerId)
                          .queryParam("status", "ACTIVE")
                          .queryParam("limit", 1)
                          .build())
              .retrieve()
              .body(new ParameterizedTypeReference<Page<IdOnly>>() {});
      return firstOf(page).map(IdOnly::id).orElse(null);
    } catch (RestClientException e) {
      throw new AsaasException("Falha ao consultar assinaturas no Asaas", e);
    }
  }

  public Optional<String> findOpenPaymentUrl(String subscriptionId) {
    for (String status : List.of("OVERDUE", "PENDING")) {
      Optional<String> url = findPaymentUrl(subscriptionId, status);
      if (url.isPresent()) {
        return url;
      }
    }
    return Optional.empty();
  }

  private Optional<String> findPaymentUrl(String subscriptionId, String status) {
    try {
      Page<PaymentSummary> page =
          client
              .get()
              .uri(
                  uri ->
                      uri.path("/payments")
                          .queryParam("subscription", subscriptionId)
                          .queryParam("status", status)
                          .queryParam("limit", 1)
                          .build())
              .retrieve()
              .body(new ParameterizedTypeReference<Page<PaymentSummary>>() {});
      return firstOf(page).map(PaymentSummary::invoiceUrl);
    } catch (RestClientException e) {
      throw new AsaasException("Falha ao consultar cobranças no Asaas", e);
    }
  }

  private <T> Optional<T> firstOf(Page<T> page) {
    if (page == null || page.data() == null || page.data().isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(page.data().get(0));
  }

  public record CheckoutSession(String id, String url) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record CheckoutRequest(
      List<String> billingTypes,
      List<String> chargeTypes,
      int minutesToExpire,
      String externalReference,
      Callback callback,
      List<Item> items,
      SubscriptionRule subscription) {}

  record Callback(String successUrl, String cancelUrl, String expiredUrl) {}

  record Item(String name, String description, int quantity, BigDecimal value) {}

  record SubscriptionRule(String cycle, String nextDueDate) {}

  record CheckoutResponse(String id, String link) {}

  record Page<T>(List<T> data) {}

  record IdOnly(String id) {}

  record PaymentSummary(String id, String invoiceUrl) {}
}
