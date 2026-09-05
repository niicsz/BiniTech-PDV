package com.binitech.auth;

import java.time.Instant;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {
  @ExceptionHandler(LoginService.InvalidCredentialsException.class)
  ResponseEntity<ErrorResponse> invalidCredentials() {
    return error(
        HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Credenciais ou sessão inválidas.");
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  ResponseEntity<ErrorResponse> invalidRequest() {
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "Dados inválidos. Verifique os campos enviados.");
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<ErrorResponse> unavailable() {
    return error(
        HttpStatus.SERVICE_UNAVAILABLE,
        "AUTH_UNAVAILABLE",
        "Autenticação temporariamente indisponível.");
  }

  private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new ErrorResponse(code, message, Instant.now()));
  }

  record ErrorResponse(String code, String message, Instant timestamp) {}
}
