package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.binitech.pdv.adapters.inbound.web.generated.model.CurrentUserDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.UserRole;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthControllerCurrentUserTest {

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("GET /api/auth/me devolve o papel do cadastro do PDV, não o do Auth")
  void currentUserReturnsPdvRole() {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            "user-1", "teste", List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")));
    authentication.setDetails("tenant-1");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    AuthController controller =
        new AuthController(null, null, new AuthenticatedUserProvider(), null, null);

    ResponseEntity<CurrentUserDTO> response = controller.currentUser();

    CurrentUserDTO body = response.getBody();
    assertEquals(200, response.getStatusCode().value());
    assertEquals("user-1", body.getUserId());
    assertEquals("teste", body.getUsername());
    assertEquals(UserRole.TENANT_ADMIN, body.getRole());
    assertEquals("tenant-1", body.getTenantId());
  }
}
