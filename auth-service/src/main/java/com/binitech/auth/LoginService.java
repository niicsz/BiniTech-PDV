package com.binitech.auth;

import com.binitech.auth.IdentityStore.Identity;
import com.binitech.auth.SessionStore.RefreshSession;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
  private final IdentityStore identities;
  private final SessionStore sessions;
  private final TokenService tokens;
  private final PasswordEncoder passwords;
  private final long refreshExpiration;
  private final String dummyHash;

  public LoginService(
      IdentityStore identities,
      SessionStore sessions,
      TokenService tokens,
      PasswordEncoder passwords,
      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
    if (refreshExpiration <= 0) {
      throw new IllegalArgumentException("JWT_REFRESH_EXPIRATION deve ser positivo.");
    }
    this.identities = identities;
    this.sessions = sessions;
    this.tokens = tokens;
    this.passwords = passwords;
    this.refreshExpiration = refreshExpiration;
    this.dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  public AuthResult login(String username, String password, String tenantId) {
    List<Identity> candidates = identities.findCandidates(username, tenantId);
    if (candidates.isEmpty()) {
      passwords.matches(password, dummyHash);
      throw new InvalidCredentialsException();
    }
    Identity match = null;
    int matches = 0;
    for (Identity candidate : candidates) {
      if (passwordMatches(password, candidate.password())) {
        match = candidate;
        matches++;
      }
    }
    if (matches != 1 || !match.isActive()) {
      throw new InvalidCredentialsException();
    }
    // Independent logins can coexist, allowing several applications to use the identity service.
    return issueSession(match, sessions.sessionVersion(match.id()));
  }

  public AuthResult refresh(String token) {
    RefreshSession previous = sessions.consume(token).orElseThrow(InvalidCredentialsException::new);
    if (previous.expiryDate() == null || !previous.expiryDate().isAfter(Instant.now())) {
      throw new InvalidCredentialsException();
    }
    Identity identity =
        identities.findById(previous.userId()).orElseThrow(InvalidCredentialsException::new);
    long version = sessions.sessionVersion(identity.id());
    long previousVersion = previous.sessionVersion() == null ? 0L : previous.sessionVersion();
    if (!identity.isActive()
        || !Objects.equals(identity.tenantId(), previous.tenantId())
        || version != previousVersion) {
      throw new InvalidCredentialsException();
    }
    return issueSession(identity, version);
  }

  public SessionIdentity session(String token) {
    Claims claims = parse(token);
    String id = claims.getSubject();
    Number version = claims.get("sessionVersion", Number.class);
    if (sessions.isBlacklisted(token)
        || sessions.sessionVersion(id) != (version == null ? 0L : version.longValue())) {
      throw new InvalidCredentialsException();
    }
    Identity identity = identities.findById(id).orElseThrow(InvalidCredentialsException::new);
    if (!identity.isActive()) {
      throw new InvalidCredentialsException();
    }
    return new SessionIdentity(
        identity.id(), identity.username(), identity.role(), identity.tenantId());
  }

  public void logout(String token) {
    SessionIdentity identity = session(token);
    Claims claims = parse(token);
    sessions.blacklist(
        token, Math.max(1, claims.getExpiration().getTime() - System.currentTimeMillis()));
    sessions.deleteForUser(identity.userId(), identity.tenantId());
  }

  private Claims parse(String token) {
    try {
      return tokens.parse(token);
    } catch (JwtException | IllegalArgumentException exception) {
      throw new InvalidCredentialsException();
    }
  }

  private boolean passwordMatches(String password, String hash) {
    try {
      return passwords.matches(password, hash);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private AuthResult issueSession(Identity identity, long version) {
    String accessToken = tokens.issue(identity, version);
    String refreshToken = UUID.randomUUID().toString();
    sessions.save(
        new RefreshSession(
            null,
            refreshToken,
            identity.id(),
            identity.tenantId(),
            Instant.now().plusMillis(refreshExpiration),
            version));
    return new AuthResult(
        accessToken, refreshToken, identity.username(), identity.role(), identity.tenantId());
  }

  public record AuthResult(
      String accessToken, String refreshToken, String username, String role, String tenantId) {}

  public record SessionIdentity(String userId, String username, String role, String tenantId) {}

  public static class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
      super("Credenciais ou sessão inválidas.");
    }
  }
}
