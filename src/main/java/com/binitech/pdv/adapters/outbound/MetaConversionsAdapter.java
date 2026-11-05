package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.AdConversionsPort;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MetaConversionsAdapter implements AdConversionsPort {

  private static final Logger log = LoggerFactory.getLogger(MetaConversionsAdapter.class);

  private final String pixelId;
  private final String token;
  private final RestClient client;
  private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

  public MetaConversionsAdapter(
      @Value("${meta.pixel-id:}") String pixelId,
      @Value("${meta.capi-token:}") String token,
      @Value("${meta.graph-url:https://graph.facebook.com/v21.0}") String graphUrl) {
    this.pixelId = pixelId == null ? "" : pixelId.trim();
    this.token = token == null ? "" : token.trim();
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    this.client = RestClient.builder().baseUrl(graphUrl).requestFactory(factory).build();
  }

  boolean enabled() {
    return pixelId.matches("\\d{5,20}") && !token.isEmpty();
  }

  @Override
  public void sendSignup(SignupConversion conversion) {
    if (!enabled() || conversion == null) {
      return;
    }
    executor.execute(() -> send(conversion));
  }

  private void send(SignupConversion c) {
    try {
      Map<String, Object> userData = userData(c);
      long now = Instant.now().getEpochSecond();
      List<Map<String, Object>> events =
          List.of(
              event("CompleteRegistration", c.eventId(), now, c, userData, null),
              event("StartTrial", c.eventId() + "-trial", now, c, userData, c.monthlyValue()));
      client
          .post()
          .uri("/{pixel}/events?access_token={token}", pixelId, token)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("data", events))
          .retrieve()
          .toBodilessEntity();
      log.info("Conversões de cadastro enviadas à Meta: eventId={}", c.eventId());
    } catch (Exception e) {
      log.warn("Falha ao enviar conversões à Meta: {}", e.getMessage());
    }
  }

  private static Map<String, Object> event(
      String name,
      String eventId,
      long time,
      SignupConversion c,
      Map<String, Object> userData,
      BigDecimal value) {
    Map<String, Object> event = new LinkedHashMap<>();
    event.put("event_name", name);
    event.put("event_time", time);
    event.put("event_id", eventId);
    event.put("action_source", "website");
    if (c.sourceUrl() != null) {
      event.put("event_source_url", c.sourceUrl());
    }
    event.put("user_data", userData);
    if (value != null) {
      event.put(
          "custom_data",
          Map.of(
              "currency", "BRL", "value", value, "predicted_ltv", value.multiply(BigDecimal.TEN)));
    }
    return event;
  }

  private static Map<String, Object> userData(SignupConversion c) {
    Map<String, Object> data = new LinkedHashMap<>();
    if (c.email() != null) {
      data.put("em", List.of(sha256(c.email().trim().toLowerCase(Locale.ROOT))));
    }
    if (c.phone() != null) {
      String digits = c.phone().replaceAll("\\D", "");
      if (!digits.isEmpty()) {
        data.put("ph", List.of(sha256(digits.startsWith("55") ? digits : "55" + digits)));
      }
    }
    data.put("country", List.of(sha256("br")));
    putIfPresent(data, "client_ip_address", c.ip());
    putIfPresent(data, "client_user_agent", c.userAgent());
    putIfPresent(data, "fbp", c.fbp());
    putIfPresent(data, "fbc", c.fbc());
    return data;
  }

  private static void putIfPresent(Map<String, Object> data, String key, String value) {
    if (value != null && !value.isBlank()) {
      data.put(key, value);
    }
  }

  static String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 indisponível", e);
    }
  }
}
