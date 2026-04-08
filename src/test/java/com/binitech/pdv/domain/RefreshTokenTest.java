package com.binitech.pdv.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

  @Test
  @DisplayName("isExpired deve retornar false para data futura")
  void isExpired_withFutureDate_shouldReturnFalse() {
    RefreshToken token = new RefreshToken();
    token.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));

    assertFalse(token.isExpired());
  }

  @Test
  @DisplayName("isExpired deve retornar true para data passada")
  void isExpired_withPastDate_shouldReturnTrue() {
    RefreshToken token = new RefreshToken();
    token.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));

    assertTrue(token.isExpired());
  }

  @Test
  @DisplayName("Construtor com argumentos deve definir todos os campos")
  void constructor_shouldSetAllFields() {
    Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);
    RefreshToken token = new RefreshToken("id1", "token123", "user1", expiry);

    assertEquals("id1", token.getId());
    assertEquals("token123", token.getToken());
    assertEquals("user1", token.getUserId());
    assertEquals(expiry, token.getExpiryDate());
  }
}
