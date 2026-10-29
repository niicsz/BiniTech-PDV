package com.binitech.pdv.adapters.outbound;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.AuthResult;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.TwoFactorChallenge;
import com.binitech.pdv.domain.exception.AuthenticationUnavailableException;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.TwoFactorException;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpAuthenticationGatewayTest {
  private MockRestServiceServer server;
  private HttpAuthenticationGateway gateway;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://auth:8081");
    server = MockRestServiceServer.bindTo(builder).build();
    gateway = new HttpAuthenticationGateway(builder.build());
  }

  @Test
  void login_forwardsTenantAndPreservesContract() {
    server
        .expect(requestTo("http://auth:8081/api/auth/login"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    "{\"username\":\"admin\",\"password\":\"password\",\"tenantId\":\"tenant1\"}"))
        .andRespond(
            withSuccess(
                "{\"accessToken\":\"access\",\"refreshToken\":\"refresh\",\"username\":\"admin\",\"role\":\"ADMIN\",\"tenantId\":\"tenant1\"}",
                MediaType.APPLICATION_JSON));
    var result = assertInstanceOf(AuthResult.class, gateway.login("admin", "password", "tenant1"));
    assertEquals("access", result.accessToken());
    assertEquals("tenant1", result.tenantId());
    server.verify();
  }

  @Test
  void login_withTwoFactorReturnsChallengeAndCompletesWithCode() {
    server
        .expect(requestTo("http://auth:8081/api/auth/login"))
        .andRespond(
            withSuccess(
                "{\"twoFactorRequired\":true,\"challengeToken\":\"challenge\",\"expiresAt\":\"2026-10-29T20:05:00Z\"}",
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://auth:8081/api/auth/login/two-factor"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("{\"challengeToken\":\"challenge\",\"code\":\"123456\"}"))
        .andRespond(
            withSuccess(
                "{\"accessToken\":\"access\",\"refreshToken\":\"refresh\",\"username\":\"admin\",\"role\":\"ADMIN\",\"tenantId\":\"tenant1\"}",
                MediaType.APPLICATION_JSON));

    var challenge =
        assertInstanceOf(TwoFactorChallenge.class, gateway.login("admin", "password", null));
    assertEquals("challenge", challenge.challengeToken());
    assertEquals(Instant.parse("2026-10-29T20:05:00Z"), challenge.expiresAt());
    assertEquals("access", gateway.completeTwoFactorLogin("challenge", "123456").accessToken());
    server.verify();
  }

  @Test
  void twoFactorErrors_keepRemainingAttemptsAndLockInformation() {
    server
        .expect(requestTo("http://auth:8081/api/auth/login/two-factor"))
        .andRespond(
            withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    "{\"code\":\"INVALID_TWO_FACTOR_CODE\",\"message\":\"Código de verificação inválido. Restam 2 tentativas antes do bloqueio.\",\"remainingAttempts\":2}"));
    server
        .expect(requestTo("http://auth:8081/api/auth/login/two-factor"))
        .andRespond(
            withStatus(HttpStatus.LOCKED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    "{\"code\":\"TWO_FACTOR_LOCKED\",\"message\":\"Usuário bloqueado por 1 hora após 3 tentativas inválidas do código de verificação.\",\"remainingAttempts\":0,\"lockedUntil\":\"2026-10-29T21:00:00Z\"}"));

    var wrong =
        assertThrows(
            TwoFactorException.class, () -> gateway.completeTwoFactorLogin("challenge", "000000"));
    assertEquals(422, wrong.status());
    assertEquals(2, wrong.remainingAttempts());
    var locked =
        assertThrows(
            TwoFactorException.class, () -> gateway.completeTwoFactorLogin("challenge", "000000"));
    assertEquals(423, locked.status());
    assertEquals("TWO_FACTOR_LOCKED", locked.code());
    assertEquals(Instant.parse("2026-10-29T21:00:00Z"), locked.lockedUntil());
    server.verify();
  }

  @Test
  void twoFactorManagement_forwardsUserBearer() {
    server
        .expect(requestTo("http://auth:8081/api/auth/two-factor"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer access"))
        .andRespond(withSuccess("{\"enabled\":false}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://auth:8081/api/auth/two-factor/setup"))
        .andExpect(header("Authorization", "Bearer access"))
        .andRespond(
            withSuccess(
                "{\"secret\":\"c2VjcmV0\",\"otpauthUri\":\"otpauth://totp/x\",\"algorithm\":\"SHA512\",\"digits\":6,\"period\":30}",
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://auth:8081/api/auth/two-factor/enable"))
        .andExpect(header("Authorization", "Bearer access"))
        .andExpect(content().json("{\"code\":\"123456\"}"))
        .andRespond(withNoContent());

    assertFalse(gateway.twoFactorStatus("access").enabled());
    assertEquals("SHA512", gateway.setupTwoFactor("access").algorithm());
    gateway.enableTwoFactor("access", "123456");
    server.verify();
  }

  @Test
  void refreshAndLogout_useSharedService() {
    server
        .expect(requestTo("http://auth:8081/api/auth/refresh"))
        .andExpect(content().json("{\"refreshToken\":\"refresh\"}"))
        .andRespond(
            withSuccess(
                "{\"accessToken\":\"access\",\"refreshToken\":\"new-refresh\",\"username\":\"admin\",\"role\":\"ADMIN\"}",
                MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("http://auth:8081/api/auth/logout"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer access"))
        .andRespond(withNoContent());
    assertEquals("new-refresh", gateway.refresh("refresh").refreshToken());
    gateway.logout("access");
    server.verify();
  }

  @Test
  void invalidCredentials_preservePdvBusinessError() {
    server
        .expect(requestTo("http://auth:8081/api/auth/login"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
    assertThrows(BusinessException.class, () -> gateway.login("admin", "wrong", null));
    server.verify();
  }

  @Test
  void serverError_doesNotExposeRemoteBodyOrFallbackToLocalLogin() {
    server
        .expect(requestTo("http://auth:8081/api/auth/login"))
        .andRespond(withServerError().body("private backend details"));
    var error =
        assertThrows(
            AuthenticationUnavailableException.class,
            () -> gateway.login("admin", "password", null));
    assertFalse(error.getMessage().contains("private"));
    server.verify();
  }

  @Test
  void timeout_returnsUnavailable() {
    server
        .expect(requestTo("http://auth:8081/api/auth/refresh"))
        .andRespond(withException(new IOException("read timed out")));
    assertThrows(AuthenticationUnavailableException.class, () -> gateway.refresh("refresh"));
    server.verify();
  }

  @Test
  void emptyResponse_returnsUnavailable() {
    server.expect(requestTo("http://auth:8081/api/auth/login")).andRespond(withNoContent());
    assertThrows(
        AuthenticationUnavailableException.class, () -> gateway.login("admin", "password", null));
  }

  @Test
  void sessionValidatesBearerThroughAuth() {
    server
        .expect(requestTo("http://auth:8081/api/auth/session"))
        .andExpect(header("Authorization", "Bearer access"))
        .andRespond(
            withSuccess(
                "{\"userId\":\"user1\",\"username\":\"user\",\"tenantId\":\"tenant1\"}",
                MediaType.APPLICATION_JSON));
    assertEquals("user1", gateway.session("access").userId());
    server.verify();
  }

  @Test
  void provisionDelegatesCredentialsAndStableIdWithoutHashingLocally() {
    server
        .expect(requestTo("http://auth:8081/api/internal/identities/provision"))
        .andExpect(
            content()
                .json(
                    "{\"identityId\":\"user1\",\"username\":\"user\",\"password\":\"password\",\"tenantId\":\"tenant1\",\"recoveryEmail\":null}"))
        .andRespond(withNoContent());
    gateway.provision("user1", "user", "password", "tenant1", null);
    server.verify();
  }

  @Test
  void missingRecoveryCandidateIsNotAnInfrastructureError() {
    server
        .expect(requestTo("http://auth:8081/api/internal/identities/recovery"))
        .andRespond(withNoContent());
    assertTrue(gateway.requestRecovery("unknown").isEmpty());
    server.verify();
  }

  @Test
  void purgeTenant_callsInternalEndpoint() {
    server
        .expect(requestTo("http://auth:8081/api/internal/identities/purge-tenant"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("{\"tenantId\":\"tenant1\"}"))
        .andRespond(withNoContent());
    gateway.purgeTenant("tenant1");
    server.verify();
  }
}
