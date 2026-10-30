package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.domain.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/two-factor")
public class TwoFactorController {
  private static final Logger log = LoggerFactory.getLogger(TwoFactorController.class);

  private final AuthUseCasePort authUseCase;
  private final HttpServletRequest httpServletRequest;

  public TwoFactorController(AuthUseCasePort authUseCase, HttpServletRequest httpServletRequest) {
    this.authUseCase = authUseCase;
    this.httpServletRequest = httpServletRequest;
  }

  @GetMapping
  public AuthUseCasePort.TwoFactorStatus status() {
    return authUseCase.twoFactorStatus(accessToken());
  }

  @PostMapping("/setup")
  public AuthUseCasePort.TwoFactorSetup setup() {
    log.info("Configuração de verificação em duas etapas iniciada");
    return authUseCase.setupTwoFactor(accessToken());
  }

  @PostMapping("/enable")
  public RecoveryCodesResponse enable(@Valid @RequestBody CodeRequest request) {
    List<String> codes = authUseCase.enableTwoFactor(accessToken(), request.code());
    log.info("Verificação em duas etapas ativada");
    return new RecoveryCodesResponse(codes);
  }

  @PostMapping("/recovery-codes")
  public RecoveryCodesResponse regenerateRecoveryCodes(@Valid @RequestBody CodeRequest request) {
    List<String> codes = authUseCase.regenerateRecoveryCodes(accessToken(), request.code());
    log.info("Códigos de recuperação da verificação em duas etapas gerados novamente");
    return new RecoveryCodesResponse(codes);
  }

  @PostMapping("/disable")
  public ResponseEntity<Void> disable(@Valid @RequestBody CodeRequest request) {
    authUseCase.disableTwoFactor(accessToken(), request.code());
    log.info("Verificação em duas etapas desativada");
    return ResponseEntity.noContent().build();
  }

  private String accessToken() {
    String header = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank()) {
      throw new BusinessException("Sessão inválida.");
    }
    return header.substring(7);
  }

  public record RecoveryCodesResponse(List<String> recoveryCodes) {}

  public record CodeRequest(
      @NotBlank
          @Pattern(
              regexp = "\\d{6,8}|[A-Za-z0-9]{5}-?[A-Za-z0-9]{5}",
              message = "Informe o código de 6 dígitos ou um código de recuperação")
          String code) {}
}
