package com.binitech.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.binitech.auth.IdentityStore.Identity;
import com.binitech.auth.LoginService.InvalidCredentialsException;
import com.binitech.auth.SessionStore.RefreshSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginServiceTest {
  private final IdentityStore identities = mock(IdentityStore.class);
  private final SessionStore sessions = mock(SessionStore.class);
  private final TokenService tokens =
      new TokenService("test-secret-key-with-at-least-32-bytes", 60000);
  private final org.springframework.security.crypto.password.PasswordEncoder passwords =
      new AuthConfiguration().passwordEncoder("test-pepper");
  private final ConcurrentHashMap<String, RefreshSession> refreshSessions =
      new ConcurrentHashMap<>();
  private LoginService login;
  private Identity user;

  @BeforeEach
  void setUp() {
    login = new LoginService(identities, sessions, tokens, passwords, 86400000);
    user =
        new Identity(
            "user1", "admin", passwords.encode("password"), "TENANT_ADMIN", "tenant1", true);
    when(identities.findCandidates("admin", null)).thenReturn(List.of(user));
    when(identities.findById("user1")).thenReturn(Optional.of(user));
    doAnswer(
            invocation -> {
              RefreshSession session = invocation.getArgument(0);
              refreshSessions.put(session.token(), session);
              return null;
            })
        .when(sessions)
        .save(any());
    when(sessions.consume(anyString()))
        .thenAnswer(
            invocation -> Optional.ofNullable(refreshSessions.remove(invocation.getArgument(0))));
  }

  @Test
  void loginAndSession_returnCompatibleClaimsWithoutPassword() {
    var result = login.login("admin", "password", null);
    var claims = tokens.parse(result.accessToken());
    assertEquals("user1", claims.getSubject());
    assertEquals("TENANT_ADMIN", claims.get("role"));
    assertEquals("tenant1", result.tenantId());
    assertFalse(claims.containsKey("password"));
    assertEquals("user1", login.session(result.accessToken()).userId());
  }

  @Test
  void loginForAnotherApplication_preservesExistingRefreshSession() {
    var first = login.login("admin", "password", null);
    var second = login.login("admin", "password", null);
    assertNotEquals(first.accessToken(), second.accessToken());
    assertNotNull(login.refresh(first.refreshToken()));
    assertNotNull(login.refresh(second.refreshToken()));
    verify(sessions, never()).deleteForUser(anyString(), any());
  }

  @Test
  void refresh_consumesOldTokenAndIssuesDistinctTokens() {
    var first = login.login("admin", "password", null);
    var second = login.refresh(first.refreshToken());
    assertNotEquals(first.refreshToken(), second.refreshToken());
    assertNotEquals(first.accessToken(), second.accessToken());
    assertThrows(InvalidCredentialsException.class, () -> login.refresh(first.refreshToken()));
  }

  @Test
  void concurrentRefresh_onlyOneRequestSucceeds() throws Exception {
    var first = login.login("admin", "password", null);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Boolean> refresh =
        () -> {
          start.await();
          try {
            login.refresh(first.refreshToken());
            return true;
          } catch (InvalidCredentialsException exception) {
            return false;
          }
        };
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var a = executor.submit(refresh);
      var b = executor.submit(refresh);
      start.countDown();
      assertNotEquals(a.get(), b.get());
    }
  }

  @Test
  void unknownUserWrongPasswordAndInactiveUser_areRejected() {
    when(identities.findCandidates("missing", null)).thenReturn(List.of());
    assertThrows(InvalidCredentialsException.class, () -> login.login("missing", "password", null));
    assertThrows(InvalidCredentialsException.class, () -> login.login("admin", "wrong", null));
    when(identities.findCandidates("admin", null))
        .thenReturn(
            List.of(
                new Identity(
                    user.id(),
                    user.username(),
                    user.password(),
                    user.role(),
                    user.tenantId(),
                    false)));
    assertThrows(InvalidCredentialsException.class, () -> login.login("admin", "password", null));
    assertTrue(refreshSessions.isEmpty());
  }

  @Test
  void ambiguousLogin_requiresTenantSelection() {
    var other = new Identity("user2", "admin", user.password(), "OPERATOR", "tenant2", true);
    when(identities.findCandidates("admin", null)).thenReturn(List.of(user, other));
    assertThrows(InvalidCredentialsException.class, () -> login.login("admin", "password", null));
    when(identities.findCandidates("admin", "tenant2")).thenReturn(List.of(other));
    assertEquals("tenant2", login.login("admin", "password", "tenant2").tenantId());
  }

  @Test
  void expiredRefresh_isConsumedAndRejected() {
    refreshSessions.put(
        "expired",
        new RefreshSession(null, "expired", user.id(), user.tenantId(), Instant.EPOCH, 0L));
    assertThrows(InvalidCredentialsException.class, () -> login.refresh("expired"));
    assertFalse(refreshSessions.containsKey("expired"));
  }

  @Test
  void passwordChangeRevocation_rejectsAccessAndRefresh() {
    var first = login.login("admin", "password", null);
    when(sessions.sessionVersion(user.id())).thenReturn(1L);
    assertThrows(InvalidCredentialsException.class, () -> login.session(first.accessToken()));
    assertThrows(InvalidCredentialsException.class, () -> login.refresh(first.refreshToken()));
  }

  @Test
  void inactiveUser_cannotUseExistingAccessOrRefresh() {
    var first = login.login("admin", "password", null);
    when(identities.findById(user.id()))
        .thenReturn(
            Optional.of(
                new Identity(
                    user.id(),
                    user.username(),
                    user.password(),
                    user.role(),
                    user.tenantId(),
                    false)));
    assertThrows(InvalidCredentialsException.class, () -> login.session(first.accessToken()));
    assertThrows(InvalidCredentialsException.class, () -> login.refresh(first.refreshToken()));
  }

  @Test
  void session_readsCurrentRoleAndRejectsDeletedUsers() {
    var first = login.login("admin", "password", null);
    when(identities.findById(user.id()))
        .thenReturn(
            Optional.of(
                new Identity(
                    user.id(),
                    user.username(),
                    user.password(),
                    "OPERATOR",
                    user.tenantId(),
                    null)));
    assertEquals("OPERATOR", login.session(first.accessToken()).role());
    when(identities.findById(user.id())).thenReturn(Optional.empty());
    assertThrows(InvalidCredentialsException.class, () -> login.session(first.accessToken()));
  }

  @Test
  void logout_blacklistsAccessAndDeletesRefreshSessions() {
    var first = login.login("admin", "password", null);
    login.logout(first.accessToken());
    verify(sessions).blacklist(eq(first.accessToken()), longThat(ttl -> ttl > 0 && ttl <= 60000));
    verify(sessions).deleteForUser(user.id(), user.tenantId());
    when(sessions.isBlacklisted(first.accessToken())).thenReturn(true);
    assertThrows(InvalidCredentialsException.class, () -> login.session(first.accessToken()));
  }

  @Test
  void malformedExpiredAndTamperedTokens_areRejected() {
    assertThrows(InvalidCredentialsException.class, () -> login.session("not-a-token"));
    var otherSigner = new TokenService("another-secret-key-with-at-least-32-bytes", 60000);
    assertThrows(
        InvalidCredentialsException.class, () -> login.session(otherSigner.issue(user, 0)));
    var expired =
        io.jsonwebtoken.Jwts.builder()
            .subject(user.id())
            .expiration(java.util.Date.from(Instant.EPOCH))
            .signWith(
                io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                    "test-secret-key-with-at-least-32-bytes"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();
    assertThrows(InvalidCredentialsException.class, () -> login.session(expired));
  }

  @Test
  void legacyRefreshTokenWithoutVersion_remainsCompatible() {
    refreshSessions.put(
        "legacy",
        new RefreshSession(
            "old-id", "legacy", user.id(), user.tenantId(), Instant.now().plusSeconds(60), null));
    assertNotNull(login.refresh("legacy"));
  }
}
