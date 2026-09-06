package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.domain.exception.AuthenticationUnavailableException;
import com.binitech.pdv.domain.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final AuthenticationGateway authentication;
  private final UserRepositoryPort users;

  public JwtAuthenticationFilter(AuthenticationGateway authentication, UserRepositoryPort users) {
    this.authentication = authentication;
    this.users = users;
  }

  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        var identity = authentication.session(header.substring(7));
        var user = users.findById(identity.userId()).orElse(null);
        if (user != null
            && user.isActive()
            && Objects.equals(user.getTenantId(), identity.tenantId())) {
          var principal =
              new UsernamePasswordAuthenticationToken(
                  user.getId(),
                  user.getUsername(),
                  List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
          principal.setDetails(user.getTenantId());
          SecurityContextHolder.getContext().setAuthentication(principal);
        }
      } catch (AuthenticationUnavailableException e) {
        SecurityContextHolder.clearContext();
        response.setStatus(503);
        return;
      } catch (BusinessException e) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
