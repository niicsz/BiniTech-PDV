package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.utils.Enum.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

  public String getUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("Usuário não autenticado.");
    }
    return (String) auth.getPrincipal();
  }

  public Role getUserRole() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return Role.OPERATOR;
    }
    String role =
        auth.getAuthorities().stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse("OPERATOR");
    return Role.valueOf(role);
  }
}
