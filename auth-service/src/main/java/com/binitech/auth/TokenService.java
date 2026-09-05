package com.binitech.auth;

import com.binitech.auth.IdentityStore.Identity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private final SecretKey key;
  private final long accessExpiration;

  public TokenService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-expiration}") long accessExpiration) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes.");
    }
    if (accessExpiration <= 0) {
      throw new IllegalArgumentException("JWT_ACCESS_EXPIRATION deve ser positivo.");
    }
    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessExpiration = accessExpiration;
  }

  public String issue(Identity identity, long version) {
    Date now = new Date();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(identity.id())
        .claim("username", identity.username())
        .claim("role", identity.role())
        .claim("tenantId", identity.tenantId())
        .claim("sessionVersion", version)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + accessExpiration))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    if (claims.getSubject() == null
        || claims.getSubject().isBlank()
        || claims.getExpiration() == null) {
      throw new IllegalArgumentException("Token sem identificação ou expiração.");
    }
    return claims;
  }
}
