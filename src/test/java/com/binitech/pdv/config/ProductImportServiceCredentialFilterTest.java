package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;

class ProductImportServiceCredentialFilterTest {
  private static final String KEY = "0123456789abcdef0123456789abcdef";

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void acceptsValidServiceCredentialOnlyOnInternalImportRoute() throws Exception {
    var request = new MockHttpServletRequest("POST", "/api/internal/product-import/authorize");
    request.addHeader("X-Product-Import-Service-Key", KEY);
    var response = new MockHttpServletResponse();

    new ProductImportServiceCredentialFilter(KEY)
        .doFilter(request, response, new MockFilterChain());

    assertEquals(200, response.getStatus());
    assertTrue(
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_PRODUCT_IMPORT_SERVICE")));
  }

  @Test
  void rejectsMissingOrInvalidCredentialWithoutCallingAuthenticationAlternative() throws Exception {
    var request = new MockHttpServletRequest("POST", "/api/internal/product-import/apply");
    var response = new MockHttpServletResponse();

    new ProductImportServiceCredentialFilter(KEY)
        .doFilter(request, response, new MockFilterChain());

    assertEquals(401, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void refusesWeakConfiguredCredentialAtStartup() {
    assertThrows(
        IllegalArgumentException.class, () -> new ProductImportServiceCredentialFilter("short"));
  }
}
