package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    String secret = "test-secret-key-that-is-at-least-32-characters-long-for-hmac";
    jwtTokenProvider = new JwtTokenProvider(secret, 3600000L);
  }

  @Test
  @DisplayName("Deve gerar token válido")
  void generateAccessToken_shouldBeValid() {
    String token = jwtTokenProvider.generateAccessToken("user1", "admin", "ADMIN");

    assertNotNull(token);
    assertTrue(jwtTokenProvider.validateToken(token));
  }

  @Test
  @DisplayName("Deve extrair userId do token")
  void getUserIdFromToken_shouldReturnCorrectUserId() {
    String token = jwtTokenProvider.generateAccessToken("user1", "admin", "ADMIN");

    assertEquals("user1", jwtTokenProvider.getUserIdFromToken(token));
  }

  @Test
  @DisplayName("Deve extrair username do token")
  void getUsernameFromToken_shouldReturnCorrectUsername() {
    String token = jwtTokenProvider.generateAccessToken("user1", "admin", "ADMIN");

    assertEquals("admin", jwtTokenProvider.getUsernameFromToken(token));
  }

  @Test
  @DisplayName("Deve extrair role do token")
  void getRoleFromToken_shouldReturnCorrectRole() {
    String token = jwtTokenProvider.generateAccessToken("user1", "admin", "ADMIN");

    assertEquals("ADMIN", jwtTokenProvider.getRoleFromToken(token));
  }

  @Test
  @DisplayName("Token adulterado deve ser inválido")
  void validateToken_withTamperedToken_shouldReturnFalse() {
    String token = jwtTokenProvider.generateAccessToken("user1", "admin", "ADMIN");
    String tamperedToken = token + "tampered";

    assertFalse(jwtTokenProvider.validateToken(tamperedToken));
  }

  @Test
  @DisplayName("Token nulo deve ser inválido")
  void validateToken_withNull_shouldReturnFalse() {
    assertFalse(jwtTokenProvider.validateToken(null));
  }

  @Test
  @DisplayName("Token vazio deve ser inválido")
  void validateToken_withEmpty_shouldReturnFalse() {
    assertFalse(jwtTokenProvider.validateToken(""));
  }

  @Test
  @DisplayName("Token expirado deve ser inválido")
  void validateToken_withExpiredToken_shouldReturnFalse() {
    JwtTokenProvider expiredProvider =
        new JwtTokenProvider("test-secret-key-that-is-at-least-32-characters-long-for-hmac", 0L);
    String token = expiredProvider.generateAccessToken("user1", "admin", "ADMIN");

    assertFalse(expiredProvider.validateToken(token));
  }

  @Test
  @DisplayName("Token assinado com outra chave deve ser inválido")
  void validateToken_withDifferentKey_shouldReturnFalse() {
    JwtTokenProvider otherProvider =
        new JwtTokenProvider(
            "another-secret-key-that-is-at-least-32-characters-long-here", 3600000L);
    String token = otherProvider.generateAccessToken("user1", "admin", "ADMIN");

    assertFalse(jwtTokenProvider.validateToken(token));
  }
}
