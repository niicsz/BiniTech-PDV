package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.*;

import com.binitech.pdv.adapters.inbound.web.generated.model.ErrorDTO;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
  }

  @Test
  @DisplayName("ResourceNotFoundException deve retornar 404")
  void handleNotFound_shouldReturn404() {
    ResourceNotFoundException ex = new ResourceNotFoundException("Produto", "id", "123");

    ResponseEntity<ErrorDTO> response = handler.handleNotFound(ex);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals("NOT_FOUND", response.getBody().getCode());
    assertNotNull(response.getBody().getMessage());
    assertNotNull(response.getBody().getTimestamp());
  }

  @Test
  @DisplayName("BusinessException deve retornar 400")
  void handleBusiness_shouldReturn400() {
    BusinessException ex = new BusinessException("Erro de negócio");

    ResponseEntity<ErrorDTO> response = handler.handleBusiness(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BUSINESS_ERROR", response.getBody().getCode());
    assertEquals("Erro de negócio", response.getBody().getMessage());
  }

  @Test
  @DisplayName("IllegalArgumentException deve retornar 400")
  void handleIllegalArgument_shouldReturn400() {
    IllegalArgumentException ex = new IllegalArgumentException("Argumento inválido");

    ResponseEntity<ErrorDTO> response = handler.handleIllegalArgument(ex);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("BAD_REQUEST", response.getBody().getCode());
    assertEquals("Argumento inválido", response.getBody().getMessage());
  }

  @Test
  @DisplayName("AccessDeniedException deve retornar 403")
  void handleAccessDenied_shouldReturn403() {
    AccessDeniedException ex = new AccessDeniedException("Acesso negado");

    ResponseEntity<ErrorDTO> response = handler.handleAccessDenied(ex);

    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("FORBIDDEN", response.getBody().getCode());
    assertTrue(response.getBody().getMessage().contains("permissão"));
  }

  @Test
  @DisplayName("Exception genérica deve retornar 500")
  void handleGeneric_shouldReturn500() {
    Exception ex = new Exception("Erro genérico");

    ResponseEntity<ErrorDTO> response = handler.handleGeneric(ex);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("INTERNAL_ERROR", response.getBody().getCode());
    assertTrue(response.getBody().getMessage().contains("Erro interno"));
  }
}
