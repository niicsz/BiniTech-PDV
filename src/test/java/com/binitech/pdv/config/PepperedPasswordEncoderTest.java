package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PepperedPasswordEncoderTest {

  @Test
  @DisplayName("encode deve adicionar pepper antes de delegar")
  void encode_shouldAppendPepper() {
    PasswordEncoder delegate = mock(PasswordEncoder.class);
    when(delegate.encode("password" + "pepper123")).thenReturn("encoded");

    PepperedPasswordEncoder encoder = new PepperedPasswordEncoder(delegate, "pepper123");
    String result = encoder.encode("password");

    assertEquals("encoded", result);
    verify(delegate).encode("password" + "pepper123");
  }

  @Test
  @DisplayName("matches deve adicionar pepper antes de delegar")
  void matches_shouldAppendPepper() {
    PasswordEncoder delegate = mock(PasswordEncoder.class);
    when(delegate.matches("password" + "pepper123", "encoded")).thenReturn(true);

    PepperedPasswordEncoder encoder = new PepperedPasswordEncoder(delegate, "pepper123");
    boolean result = encoder.matches("password", "encoded");

    assertTrue(result);
    verify(delegate).matches("password" + "pepper123", "encoded");
  }

  @Test
  @DisplayName("matches com senha errada deve retornar false")
  void matches_withWrongPassword_shouldReturnFalse() {
    PasswordEncoder delegate = mock(PasswordEncoder.class);
    when(delegate.matches("wrongpassword" + "pepper123", "encoded")).thenReturn(false);

    PepperedPasswordEncoder encoder = new PepperedPasswordEncoder(delegate, "pepper123");
    boolean result = encoder.matches("wrongpassword", "encoded");

    assertFalse(result);
  }

  @Test
  @DisplayName("Construtor com pepper nulo deve lançar IllegalArgumentException")
  void constructor_withNullPepper_shouldThrow() {
    PasswordEncoder delegate = mock(PasswordEncoder.class);

    assertThrows(IllegalArgumentException.class, () -> new PepperedPasswordEncoder(delegate, null));
  }

  @Test
  @DisplayName("Construtor com pepper vazio deve lançar IllegalArgumentException")
  void constructor_withBlankPepper_shouldThrow() {
    PasswordEncoder delegate = mock(PasswordEncoder.class);

    assertThrows(
        IllegalArgumentException.class, () -> new PepperedPasswordEncoder(delegate, "   "));
  }

  @Test
  @DisplayName("upgradeEncoding deve delegar")
  void upgradeEncoding_shouldDelegate() {
    PasswordEncoder delegate = mock(PasswordEncoder.class);
    when(delegate.upgradeEncoding("encoded")).thenReturn(false);

    PepperedPasswordEncoder encoder = new PepperedPasswordEncoder(delegate, "pepper123");
    boolean result = encoder.upgradeEncoding("encoded");

    assertFalse(result);
    verify(delegate).upgradeEncoding("encoded");
  }
}
