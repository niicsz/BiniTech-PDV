package com.binitech.pdv.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

  private final SecretKey key;
  private final long accessExpiration;

  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-expiration}") long accessExpiration) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "Configuração inválida: jwt.secret não pode estar vazio. "
              + "Defina a variável de ambiente JWT_SECRET com pelo menos 32 caracteres.");
    }
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalStateException(
          "Configuração inválida: jwt.secret deve ter pelo menos 32 bytes "
              + "(256 bits) para HS256. Atual: "
              + secretBytes.length
              + " bytes.");
    }
    this.key = Keys.hmacShaKeyFor(secretBytes);
    this.accessExpiration = accessExpiration;
    log.info("JwtTokenProvider inicializado com expiração de access token: {}ms", accessExpiration);
  }

  public String generateAccessToken(String userId, String username, String role, String tenantId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessExpiration);

    String token =
        Jwts.builder()
            .subject(userId)
            .claim("username", username)
            .claim("role", role)
            .claim("tenantId", tenantId)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact();

    log.debug(
        "Access token gerado para userId={} username={} role={} tenantId={}",
        userId,
        username,
        role,
        tenantId);
    return token;
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      if (log.isWarnEnabled()) {
        log.warn("Validação de token JWT falhou: {}", e.getMessage());
      }
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

  public String getTenantIdFromToken(String token) {
    return getClaims(token).get("tenantId", String.class);
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
