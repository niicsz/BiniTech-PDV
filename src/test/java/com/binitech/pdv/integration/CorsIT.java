package com.binitech.pdv.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsIT {

  private static final String ALLOWED_ORIGIN = "http://localhost:4200";
  private static final String DISALLOWED_ORIGIN = "https://evil.example.com";

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Preflight de origem permitida deve retornar 200 com headers de CORS")
  void preflight_allowedOrigin_shouldReturn200WithCorsHeaders() throws Exception {
    mockMvc
        .perform(
            options("/api/auth/refresh")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  @DisplayName("Preflight de origem nao permitida deve ser rejeitado")
  void preflight_disallowedOrigin_shouldBeForbidden() throws Exception {
    mockMvc
        .perform(
            options("/api/auth/refresh")
                .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Respostas HTTPS devem incluir cabeçalhos defensivos")
  void secureResponse_shouldIncludeDefensiveHeaders() throws Exception {
    mockMvc
        .perform(get("/actuator/health").secure(true))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
        .andExpect(
            header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
        .andExpect(
            header().string("Strict-Transport-Security", containsString("max-age=31536000")));
  }
}
