package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.model.ErrorDTO;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorDTO> handleNotFound(ResourceNotFoundException ex) {
    ErrorDTO error = new ErrorDTO();
    error.setCode("NOT_FOUND");
    error.setMessage(ex.getMessage());
    error.setTimestamp(OffsetDateTime.now());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorDTO> handleBusiness(BusinessException ex) {
    ErrorDTO error = new ErrorDTO();
    error.setCode("BUSINESS_ERROR");
    error.setMessage(ex.getMessage());
    error.setTimestamp(OffsetDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorDTO> handleIllegalArgument(IllegalArgumentException ex) {
    ErrorDTO error = new ErrorDTO();
    error.setCode("BAD_REQUEST");
    error.setMessage(ex.getMessage());
    error.setTimestamp(OffsetDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDTO> handleGeneric(Exception ex) {
    ErrorDTO error = new ErrorDTO();
    error.setCode("INTERNAL_ERROR");
    error.setMessage("Erro interno do servidor: " + ex.getMessage());
    error.setTimestamp(OffsetDateTime.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
