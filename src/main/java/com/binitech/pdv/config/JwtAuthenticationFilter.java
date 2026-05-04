package com.binitech.pdv.config;

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

  public JwtAuthenticationFilter(
      JwtTokenProvider jwtTokenProvider, TokenBlacklistService tokenBlacklistService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.tokenBlacklistService = tokenBlacklistService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = extractToken(request);

    if (StringUtils.hasText(token)
        && jwtTokenProvider.validateToken(token)
        && !tokenBlacklistService.isBlacklisted(token)) {
      String userId = jwtTokenProvider.getUserIdFromToken(token);
      String username = jwtTokenProvider.getUsernameFromToken(token);
      String role = jwtTokenProvider.getRoleFromToken(token);

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userId, username, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

      SecurityContextHolder.getContext().setAuthentication(authentication);
      if (log.isDebugEnabled()) {
        log.debug(
            "Autenticação JWT configurada: userId={} username={} role={} path={}",
            Encode.forJava(userId),
            Encode.forJava(username),
            Encode.forJava(role),
            Encode.forJava(request.getRequestURI()));
      }
    } else if (StringUtils.hasText(token) && log.isWarnEnabled()) {
      log.warn(
          "Token JWT inválido recebido para path={}", Encode.forJava(request.getRequestURI()));
    }

    filterChain.doFilter(request, response);
  }

  private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
