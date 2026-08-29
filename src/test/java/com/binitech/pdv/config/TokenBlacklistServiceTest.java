package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private TokenBlacklistService service;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    service = new TokenBlacklistService(redisTemplate, 60_000L);
  }

  @Test
  @DisplayName("Versão diferente deve indicar sessão revogada")
  void isSessionRevoked_withDifferentVersion_shouldReturnTrue() {
    when(valueOperations.get("user:session-version:user1")).thenReturn("3");

    assertTrue(service.isSessionRevoked("user1", 2L));
    assertFalse(service.isSessionRevoked("user1", 3L));
  }

  @Test
  @DisplayName("Revogação deve incrementar atomicamente a versão das sessões")
  void revokeAllForUser_shouldIncrementVersion() {
    service.revokeAllForUser("user1");

    verify(valueOperations).increment("user:session-version:user1");
  }
}
