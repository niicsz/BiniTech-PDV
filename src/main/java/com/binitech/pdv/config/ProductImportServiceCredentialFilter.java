package com.binitech.pdv.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ProductImportServiceCredentialFilter extends OncePerRequestFilter {
  private static final String PREFIX = "/api/internal/product-import/";
  private final byte[] credential;

  public ProductImportServiceCredentialFilter(String credential) {
    if (credential == null || credential.length() < 32) {
      throw new IllegalArgumentException(
          "PRODUCT_IMPORT_SERVICE_KEY deve ter ao menos 32 caracteres.");
    }
    this.credential = credential.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().substring(request.getContextPath().length()).startsWith(PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String supplied = request.getHeader("X-Product-Import-Service-Key");
    if (supplied == null
        || !MessageDigest.isEqual(credential, supplied.getBytes(StandardCharsets.UTF_8))) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "product-import-service",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PRODUCT_IMPORT_SERVICE"))));
    chain.doFilter(request, response);
  }
}
