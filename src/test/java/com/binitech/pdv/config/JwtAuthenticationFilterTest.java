package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.binitech.pdv.application.ports.outbound.*;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.*;
import com.binitech.pdv.utils.enums.Role;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
  AuthenticationGateway auth = mock(AuthenticationGateway.class);
  UserRepositoryPort users = mock(UserRepositoryPort.class);
  FilterChain chain = mock(FilterChain.class);
  MockHttpServletRequest request = new MockHttpServletRequest();
  MockHttpServletResponse response = new MockHttpServletResponse();
  JwtAuthenticationFilter filter = new JwtAuthenticationFilter(auth, users);

  @BeforeEach
  void resetContext() {
    SecurityContextHolder.clearContext();
    request.addHeader("Authorization", "Bearer token");
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void usesLocalPermissionsInsteadOfAuthRole() throws Exception {
    when(auth.session("token"))
        .thenReturn(new AuthenticationGateway.SessionIdentity("u1", "user", "SUPER_ADMIN", "t1"));
    when(users.findById("u1"))
        .thenReturn(Optional.of(new User("u1", "user", null, Role.OPERATOR, "t1")));
    filter.doFilterInternal(request, response, chain);
    var principal = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(principal);
    assertEquals("[ROLE_OPERATOR]", principal.getAuthorities().toString());
    verify(chain).doFilter(request, response);
  }

  @Test
  void rejectsInactiveMembership() throws Exception {
    when(auth.session("token"))
        .thenReturn(new AuthenticationGateway.SessionIdentity("u1", "user", null, "t1"));
    var user = new User("u1", "user", null, Role.OPERATOR, "t1");
    user.setActive(false);
    when(users.findById("u1")).thenReturn(Optional.of(user));
    filter.doFilterInternal(request, response, chain);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void rejectsDifferentTenant() throws Exception {
    when(auth.session("token"))
        .thenReturn(new AuthenticationGateway.SessionIdentity("u1", "user", null, "t2"));
    when(users.findById("u1"))
        .thenReturn(Optional.of(new User("u1", "user", null, Role.OPERATOR, "t1")));
    filter.doFilterInternal(request, response, chain);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void rejectsRevokedOrInvalidSession() throws Exception {
    when(auth.session("token")).thenThrow(new BusinessException("Invalid session"));
    filter.doFilterInternal(request, response, chain);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(users);
  }

  @Test
  void failsClosedWhenAuthUnavailable() throws Exception {
    when(auth.session("token")).thenThrow(new AuthenticationUnavailableException());
    filter.doFilterInternal(request, response, chain);
    assertEquals(503, response.getStatus());
    verifyNoInteractions(chain, users);
  }

  @Test
  void permitsUnauthenticatedRequestToContinueToSecurityRules() throws Exception {
    request.removeHeader("Authorization");
    filter.doFilterInternal(request, response, chain);
    verifyNoInteractions(auth, users);
    verify(chain).doFilter(request, response);
  }
}
