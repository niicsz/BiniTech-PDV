package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtTokenProvider jwtTokenProvider;
  private final TokenBlacklistService tokenBlacklistService;
  private final UserRepositoryPort userRepository;

  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider,
      TokenBlacklistService tokenBlacklistService,
      UserRepositoryPort userRepository) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.tokenBlacklistService = tokenBlacklistService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = extractToken(request);
    if (StringUtils.hasText(token)) {
      processToken(token, request);
    }

    filterChain.doFilter(request, response);
  }

  private void processToken(String token, HttpServletRequest request) {
    if (!jwtTokenProvider.validateToken(token) || tokenBlacklistService.isBlacklisted(token)) {
      logInvalidToken(request);
      return;
    }

    String userId = jwtTokenProvider.getUserIdFromToken(token);
    long sessionVersion = jwtTokenProvider.getSessionVersionFromToken(token);
    if (tokenBlacklistService.isSessionRevoked(userId, sessionVersion)) {
      logRevokedSession(userId, request);
      return;
    }

    User currentUser = userRepository.findById(userId).orElse(null);
    if (currentUser == null || !currentUser.isActive()) {
      logInactiveUser(userId, request);
      return;
    }

    authenticate(userId, currentUser, request);
  }

  private void authenticate(String userId, User currentUser, HttpServletRequest request) {
    String username = currentUser.getUsername();
    String role = currentUser.getRole().name();
    String tenantId = currentUser.getTenantId();

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userId, username, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    authentication.setDetails(tenantId);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    if (log.isDebugEnabled()) {
      log.debug(
          "Autenticação JWT configurada: userId={} username={} role={} tenantId={} path={}",
          Encode.forJava(userId),
          Encode.forJava(username),
          Encode.forJava(role),
          Encode.forJava(String.valueOf(tenantId)),
          Encode.forJava(request.getRequestURI()));
    }
  }

  private void logInvalidToken(HttpServletRequest request) {
    if (log.isWarnEnabled()) {
      log.warn("Token JWT inválido recebido para path={}", Encode.forJava(request.getRequestURI()));
    }
  }

  private void logRevokedSession(String userId, HttpServletRequest request) {
    if (log.isWarnEnabled()) {
      log.warn(
          "Sessão JWT revogada recebida para userId={} path={}",
          Encode.forJava(userId),
          Encode.forJava(request.getRequestURI()));
    }
  }

  private void logInactiveUser(String userId, HttpServletRequest request) {
    if (log.isWarnEnabled()) {
      log.warn(
          "Token rejeitado para usuário inexistente ou inativo: userId={} path={}",
          Encode.forJava(userId),
          Encode.forJava(request.getRequestURI()));
    }
  }

  private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
