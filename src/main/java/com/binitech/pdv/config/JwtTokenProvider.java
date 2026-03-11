package com.binitech.pdv.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long accessExpiration;

  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-expiration}") long accessExpiration) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessExpiration = accessExpiration;
  }

  public String generateAccessToken(String userId, String username, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessExpiration);

    return Jwts.builder()
        .subject(userId)
        .claim("username", username)
        .claim("role", role)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public String getUserIdFromToken(String token) {
    return getClaims(token).getSubject();
  }

  public String getUsernameFromToken(String token) {
    return getClaims(token).get("username", String.class);
  }

  public String getRoleFromToken(String token) {
    return getClaims(token).get("role", String.class);
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
