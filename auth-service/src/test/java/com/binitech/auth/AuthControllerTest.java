package com.binitech.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binitech.auth.IdentityStore.Identity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class,
    properties = {
      "jwt.secret=test-secret-key-with-at-least-32-bytes", "jwt.access-expiration=60000",
      "jwt.refresh-expiration=86400000", "security.pepper=test-pepper",
      "cors.allowed-origins=https://pdv.example,https://other.example"
    })
@Import({
  AuthConfiguration.class,
  LoginService.class,
  TokenService.class,
  AuthExceptionHandler.class
})
class AuthControllerTest {
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;
  @Autowired TokenService tokens;
  @MockitoBean IdentityStore identities;
  @MockitoBean SessionStore sessions;
  private Identity user;

  @BeforeEach
  void setUp() {
    user = new Identity("user1", "admin", passwords.encode("password"), "ADMIN", "tenant1", true);
    when(identities.findCandidates("admin", null)).thenReturn(List.of(user));
    when(identities.findById("user1")).thenReturn(Optional.of(user));
  }

  @Test
  void login_returnsOriginalContractWithNoStoreCredentials() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"password\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
  }

  @Test
  void session_requiresValidBearer() throws Exception {
    mvc.perform(get("/api/auth/session")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/auth/session").header("Authorization", "Bearer invalid"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/auth/session").header("Authorization", "Bearer " + tokens.issue(user, 0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("user1"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void logout_requiresBearerEvenThoughRouteIsPublicToSecurityFilter() throws Exception {
    mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + tokens.issue(user, 0)))
        .andExpect(status().isNoContent());
    verify(sessions).deleteForUser("user1", "tenant1");
  }

  @Test
  void malformedOrInvalidBody_doesNotEchoPassword() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"never-echo-this\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("never-echo-this"))));
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invalidCredentialsAndRefresh_return401() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"unknown\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void cors_allowsBothApplicationsAndRejectsUnlistedOrigin() throws Exception {
    for (String origin : List.of("https://pdv.example", "https://other.example")) {
      mvc.perform(
              options("/api/auth/login")
                  .header("Origin", origin)
                  .header("Access-Control-Request-Method", "POST")
                  .header("Access-Control-Request-Headers", "Content-Type"))
          .andExpect(status().isOk())
          .andExpect(header().string("Access-Control-Allow-Origin", origin));
    }
    mvc.perform(
            options("/api/auth/login")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
  }

  @Test
  void databaseUnavailable_failsClosedWith503() throws Exception {
    when(identities.findCandidates(anyString(), any()))
        .thenThrow(
            new org.springframework.dao.DataAccessResourceFailureException(
                "internal connection details"));
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"password\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("AUTH_UNAVAILABLE"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal connection"))));
  }

  @Test
  void pdvBusinessEndpoints_areNotExposed() throws Exception {
    mvc.perform(get("/api/products")).andExpect(status().isForbidden());
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }
}
