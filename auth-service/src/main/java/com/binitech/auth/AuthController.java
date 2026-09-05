package com.binitech.auth;

import com.binitech.auth.LoginService.AuthResult;
import com.binitech.auth.LoginService.InvalidCredentialsException;
import com.binitech.auth.LoginService.SessionIdentity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final LoginService login;

  public AuthController(LoginService login) {
    this.login = login;
  }

  @PostMapping("/login")
  public AuthResult login(@Valid @RequestBody LoginRequest request) {
    return login.login(request.username(), request.password(), request.tenantId());
  }

  @PostMapping("/refresh")
  public AuthResult refresh(@Valid @RequestBody RefreshRequest request) {
    return login.refresh(request.refreshToken());
  }

  @GetMapping("/session")
  public SessionIdentity session(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    return login.session(bearer(authorization));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    login.logout(bearer(authorization));
    return ResponseEntity.noContent().build();
  }

  private String bearer(String authorization) {
    if (authorization == null
        || !authorization.startsWith("Bearer ")
        || authorization.substring(7).isBlank()) {
      throw new InvalidCredentialsException();
    }
    return authorization.substring(7);
  }

  public record LoginRequest(
      @NotBlank @Size(max = 200) String username,
      @NotBlank @Size(max = 1024) String password,
      @Size(max = 200) String tenantId) {}

  public record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {}
}
