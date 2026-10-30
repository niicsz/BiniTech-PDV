package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.AuthResult;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.LoginOutcome;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.TwoFactorChallenge;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.TwoFactorSetup;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.TwoFactorStatus;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.domain.exception.AuthenticationUnavailableException;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.TwoFactorException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class HttpAuthenticationGateway implements AuthenticationGateway {
  private final RestClient client;

  public HttpAuthenticationGateway(RestClient client) {
    this.client = client;
  }

  @Override
  public LoginOutcome login(String username, String password, String tenantId) {
    LoginResponse response =
        invoke(
            () ->
                client
                    .post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LoginRequest(username, password, tenantId))
                    .retrieve()
                    .body(LoginResponse.class));
    if (Boolean.TRUE.equals(response.twoFactorRequired())) {
      if (response.challengeToken() == null || response.expiresAt() == null) {
        throw new AuthenticationUnavailableException();
      }
      return new TwoFactorChallenge(response.challengeToken(), instant(response.expiresAt()));
    }
    return new AuthResult(
        response.accessToken(),
        response.refreshToken(),
        response.username(),
        response.role(),
        response.tenantId());
  }

  @Override
  public AuthResult completeTwoFactorLogin(String challengeToken, String code) {
    return invoke(
        () ->
            client
                .post()
                .uri("/api/auth/login/two-factor")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("challengeToken", challengeToken, "code", code))
                .retrieve()
                .body(AuthResult.class));
  }

  @Override
  public TwoFactorStatus twoFactorStatus(String accessToken) {
    StatusResponse response =
        invoke(
            () ->
                client
                    .get()
                    .uri("/api/auth/two-factor")
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .body(StatusResponse.class));
    return new TwoFactorStatus(
        response.enabled(), instant(response.lockedUntil()), response.recoveryCodesRemaining());
  }

  @Override
  public TwoFactorSetup setupTwoFactor(String accessToken) {
    return invoke(
        () ->
            client
                .post()
                .uri("/api/auth/two-factor/setup")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(TwoFactorSetup.class));
  }

  @Override
  public List<String> enableTwoFactor(String accessToken, String code) {
    return recoveryCodes("/api/auth/two-factor/enable", accessToken, code);
  }

  @Override
  public List<String> regenerateRecoveryCodes(String accessToken, String code) {
    return recoveryCodes("/api/auth/two-factor/recovery-codes", accessToken, code);
  }

  private List<String> recoveryCodes(String path, String accessToken, String code) {
    RecoveryCodesResponse response =
        invoke(
            () ->
                client
                    .post()
                    .uri(path)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("code", code))
                    .retrieve()
                    .body(RecoveryCodesResponse.class));
    return response.recoveryCodes() == null ? List.of() : response.recoveryCodes();
  }

  @Override
  public void disableTwoFactor(String accessToken, String code) {
    twoFactorCode("/api/auth/two-factor/disable", accessToken, code);
  }

  private void twoFactorCode(String path, String accessToken, String code) {
    invoke(
        () ->
            client
                .post()
                .uri(path)
                .headers(h -> h.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("code", code))
                .retrieve()
                .toBodilessEntity());
  }

  private static Instant instant(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (java.time.format.DateTimeParseException exception) {
      throw new AuthenticationUnavailableException();
    }
  }

  @Override
  public AuthResult refresh(String refreshToken) {
    return invoke(
        () ->
            client
                .post()
                .uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("refreshToken", refreshToken))
                .retrieve()
                .body(AuthResult.class));
  }

  @Override
  public void logout(String accessToken) {
    invoke(
        () ->
            client
                .post()
                .uri("/api/auth/logout")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .toBodilessEntity());
  }

  @Override
  public SessionIdentity session(String accessToken) {
    return invoke(
        () ->
            client
                .get()
                .uri("/api/auth/session")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(SessionIdentity.class));
  }

  @Override
  public void provision(
      String id, String username, String password, String tenantId, String recoveryEmail) {
    post("/provision", new Provision(id, username, password, tenantId, recoveryEmail));
  }

  @Override
  public void changePassword(String id, String currentPassword, String newPassword) {
    post(
        "/change-password",
        Map.of("identityId", id, "currentPassword", currentPassword, "newPassword", newPassword));
  }

  @Override
  public void revokeSessions(String id) {
    post("/revoke", Map.of("identityId", id));
  }

  @Override
  public boolean resetTwoFactor(String identityId) {
    TwoFactorResetResponse response =
        invoke(
            () ->
                client
                    .post()
                    .uri("/api/internal/identities/two-factor/reset")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("identityId", identityId))
                    .retrieve()
                    .body(TwoFactorResetResponse.class));
    return response.reset();
  }

  @Override
  public void purgeTenant(String tenantId) {
    post("/purge-tenant", Map.of("tenantId", tenantId));
  }

  @Override
  public java.util.Optional<RecoveryDelivery> requestRecovery(String username) {
    var response =
        invoke(
            () ->
                client
                    .post()
                    .uri("/api/internal/identities/recovery")
                    .body(Map.of("username", username))
                    .retrieve()
                    .toEntity(RecoveryDelivery.class));
    return java.util.Optional.ofNullable(response.getBody());
  }

  @Override
  public void resetPassword(String token, String newPassword) {
    post("/reset-password", Map.of("token", token, "newPassword", newPassword));
  }

  private void post(String path, Object body) {
    invoke(
        () ->
            client
                .post()
                .uri("/api/internal/identities" + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity());
  }

  private record Provision(
      String identityId, String username, String password, String tenantId, String recoveryEmail) {}

  private <T> T invoke(Supplier<T> operation) {
    try {
      T result = operation.get();
      if (result == null) {
        throw new AuthenticationUnavailableException();
      }
      return result;
    } catch (RestClientResponseException exception) {
      int status = exception.getStatusCode().value();
      if (status == 409 || status == 422 || status == 423) {
        throw twoFactorError(status, exception);
      }
      if (exception.getStatusCode().value() == 401) {
        throw new BusinessException("Credenciais ou sessão inválidas.");
      }
      if (exception.getStatusCode().value() == 400) {
        throw new BusinessException("Dados de autenticação inválidos.");
      }
      throw new AuthenticationUnavailableException();
    } catch (RestClientException exception) {
      throw new AuthenticationUnavailableException();
    }
  }

  private TwoFactorException twoFactorError(int status, RestClientResponseException exception) {
    AuthError body = null;
    try {
      body = exception.getResponseBodyAs(AuthError.class);
    } catch (RuntimeException ignored) {
      body = null;
    }
    String code =
        body != null && body.code() != null
            ? body.code()
            : switch (status) {
              case 423 -> "TWO_FACTOR_LOCKED";
              case 422 -> "INVALID_TWO_FACTOR_CODE";
              default -> "TWO_FACTOR_CONFLICT";
            };
    String message =
        body != null && body.message() != null
            ? body.message()
            : switch (status) {
              case 423 -> "Usuário bloqueado temporariamente por tentativas inválidas.";
              case 422 -> "Código de verificação inválido.";
              default -> "Estado do segundo fator não permite esta operação.";
            };
    Instant lockedUntil;
    try {
      lockedUntil = body == null ? null : instant(body.lockedUntil());
    } catch (AuthenticationUnavailableException invalidDate) {
      lockedUntil = null;
    }
    return new TwoFactorException(
        status, code, message, body == null ? null : body.remainingAttempts(), lockedUntil);
  }

  private record LoginRequest(String username, String password, String tenantId) {}

  private record LoginResponse(
      String accessToken,
      String refreshToken,
      String username,
      String role,
      String tenantId,
      Boolean twoFactorRequired,
      String challengeToken,
      String expiresAt) {}

  private record StatusResponse(boolean enabled, String lockedUntil, int recoveryCodesRemaining) {}

  private record RecoveryCodesResponse(List<String> recoveryCodes) {}

  private record TwoFactorResetResponse(boolean reset) {}

  private record AuthError(
      String code, String message, Integer remainingAttempts, String lockedUntil) {}
}
