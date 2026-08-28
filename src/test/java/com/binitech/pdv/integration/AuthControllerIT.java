package com.binitech.pdv.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
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
  @Autowired private TenantRepositoryPort tenantRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private MongoTemplate mongoTemplate;

  private String adminToken;
  private String tenantAdminToken;
  private String superAdminToken;
  private User adminUser;

  @BeforeEach
  void setUp() {
    mongoTemplate.getDb().getCollection("users").drop();
    mongoTemplate.getDb().getCollection("refresh_tokens").drop();
    mongoTemplate.getDb().getCollection("tenants").drop();

    createTenant("tenant-admin", "starter");
    createTenant("tenant-a", "starter");

    adminUser =
        userRepository
            .findByUsername("admin")
            .orElseGet(
                () -> {
                  User user = new User();
                  user.setUsername("admin");
                  user.setPassword(passwordEncoder.encode("admin123"));
                  user.setRole(Role.ADMIN);
                  user.setTenantId("tenant-admin");
                  return userRepository.save(user);
                });

    User tenantAdmin = new User();
    tenantAdmin.setUsername("tenant_admin");
    tenantAdmin.setPassword(passwordEncoder.encode("tenantadmin123"));
    tenantAdmin.setRole(Role.TENANT_ADMIN);
    tenantAdmin.setTenantId("tenant-a");
    tenantAdmin = userRepository.save(tenantAdmin);

    User superAdmin = new User();
    superAdmin.setUsername("super_admin_test");
    superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
    superAdmin.setRole(Role.SUPER_ADMIN);
    superAdmin = userRepository.save(superAdmin);

    adminToken =
        jwtTokenProvider.generateAccessToken(
            adminUser.getId(),
            adminUser.getUsername(),
            adminUser.getRole().name(),
            adminUser.getTenantId());
    tenantAdminToken =
        jwtTokenProvider.generateAccessToken(
            tenantAdmin.getId(),
            tenantAdmin.getUsername(),
            tenantAdmin.getRole().name(),
            tenantAdmin.getTenantId());
    superAdminToken =
        jwtTokenProvider.generateAccessToken(
            superAdmin.getId(),
            superAdmin.getUsername(),
            superAdmin.getRole().name(),
            superAdmin.getTenantId());
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

    @Test
    @DisplayName("TENANT_ADMIN deve ignorar tenantId enviado e criar operador no próprio tenant")
    void register_tenantAdminWithAnotherTenantId_shouldUseOwnTenant() throws Exception {
      String uniqueUsername = "operator_" + System.currentTimeMillis();

      mockMvc
          .perform(
              post("/api/auth/register")
                  .header("Authorization", "Bearer " + tenantAdminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "username", uniqueUsername,
                              "password", "password123",
                              "role", "OPERATOR",
                              "tenantId", "tenant-b"))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.role").value("OPERATOR"))
          .andExpect(jsonPath("$.tenantId").value("tenant-a"));
    }

    @Test
    @DisplayName("Plano Starter não deve permitir mais de um operador")
    void register_starterWithExistingOperator_shouldReturn400() throws Exception {
      User operator = new User();
      operator.setUsername("existing_operator");
      operator.setPassword(passwordEncoder.encode("password123"));
      operator.setRole(Role.OPERATOR);
      operator.setTenantId("tenant-a");
      userRepository.save(operator);

      mockMvc
          .perform(
              post("/api/auth/register")
                  .header("Authorization", "Bearer " + tenantAdminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "username", "second_operator",
                              "password", "password123",
                              "role", "OPERATOR"))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
          .andExpect(
              jsonPath("$.message")
                  .value(org.hamcrest.Matchers.containsString("Limite de operadores")));
    }

    @Test
    @DisplayName("TENANT_ADMIN não deve criar SUPER_ADMIN")
    void register_tenantAdminAsSuperAdmin_shouldReturn403() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/register")
                  .header("Authorization", "Bearer " + tenantAdminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "username", "forbidden_super_" + System.currentTimeMillis(),
                              "password", "password123",
                              "role", "SUPER_ADMIN",
                              "tenantId", "tenant-b"))))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("SUPER_ADMIN deve criar outro SUPER_ADMIN sem tenant")
    void register_superAdminAsSuperAdmin_shouldReturn201WithoutTenant() throws Exception {
      mockMvc
          .perform(
              post("/api/auth/register")
                  .header("Authorization", "Bearer " + superAdminToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          Map.of(
                              "username", "super_" + System.currentTimeMillis(),
                              "password", "password123",
                              "role", "SUPER_ADMIN",
                              "tenantId", "tenant-should-be-ignored"))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
          .andExpect(jsonPath("$.tenantId").doesNotExist());
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

  private void createTenant(String id, String planId) {
    Tenant tenant = new Tenant();
    tenant.setId(id);
    tenant.setName(id);
    tenant.setSlug(id);
    tenant.setPlanId(planId);
    tenant.setStatus(TenantStatus.ACTIVE);
    tenantRepository.save(tenant);
  }
}
