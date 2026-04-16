package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.AuthApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.AuthResponseDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.LoginRequestDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.RefreshTokenRequestDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.RegisterRequestDTO;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.utils.Enum.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthUseCasePort authUseCase;
  private final HttpServletRequest httpServletRequest;

  public AuthController(AuthUseCasePort authUseCase, HttpServletRequest httpServletRequest) {
    this.authUseCase = authUseCase;
    this.httpServletRequest = httpServletRequest;
  }

  @Override
  public ResponseEntity<AuthResponseDTO> login(LoginRequestDTO loginRequestDTO) {
    log.info("Requisição de login recebida para o usuário: {}", loginRequestDTO.getUsername());
    AuthUseCasePort.AuthResult result =
        authUseCase.login(loginRequestDTO.getUsername(), loginRequestDTO.getPassword());
    log.info("Login realizado com sucesso para o usuário: {}", result.username());
    return ResponseEntity.ok(toDto(result));
  }

  @Override
  public ResponseEntity<AuthResponseDTO> registerUser(RegisterRequestDTO registerRequestDTO) {
    log.info(
        "Requisição de registro recebida para o usuário: {} com role: {}",
        registerRequestDTO.getUsername(),
        registerRequestDTO.getRole());
    Role role = Role.valueOf(registerRequestDTO.getRole().name());
    AuthUseCasePort.AuthResult result =
        authUseCase.register(
            registerRequestDTO.getUsername(), registerRequestDTO.getPassword(), role);
    log.info("Usuário registrado com sucesso: {} [role={}]", result.username(), result.role());
    return ResponseEntity.status(HttpStatus.CREATED).body(toDto(result));
  }

  @Override
  public ResponseEntity<AuthResponseDTO> refreshToken(
      RefreshTokenRequestDTO refreshTokenRequestDTO) {
    log.info("Requisição de refresh token recebida");
    AuthUseCasePort.AuthResult result =
        authUseCase.refreshToken(refreshTokenRequestDTO.getRefreshToken());
    log.info("Token renovado com sucesso para o usuário: {}", result.username());
    return ResponseEntity.ok(toDto(result));
  }

  @Override
  public ResponseEntity<Void> logout() {
    String authHeader = httpServletRequest.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      log.info("Requisição de logout recebida");
      authUseCase.logout(token);
    }
    return ResponseEntity.noContent().build();
  }

  private AuthResponseDTO toDto(AuthUseCasePort.AuthResult result) {
    AuthResponseDTO dto = new AuthResponseDTO();
    dto.setAccessToken(result.accessToken());
    dto.setRefreshToken(result.refreshToken());
    dto.setUsername(result.username());
    dto.setRole(result.role());
    return dto;
  }
}
