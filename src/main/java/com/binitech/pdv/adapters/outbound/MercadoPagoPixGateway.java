package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.PixGatewayPort;
import com.binitech.pdv.domain.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MercadoPagoPixGateway implements PixGatewayPort {

  private static final Logger log = LoggerFactory.getLogger(MercadoPagoPixGateway.class);
  private static final DateTimeFormatter EXPIRATION_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  private static final String BEARER = "Bearer ";
  private static final String PAYMENT_PATH = "/v1/payments/{id}";

  private final RestClient client;
  private final String payerEmail;

  public MercadoPagoPixGateway(
      RestClient.Builder builder,
      @Value("${pix.mercadopago.base-url:https://api.mercadopago.com}") String baseUrl,
      @Value("${pix.mercadopago.payer-email:consumidor@binitechpdv.com.br}") String payerEmail) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(5));
    factory.setReadTimeout(Duration.ofSeconds(15));
    this.client =
        builder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.USER_AGENT, "BiniTech-PDV/1.0")
            .requestFactory(factory)
            .build();
    this.payerEmail = payerEmail;
  }

  @Override
  public String verifyAccount(String accessToken) {
    try {
      Account account =
          client
              .get()
              .uri("/users/me")
              .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
              .retrieve()
              .body(Account.class);
      if (account == null) {
        throw new BusinessException("O Mercado Pago não respondeu. Tente de novo.");
      }
      return firstNonBlank(account.nickname(), account.email(), "Conta Mercado Pago");
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
        throw new BusinessException(
            "O Mercado Pago recusou o Access Token. Copie de novo o token de produção da sua conta.");
      }
      throw unavailable(e);
    } catch (RestClientException e) {
      throw unavailable(e);
    }
  }

  @Override
  public CreatedCharge createCharge(String accessToken, ChargeRequest request) {
    PaymentRequest body =
        new PaymentRequest(
            request.amount().setScale(2, RoundingMode.HALF_UP),
            request.description(),
            "pix",
            new Payer(payerEmail),
            request.externalReference(),
            request.expiresAt().format(EXPIRATION_FORMAT),
            request.notificationUrl());
    try {
      PaymentResponse response =
          client
              .post()
              .uri("/v1/payments")
              .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
              .header("X-Idempotency-Key", request.idempotencyKey())
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(PaymentResponse.class);
      if (response == null
          || response.id() == null
          || response.pointOfInteraction() == null
          || response.pointOfInteraction().transactionData() == null
          || response.pointOfInteraction().transactionData().qrCode() == null) {
        throw new BusinessException("O Mercado Pago não devolveu o QR do Pix. Tente de novo.");
      }
      TransactionData data = response.pointOfInteraction().transactionData();
      return new CreatedCharge(String.valueOf(response.id()), data.qrCode(), data.qrCodeBase64());
    } catch (RestClientResponseException e) {
      if (log.isWarnEnabled()) {
        log.warn("Mercado Pago recusou a cobrança Pix: status={}", e.getStatusCode().value());
      }
      if (isAuthError(e.getStatusCode())) {
        throw new BusinessException(
            "O Access Token do Mercado Pago não vale mais. Atualize nas configurações do Pix.");
      }
      throw new BusinessException(
          "O Mercado Pago recusou a cobrança Pix. Confira se a conta tem uma chave Pix cadastrada.");
    } catch (RestClientException e) {
      throw unavailable(e);
    }
  }

  @Override
  public ChargeState fetchState(String accessToken, String providerPaymentId) {
    PaymentResponse response =
        client
            .get()
            .uri(PAYMENT_PATH, providerPaymentId)
            .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
            .retrieve()
            .body(PaymentResponse.class);
    if (response == null || response.status() == null) {
      return ChargeState.UNKNOWN;
    }
    return switch (response.status().toLowerCase(Locale.ROOT)) {
      case "approved", "authorized" -> ChargeState.APPROVED;
      case "pending", "in_process" -> ChargeState.PENDING;
      case "cancelled", "refunded", "charged_back" -> ChargeState.CANCELLED;
      case "rejected" -> ChargeState.REJECTED;
      default -> ChargeState.UNKNOWN;
    };
  }

  @Override
  public void cancel(String accessToken, String providerPaymentId) {
    client
        .put()
        .uri(PAYMENT_PATH, providerPaymentId)
        .header(HttpHeaders.AUTHORIZATION, BEARER + accessToken)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new StatusUpdate("cancelled"))
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  public boolean isValidSignature(
      String webhookSecret, String dataId, String requestId, String signatureHeader) {
    if (signatureHeader == null || dataId == null) {
      return false;
    }
    String ts = null;
    String v1 = null;
    for (String part : signatureHeader.split(",")) {
      String[] pair = part.trim().split("=", 2);
      if (pair.length != 2) {
        continue;
      }
      if ("ts".equals(pair[0])) {
        ts = pair[1];
      } else if ("v1".equals(pair[0])) {
        v1 = pair[1];
      }
    }
    if (ts == null || v1 == null) {
      return false;
    }
    StringBuilder manifest =
        new StringBuilder("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
    if (requestId != null && !requestId.isBlank()) {
      manifest.append("request-id:").append(requestId).append(';');
    }
    manifest.append("ts:").append(ts).append(';');
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] expected = mac.doFinal(manifest.toString().getBytes(StandardCharsets.UTF_8));
      byte[] received = HexFormat.of().parseHex(v1);
      return MessageDigest.isEqual(expected, received);
    } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean isAuthError(HttpStatusCode status) {
    return status.value() == 401 || status.value() == 403;
  }

  private static BusinessException unavailable(RuntimeException e) {
    if (log.isWarnEnabled()) {
      log.warn("Mercado Pago indisponível: {}", e.getMessage());
    }
    return new BusinessException("Não foi possível falar com o Mercado Pago. Tente de novo.");
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record PaymentRequest(
      @JsonProperty("transaction_amount") BigDecimal transactionAmount,
      String description,
      @JsonProperty("payment_method_id") String paymentMethodId,
      Payer payer,
      @JsonProperty("external_reference") String externalReference,
      @JsonProperty("date_of_expiration") String dateOfExpiration,
      @JsonProperty("notification_url") String notificationUrl) {}

  record Payer(String email) {}

  record StatusUpdate(String status) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record PaymentResponse(
      Long id,
      String status,
      @JsonProperty("point_of_interaction") PointOfInteraction pointOfInteraction) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record PointOfInteraction(@JsonProperty("transaction_data") TransactionData transactionData) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TransactionData(
      @JsonProperty("qr_code") String qrCode,
      @JsonProperty("qr_code_base64") String qrCodeBase64) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Account(String nickname, String email) {}
}
