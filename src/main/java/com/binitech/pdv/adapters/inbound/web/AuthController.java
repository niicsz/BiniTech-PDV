package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.AuthApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.AuthResponseDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.LoginRequestDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.RefreshTokenRequestDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.RegisterRequestDTO;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.utils.Enum.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

  private final AuthUseCasePort authUseCase;

  public AuthController(AuthUseCasePort authUseCase) {
    this.authUseCase = authUseCase;
  }

  @Override
  public ResponseEntity<AuthResponseDTO> login(LoginRequestDTO loginRequestDTO) {
    AuthUseCasePort.AuthResult result =
        authUseCase.login(loginRequestDTO.getUsername(), loginRequestDTO.getPassword());
    return ResponseEntity.ok(toDto(result));
  }

  @Override
  public ResponseEntity<AuthResponseDTO> registerUser(RegisterRequestDTO registerRequestDTO) {
    Role role = Role.valueOf(registerRequestDTO.getRole().name());
    AuthUseCasePort.AuthResult result =
        authUseCase.register(
            registerRequestDTO.getUsername(), registerRequestDTO.getPassword(), role);
    return ResponseEntity.status(HttpStatus.CREATED).body(toDto(result));
  }

  @Override
  public ResponseEntity<AuthResponseDTO> refreshToken(
      RefreshTokenRequestDTO refreshTokenRequestDTO) {
    AuthUseCasePort.AuthResult result =
        authUseCase.refreshToken(refreshTokenRequestDTO.getRefreshToken());
    return ResponseEntity.ok(toDto(result));
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
