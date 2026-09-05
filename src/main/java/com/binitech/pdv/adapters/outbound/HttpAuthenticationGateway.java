package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.AuthResult;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.domain.exception.AuthenticationUnavailableException;
import com.binitech.pdv.domain.exception.BusinessException;
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
  public AuthResult login(String username, String password, String tenantId) {
    return invoke(
        () ->
            client
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(username, password, tenantId))
                .retrieve()
                .body(AuthResult.class));
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

  private <T> T invoke(Supplier<T> operation) {
    try {
      T result = operation.get();
      if (result == null) {
        throw new AuthenticationUnavailableException();
      }
      return result;
    } catch (RestClientResponseException exception) {
      if (exception.getStatusCode().value() == 401) {
        // Preserve the PDV's existing error contract for login and refresh.
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

  private record LoginRequest(String username, String password, String tenantId) {}
}
