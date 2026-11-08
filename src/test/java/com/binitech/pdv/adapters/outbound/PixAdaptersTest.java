package com.binitech.pdv.adapters.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.binitech.pdv.application.ports.outbound.PixGatewayPort;
import com.binitech.pdv.domain.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PixAdaptersTest {

  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

  @Test
  @DisplayName("Credencial criptografada volta igual e não fica em texto puro")
  void cipherRoundTrip() {
    SecretCipher cipher = new SecretCipher(KEY);

    String stored = cipher.encrypt("APP_USR-123");

    assertThat(stored).startsWith("v1:").doesNotContain("APP_USR");
    assertThat(cipher.encrypt("APP_USR-123")).isNotEqualTo(stored);
    assertThat(cipher.decrypt(stored)).isEqualTo("APP_USR-123");
    assertThat(cipher.encrypt(null)).isNull();
  }

  @Test
  @DisplayName("Sem chave configurada não guarda credencial")
  void cipherWithoutKey() {
    SecretCipher cipher = new SecretCipher("");

    assertThat(cipher.isConfigured()).isFalse();
    assertThatThrownBy(() -> cipher.encrypt("x")).isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("Credencial adulterada não é aceita")
  void cipherRejectsTampering() {
    SecretCipher cipher = new SecretCipher(KEY);
    String stored = cipher.encrypt("APP_USR-123");
    String tampered = stored.substring(0, stored.length() - 4) + "AAAA";

    assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("Valida a assinatura x-signature do Mercado Pago")
  void validatesSignature() throws Exception {
    MercadoPagoPixGateway gateway =
        new MercadoPagoPixGateway(RestClient.builder(), "http://localhost", "c@x.com");
    String manifest = "id:998877;request-id:req-1;ts:1704908010;";
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec("segredo".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String v1 = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

    assertThat(gateway.isValidSignature("segredo", "998877", "req-1", "ts=1704908010,v1=" + v1))
        .isTrue();
    assertThat(gateway.isValidSignature("outro", "998877", "req-1", "ts=1704908010,v1=" + v1))
        .isFalse();
    assertThat(gateway.isValidSignature("segredo", "111", "req-1", "ts=1704908010,v1=" + v1))
        .isFalse();
    assertThat(gateway.isValidSignature("segredo", "998877", "req-1", null)).isFalse();
    assertThat(gateway.isValidSignature("segredo", "998877", "req-1", "ts=1,v1=zz")).isFalse();
  }

  @Test
  @DisplayName("Chama as rotas certas do Mercado Pago com o token da loja")
  void callsMercadoPagoRoutes() throws Exception {
    List<String> calls = new CopyOnWriteArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          calls.add(
              exchange.getRequestMethod()
                  + " "
                  + exchange.getRequestURI().getPath()
                  + " "
                  + exchange.getRequestHeaders().getFirst("Authorization"));
          String path = exchange.getRequestURI().getPath();
          String body =
              switch (path) {
                case "/users/me" -> "{\"nickname\":\"LOJA\"}";
                case "/v1/payments" ->
                    "{\"id\":123,\"status\":\"pending\",\"point_of_interaction\":{\"transaction_data\":{\"qr_code\":\"000201\",\"qr_code_base64\":\"b64\"}}}";
                default -> "{\"id\":123,\"status\":\"approved\"}";
              };
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    try {
      MercadoPagoPixGateway gateway =
          new MercadoPagoPixGateway(
              RestClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort(), "c@x.com");

      assertThat(gateway.verifyAccount("tok")).isEqualTo("LOJA");
      PixGatewayPort.CreatedCharge created =
          gateway.createCharge(
              "tok",
              new PixGatewayPort.ChargeRequest(
                  "k1", new BigDecimal("12.5"), "Compra", "c1", OffsetDateTime.now(), null));
      assertThat(created.providerPaymentId()).isEqualTo("123");
      assertThat(created.qrCode()).isEqualTo("000201");
      assertThat(gateway.fetchState("tok", "123")).isEqualTo(PixGatewayPort.ChargeState.APPROVED);
      gateway.cancel("tok", "123");

      assertThat(calls)
          .containsExactly(
              "GET /users/me Bearer tok",
              "POST /v1/payments Bearer tok",
              "GET /v1/payments/123 Bearer tok",
              "PUT /v1/payments/123 Bearer tok");
    } finally {
      server.stop(0);
    }
  }
}
