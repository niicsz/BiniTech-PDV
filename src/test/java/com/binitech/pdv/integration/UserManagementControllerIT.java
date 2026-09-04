package com.binitech.pdv.integration;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private TenantRepositoryPort tenantRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private MongoTemplate mongoTemplate;

  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  private User admin;
  private User tenantAdmin;
  private User operatorA;
  private User operatorB;
  private String adminToken;
  private String tenantAdminToken;
  private String operatorToken;

  @BeforeEach
  void setUp() {
    mongoTemplate.getDb().getCollection("users").drop();
    mongoTemplate.getDb().getCollection("refresh_tokens").drop();
    mongoTemplate.getDb().getCollection("tenants").drop();

    tenantRepository.save(tenant("tenant-a"));
    tenantRepository.save(tenant("tenant-b"));

    admin = saveUser("admin-a", "admin-a@email.com", "admin123", Role.ADMIN, "tenant-a");
    tenantAdmin =
        saveUser(
            "tenant-admin-a",
            "tenant-admin-a@email.com",
            "tenantadmin123",
            Role.TENANT_ADMIN,
            "tenant-a");
    operatorA =
        saveUser("operator-a", "operator-a@email.com", "operator123", Role.OPERATOR, "tenant-a");
    operatorB =
        saveUser("operator-b", "operator-b@email.com", "operator123", Role.OPERATOR, "tenant-b");

    adminToken = token(admin);
    tenantAdminToken = token(tenantAdmin);
    operatorToken = token(operatorA);
  }

  @Test
  @DisplayName("ADMIN lista somente usuários do próprio tenant")
  void listUsers_asAdmin_shouldReturnOwnTenantOnly() throws Exception {
    mockMvc
        .perform(get("/api/users").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(
            jsonPath("$[*].username", everyItem(is(org.hamcrest.Matchers.not("operator-b")))))
        .andExpect(jsonPath("$[*].active", everyItem(is(true))));
  }

  @Test
  @DisplayName("ID manipulado não permite acesso a usuário de outro tenant")
  void getUser_crossTenantId_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get("/api/users/{id}", operatorB.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  @DisplayName("ID manipulado não permite alterar usuário de outro tenant")
  void updateStatus_crossTenantId_shouldReturn404AndPreserveUser() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/{id}/status", operatorB.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
        .andExpect(status().isNotFound());

    org.junit.jupiter.api.Assertions.assertTrue(
        userRepository.findById(operatorB.getId()).orElseThrow().isActive());
  }

  @Test
  @DisplayName("TENANT_ADMIN desativa e reativa operador autorizado")
  void updateStatus_shouldDeactivateAndReactivateOperator() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/{id}/status", operatorA.getId())
                .header("Authorization", "Bearer " + tenantAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mockMvc
        .perform(get("/api/products").header("Authorization", "Bearer " + operatorToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "username", "operator-a@email.com",
                            "password", "operator123",
                            "tenantId", "tenant-a"))))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            patch("/api/users/{id}/status", operatorA.getId())
                .header("Authorization", "Bearer " + tenantAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "username", "operator-a@email.com",
                            "password", "operator123",
                            "tenantId", "tenant-a"))))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("OPERATOR recebe 403 ao tentar acessar gerenciamento")
  void listUsers_asOperator_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/users").header("Authorization", "Bearer " + operatorToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("ADMIN não consegue promover usuário a SUPER_ADMIN")
  void updateRole_toSuperAdmin_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            patch("/api/users/{id}/role", operatorA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"SUPER_ADMIN\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    User unchanged = userRepository.findById(operatorA.getId()).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(Role.OPERATOR, unchanged.getRole());
  }

  @Test
  @DisplayName("tenantId manipulado na criação é ignorado")
  void createUser_withManipulatedTenantId_shouldUseAuthenticatedTenant() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .header("Authorization", "Bearer " + tenantAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "name", "Pedro",
                            "email", "pedro@email.com",
                            "password", "password123",
                            "role", "OPERATOR",
                            "tenantId", "tenant-b"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("pedro@email.com"));

    User created =
        userRepository.findByUsernameAndTenantId("pedro@email.com", "tenant-a").orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals("tenant-a", created.getTenantId());
    org.junit.jupiter.api.Assertions.assertTrue(
        userRepository.findByUsernameAndTenantId("pedro@email.com", "tenant-b").isEmpty());
  }

  private Tenant tenant(String id) {
    Tenant tenant = new Tenant();
    tenant.setId(id);
    tenant.setName(id);
    tenant.setSlug(id);
    tenant.setPlanId("enterprise");
    tenant.setStatus(TenantStatus.ACTIVE);
    return tenant;
  }

  private User saveUser(String name, String email, String password, Role role, String tenantId) {
    User user = new User();
    user.setName(name);
    user.setEmail(email);
    user.setUsername(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    user.setTenantId(tenantId);
    user.setActive(true);
    return userRepository.save(user);
  }

  private String token(User user) {
    return jwtTokenProvider.generateAccessToken(
        user.getId(), user.getUsername(), user.getRole().name(), user.getTenantId());
  }
}
