package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.AuthApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.AuthResponseDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.LoginRequestDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.RefreshTokenRequestDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.RegisterRequestDTO;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.application.ports.inbound.PasswordResetUseCasePort;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthUseCasePort authUseCase;
  private final HttpServletRequest httpServletRequest;
  private final AuthenticatedUserProvider authenticatedUserProvider;
  private final PasswordResetUseCasePort passwordResetUseCase;

  public AuthController(
      AuthUseCasePort authUseCase,
      HttpServletRequest httpServletRequest,
      AuthenticatedUserProvider authenticatedUserProvider,
      PasswordResetUseCasePort passwordResetUseCase) {
    this.authUseCase = authUseCase;
    this.httpServletRequest = httpServletRequest;
    this.authenticatedUserProvider = authenticatedUserProvider;
    this.passwordResetUseCase = passwordResetUseCase;
  }

  @Override
  public ResponseEntity<AuthResponseDTO> login(LoginRequestDTO loginRequestDTO) {
    log.info(
        "Requisição de login recebida para usuário: {}",
        LogSanitizer.maskUsername(loginRequestDTO.getUsername()));
    AuthUseCasePort.AuthResult result =
        authUseCase.login(
            loginRequestDTO.getUsername(),
            loginRequestDTO.getPassword(),
            loginRequestDTO.getTenantId());
    log.info("Login realizado com sucesso");
    return ResponseEntity.ok(toDto(result));
  }

  @Override
  public ResponseEntity<AuthResponseDTO> registerUser(RegisterRequestDTO registerRequestDTO) {
    log.info(
        "Requisição de registro recebida para usuário: {} role={}",
        LogSanitizer.maskUsername(registerRequestDTO.getUsername()),
        registerRequestDTO.getRole());
    Role role = Role.valueOf(registerRequestDTO.getRole().name());
    String tenantId =
        registerRequestDTO.getTenantId() != null
            ? registerRequestDTO.getTenantId()
            : authenticatedUserProvider.getTenantId();
    AuthUseCasePort.AuthResult result =
        authUseCase.register(
            registerRequestDTO.getUsername(), registerRequestDTO.getPassword(), role, tenantId);
    log.info("Usuário registrado com sucesso [role={}]", result.role());
    return ResponseEntity.status(HttpStatus.CREATED).body(toDto(result));
  }

  @Override
  public ResponseEntity<AuthResponseDTO> refreshToken(
      RefreshTokenRequestDTO refreshTokenRequestDTO) {
    log.info("Requisição de refresh token recebida");
    AuthUseCasePort.AuthResult result =
        authUseCase.refreshToken(refreshTokenRequestDTO.getRefreshToken());
    log.info("Token renovado com sucesso");
    return ResponseEntity.ok(toDto(result));
  }

  @PostMapping("/api/auth/change-password")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    log.info("Requisição de troca de senha recebida");
    authUseCase.changePassword(
        authenticatedUserProvider.getUserId(), request.currentPassword(), request.newPassword());
    log.info("Senha alterada com sucesso");
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/api/auth/forgot-password")
  public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    log.info("Requisição de redefinição de senha recebida");
    passwordResetUseCase.requestReset(request.tenantSlug(), request.username());
    // Sempre 204 para não revelar se a conta existe.
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/api/auth/reset-password")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    log.info("Requisição de confirmação de redefinição de senha recebida");
    passwordResetUseCase.resetPassword(request.token(), request.newPassword());
    return ResponseEntity.noContent().build();
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
    dto.setTenantId(result.tenantId());
    return dto;
  }

  public record ChangePasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 6, message = "A nova senha deve ter no mínimo 6 caracteres")
          String newPassword) {}

  public record ForgotPasswordRequest(@NotBlank String tenantSlug, @NotBlank String username) {}

  public record ResetPasswordRequest(
      @NotBlank String token,
      @NotBlank @Size(min = 6, message = "A nova senha deve ter no mínimo 6 caracteres")
          String newPassword) {}
}
