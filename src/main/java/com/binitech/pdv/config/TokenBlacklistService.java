package com.binitech.pdv.config;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TokenBlacklistService {

  private static final String BLACKLIST_PREFIX = "token:blacklist:";
  private static final String SESSION_VERSION_PREFIX = "user:session-version:";

  private final StringRedisTemplate redisTemplate;
  private final long accessExpiration;

  public TokenBlacklistService(
      StringRedisTemplate redisTemplate, @Value("${jwt.access-expiration}") long accessExpiration) {
    this.redisTemplate = redisTemplate;
    this.accessExpiration = accessExpiration;
  }

  public void blacklist(String token) {
    redisTemplate
        .opsForValue()
        .set(BLACKLIST_PREFIX + token, "1", accessExpiration, TimeUnit.MILLISECONDS);
  }

  public boolean isBlacklisted(String token) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
  }

  public long getSessionVersion(String userId) {
    String storedVersion = redisTemplate.opsForValue().get(SESSION_VERSION_PREFIX + userId);
    if (storedVersion == null) {
      return 0L;
    }
    try {
      return Long.parseLong(storedVersion);
    } catch (NumberFormatException exception) {
      throw new IllegalStateException("Versão de sessão inválida para o usuário.", exception);
    }
  }

  public void revokeAllForUser(String userId) {
    redisTemplate.opsForValue().increment(SESSION_VERSION_PREFIX + userId);
  }

  public boolean isSessionRevoked(String userId, long tokenSessionVersion) {
    return tokenSessionVersion != getSessionVersion(userId);
  }
}
