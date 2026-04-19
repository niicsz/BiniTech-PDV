package com.binitech.pdv.config;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TokenBlacklistService {

  private static final String BLACKLIST_PREFIX = "token:blacklist:";

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
}
