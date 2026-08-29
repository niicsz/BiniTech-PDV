package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.model.ErrorDTO;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDTO> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe -> {
                  String field = fe.getField();
                  Object rejected = fe.getRejectedValue();
                  String fieldLabel = getFieldLabel(field);
                  if (rejected == null || rejected.toString().isBlank()) {
                    return fieldLabel + " é obrigatório(a).";
                  }
                  return fieldLabel + ": valor inválido (" + rejected + ").";
                })
            .collect(Collectors.joining(" "));
    if (message.isBlank()) {
      message = "Dados inválidos. Verifique os campos e tente novamente.";
    }
    log.warn("Erro de validação: {}", message);
    ErrorDTO error = new ErrorDTO();
    error.setCode("VALIDATION_ERROR");
    error.setMessage(message);
    error.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorDTO> handleMalformedJson(HttpMessageNotReadableException ex) {
    log.warn("Corpo JSON inválido: {}", ex.getMostSpecificCause().getMessage());
    ErrorDTO error = new ErrorDTO();
    error.setCode("MALFORMED_JSON");
    error.setMessage("JSON inválido. Verifique a sintaxe dos dados enviados.");
    error.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorDTO> handleNotFound(ResourceNotFoundException ex) {
    log.warn("Recurso não encontrado: {}", ex.getMessage());
    ErrorDTO error = new ErrorDTO();
    error.setCode("NOT_FOUND");
    error.setMessage(ex.getMessage());
    error.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorDTO> handleBusiness(BusinessException ex) {
    log.warn("Erro de negócio: {}", ex.getMessage());
    ErrorDTO error = new ErrorDTO();
    error.setCode("BUSINESS_ERROR");
    error.setMessage(ex.getMessage());
    error.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorDTO> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Argumento inválido: {}", ex.getMessage());
    ErrorDTO error = new ErrorDTO();
    error.setCode("BAD_REQUEST");
    error.setMessage(ex.getMessage());
    error.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorDTO> handleAccessDenied(AccessDeniedException ex) {
    log.warn("Acesso negado: {}", ex.getMessage());
    ErrorDTO error = new ErrorDTO();
    error.setCode("FORBIDDEN");
    error.setMessage("Acesso negado. Você não tem permissão para esta operação.");
    error.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDTO> handleGeneric(Exception ex) {
    log.error("Erro interno inesperado: {}", ex.getMessage(), ex);
    ErrorDTO error = new ErrorDTO();
    error.setCode("INTERNAL_ERROR");
    error.setMessage("Erro interno do servidor. Tente novamente mais tarde.");
    error.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  private String getFieldLabel(String field) {
    return switch (field) {
      case "barcode" -> "Código de barras";
      case "description" -> "Descrição";
      case "price" -> "Preço de venda";
      case "costPrice" -> "Preço de custo";
      case "stockQuantity" -> "Estoque";
      case "category" -> "Categoria";
      case "username" -> "Nome de usuário";
      case "password" -> "Senha";
      default -> field;
    };
  }
}
