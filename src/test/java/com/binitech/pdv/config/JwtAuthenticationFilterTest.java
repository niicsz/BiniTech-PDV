package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private TokenBlacklistService tokenBlacklistService;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklistService);
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Token válido deve definir autenticação no SecurityContext")
  void doFilter_withValidToken_shouldSetAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
    when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
    when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn("user1");
    when(jwtTokenProvider.getSessionVersionFromToken("valid-token")).thenReturn(2L);
    when(tokenBlacklistService.isSessionRevoked("user1", 2L)).thenReturn(false);
    when(jwtTokenProvider.getUsernameFromToken("valid-token")).thenReturn("admin");
    when(jwtTokenProvider.getRoleFromToken("valid-token")).thenReturn("ADMIN");

    filter.doFilterInternal(request, response, filterChain);

    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    assertEquals("user1", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    assertEquals("admin", SecurityContextHolder.getContext().getAuthentication().getCredentials());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Token de sessão revogada não deve definir autenticação")
  void doFilter_withRevokedSession_shouldNotSetAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer revoked-token");
    when(jwtTokenProvider.validateToken("revoked-token")).thenReturn(true);
    when(jwtTokenProvider.getUserIdFromToken("revoked-token")).thenReturn("user1");
    when(jwtTokenProvider.getSessionVersionFromToken("revoked-token")).thenReturn(1L);
    when(tokenBlacklistService.isSessionRevoked("user1", 1L)).thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Sem header Authorization não deve definir autenticação")
  void doFilter_withNoHeader_shouldNotSetAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Token inválido não deve definir autenticação")
  void doFilter_withInvalidToken_shouldNotSetAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
    when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  @DisplayName("Header sem 'Bearer ' não deve definir autenticação")
  void doFilter_withNoBearerPrefix_shouldNotSetAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic some-token");

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain).doFilter(request, response);
  }
}
