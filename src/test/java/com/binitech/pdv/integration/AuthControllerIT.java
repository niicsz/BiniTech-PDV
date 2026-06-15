package com.binitech.pdv.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.Enum.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private MongoTemplate mongoTemplate;

  private String adminToken;
  private User adminUser;

  @BeforeEach
  void setUp() {
    mongoTemplate.getDb().getCollection("users").drop();
    mongoTemplate.getDb().getCollection("refresh_tokens").drop();

    adminUser =
        userRepository
            .findByUsername("admin")
            .orElseGet(
                () -> {
                  User user = new User();
                  user.setUsername("admin");
                  user.setPassword(passwordEncoder.encode("admin123"));
                  user.setRole(Role.ADMIN);
                  return userRepository.save(user);
                });

    adminToken =
        jwtTokenProvider.generateAccessToken(
            adminUser.getId(),
            adminUser.getUsername(),
            adminUser.getRole().name(),
            adminUser.getTenantId());
  }

  @Nested
  @DisplayName("POST /api/auth/login")
  class LoginTests {

    @Test
    @DisplayName("Login com credenciais válidas deve retornar 200 com tokens")
    void login_withValidCredentials_shouldReturn200() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("username", "admin", "password", "admin123"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").isNotEmpty())
          .andExpect(jsonPath("$.refreshToken").isNotEmpty())
          .andExpect(jsonPath("$.username").value("admin"))
          .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Login com credenciais inválidas deve retornar 400")
    void login_withInvalidCredentials_shouldReturn400() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("username", "admin", "password", "wrongpassword"))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    @DisplayName("Login com usuário inexistente deve retornar 400")
    void login_withUnknownUser_shouldReturn400() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("username", "nonexistent", "password", "password"))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    @DisplayName("Login sem username deve retornar 400")
    void login_withoutUsername_shouldReturn400() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("password", "password"))))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /api/auth/register")
  class RegisterTests {

    @Test
    @DisplayName("Registro como ADMIN deve retornar 201")
    void register_asAdmin_shouldReturn201() throws Exception {
      String uniqueUsername = "newuser_" + System.currentTimeMillis();
      mockMvc
          .perform(
              post("/api/auth/register")
                  .header("Authorization", "Bearer " + adminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "username", uniqueUsername,
                              "password", "password123",
                              "role", "OPERATOR"))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.accessToken").isNotEmpty())
          .andExpect(jsonPath("$.username").value(uniqueUsername))
          .andExpect(jsonPath("$.role").value("OPERATOR"));
    }

    @Test
    @DisplayName("Registro sem autenticação deve retornar 401/403")
    void register_withoutAuth_shouldReturn401Or403() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/register")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "username", "newuser",
                              "password", "password123",
                              "role", "OPERATOR"))))
          .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Registro com username duplicado deve retornar 400")
    void register_withDuplicateUsername_shouldReturn400() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/register")
                  .header("Authorization", "Bearer " + adminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "username", "admin",
                              "password", "password123",
                              "role", "OPERATOR"))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }
  }

  @Nested
  @DisplayName("POST /api/auth/refresh")
  class RefreshTokenTests {

    @Test
    @DisplayName("Refresh com token válido deve retornar 200")
    void refresh_withValidToken_shouldReturn200() throws Exception {
      String loginResponse =
          mockMvc
              .perform(
                  post("/api/auth/login")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          objectMapper.writeValueAsString(
                              Map.of("username", "admin", "password", "admin123"))))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

      mockMvc
          .perform(
              post("/api/auth/refresh")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").isNotEmpty())
          .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Refresh com token inválido deve retornar 400")
    void refresh_withInvalidToken_shouldReturn400() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/refresh")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of("refreshToken", "invalid-token-here"))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }
  }
}
