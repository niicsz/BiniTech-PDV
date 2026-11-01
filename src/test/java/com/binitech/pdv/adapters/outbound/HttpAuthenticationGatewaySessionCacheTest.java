package com.binitech.pdv.adapters.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.binitech.pdv.application.ports.outbound.AuthenticationGateway.SessionIdentity;
import com.binitech.pdv.domain.exception.AuthenticationUnavailableException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpAuthenticationGatewaySessionCacheTest {

  private static final String SESSION_URL = "http://auth:8081/api/auth/session";
  private static final String SESSION_JSON =
      "{\"userId\":\"u1\",\"username\":\"caixa\",\"role\":null,\"tenantId\":\"t1\"}";

  private RestClient.Builder builder;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    builder = RestClient.builder().baseUrl("http://auth:8081");
    server = MockRestServiceServer.bindTo(builder).build();
  }

  private HttpAuthenticationGateway gateway(Duration ttl) {
    return new HttpAuthenticationGateway(builder.build(), ttl);
  }

  @Test
  @DisplayName("Valida o token no Auth uma vez e reaproveita a sessão dentro do prazo")
  void cachesSession() {
    HttpAuthenticationGateway gateway = gateway(Duration.ofSeconds(30));
    server
        .expect(once(), requestTo(SESSION_URL))
        .andExpect(header("Authorization", "Bearer token-1"))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));

    SessionIdentity first = gateway.session("token-1");
    SessionIdentity second = gateway.session("token-1");

    assertEquals("u1", first.userId());
    assertEquals(first, second);
    server.verify();
  }

  @Test
  @DisplayName("Logout derruba a sessão do cache e a próxima chamada volta ao Auth")
  void logoutInvalidates() {
    HttpAuthenticationGateway gateway = gateway(Duration.ofSeconds(30));
    server
        .expect(once(), requestTo(SESSION_URL))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));
    server
        .expect(once(), requestTo("http://auth:8081/api/auth/logout"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));
    server
        .expect(once(), requestTo(SESSION_URL))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));

    gateway.session("token-1");
    gateway.logout("token-1");
    gateway.session("token-1");

    server.verify();
  }

  @Test
  @DisplayName("Revogar sessões do usuário ou apagar a loja limpa o cache dele")
  void revokeAndPurgeInvalidate() {
    HttpAuthenticationGateway gateway = gateway(Duration.ofSeconds(30));
    server
        .expect(once(), requestTo(SESSION_URL))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));
    server
        .expect(once(), requestTo("http://auth:8081/api/internal/identities/revoke"))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));
    server
        .expect(once(), requestTo(SESSION_URL))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));
    server
        .expect(once(), requestTo("http://auth:8081/api/internal/identities/purge-tenant"))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));
    server
        .expect(once(), requestTo(SESSION_URL))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));

    gateway.session("token-1");
    gateway.revokeSessions("u1");
    gateway.session("token-1");
    gateway.purgeTenant("t1");
    gateway.session("token-1");

    server.verify();
  }

  @Test
  @DisplayName("Falha do Auth não é guardada no cache; sem prazo configurado não há cache")
  void failuresAndDisabledCache() {
    HttpAuthenticationGateway cached = gateway(Duration.ofSeconds(30));
    server
        .expect(once(), requestTo(SESSION_URL))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    server
        .expect(once(), requestTo(SESSION_URL))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));

    assertThrows(AuthenticationUnavailableException.class, () -> cached.session("token-1"));
    assertEquals("u1", cached.session("token-1").userId());
    server.verify();

    server.reset();
    HttpAuthenticationGateway uncached = gateway(Duration.ZERO);
    server
        .expect(times(2), requestTo(SESSION_URL))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));
    uncached.session("token-1");
    uncached.session("token-1");
    server.verify();
  }
}
