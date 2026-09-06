package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.AuthResult;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseImplTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private TenantRepositoryPort tenantRepository;
  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
  @Mock private AuthenticationGateway authentication;
  @Mock private TokenBlacklistService tokenBlacklistService;
  @Mock private PasswordEncoder passwordEncoder;

  private AuthUseCaseImpl authUseCase;

  @BeforeEach
  void setUp() {
    authUseCase = new AuthUseCaseImpl(userRepository, tenantRepository, authentication);
  }

  @Test
  void login_shouldUseSharedAuthenticationService() {
    AuthResult expected = new AuthResult("access", "refresh", "admin", "ADMIN", "tenant1");
    when(authentication.login("admin", "password", "tenant1")).thenReturn(expected);
    stubMembership("access", "admin", Role.ADMIN, "tenant1", "u1");
    assertEquals(expected, authUseCase.login("admin", "password", "tenant1"));
    verifyNoInteractions(passwordEncoder, refreshTokenRepository);
  }

  @Test
  void refreshAndLogout_shouldUseSharedAuthenticationService() {
    AuthResult expected = new AuthResult("access", "new-refresh", "admin", "ADMIN", "tenant1");
    when(authentication.refresh("refresh")).thenReturn(expected);
    stubMembership("access", "admin", Role.ADMIN, "tenant1", "u1");
    assertEquals(expected, authUseCase.refreshToken("refresh"));
    authUseCase.logout("access");
    verify(authentication).logout("access");
    verifyNoInteractions(passwordEncoder, refreshTokenRepository);
  }

  @Nested
  @DisplayName("Register")
  class RegisterTests {

    @Test
    @DisplayName("Registro com dados válidos deve criar usuário e retornar tokens")
    void register_withValidData_shouldCreateUserAndReturnTokens() {
      when(userRepository.existsByUsernameAndTenantId("newuser", "tenant1")).thenReturn(false);
      when(tenantRepository.findById("tenant1")).thenReturn(Optional.of(tenant("starter")));
      when(userRepository.countByTenantIdAndRole("tenant1", Role.OPERATOR)).thenReturn(0L);
      stubMembership("access-token", "newuser", Role.OPERATOR, "tenant1", "new-id");
      when(userRepository.save(any(User.class)))
          .thenAnswer(
              inv -> {
                User u = inv.getArgument(0);
                u.setId("new-id");
                return u;
              });
      when(authentication.login("newuser", "password", "tenant1"))
          .thenReturn(
              new AuthResult("access-token", "refresh-token", "newuser", "OPERATOR", "tenant1"));

      AuthResult result = authUseCase.register("newuser", "password", Role.OPERATOR, "tenant1");

      assertNotNull(result);
      assertEquals("access-token", result.accessToken());
      assertEquals("newuser", result.username());
      assertEquals("OPERATOR", result.role());
      assertEquals("tenant1", result.tenantId());
    }

    @Test
    @DisplayName("Plano Starter deve impedir mais de um operador")
    void register_starterAtOperatorLimit_shouldThrowException() {
      when(userRepository.existsByUsernameAndTenantId("second-operator", "tenant1"))
          .thenReturn(false);
      when(tenantRepository.findById("tenant1")).thenReturn(Optional.of(tenant("starter")));
      when(userRepository.countByTenantIdAndRole("tenant1", Role.OPERATOR)).thenReturn(1L);

      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> authUseCase.register("second-operator", "password", Role.OPERATOR, "tenant1"));

      assertTrue(exception.getMessage().contains("Limite de operadores"));
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tenant reprovado não deve cadastrar operador")
    void register_rejectedTenant_shouldThrowException() {
      Tenant rejectedTenant = tenant("starter");
      rejectedTenant.setStatus(TenantStatus.REJECTED);
      when(userRepository.existsByUsernameAndTenantId("operator", "tenant1")).thenReturn(false);
      when(tenantRepository.findById("tenant1")).thenReturn(Optional.of(rejectedTenant));

      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> authUseCase.register("operator", "password", Role.OPERATOR, "tenant1"));

      assertTrue(exception.getMessage().contains("tenants ativos"));
      verify(userRepository, never()).countByTenantIdAndRole("tenant1", Role.OPERATOR);
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registro de usuário do tenant sem tenantId deve lançar BusinessException")
    void register_withoutTenantId_shouldThrow() {
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> authUseCase.register("newuser", "pass", Role.OPERATOR, null));

      assertTrue(exception.getMessage().contains("tenantId"));
    }

    @Test
    @DisplayName("Registro com username duplicado deve lançar BusinessException")
    void register_withDuplicateUsername_shouldThrowException() {
      when(userRepository.existsByUsernameAndTenantId("existinguser", "tenant1")).thenReturn(true);

      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> authUseCase.register("existinguser", "pass", Role.OPERATOR, "tenant1"));

      assertTrue(exception.getMessage().contains("Usuário já existe"));
    }
  }

  @Nested
  @DisplayName("Change Password")
  class ChangePasswordTests {

    @Test
    @DisplayName("Deve alterar senha com senha atual correta")
    void changePassword_withValidCurrent_shouldUpdate() {
      User user = new User("user1", "op", "oldHash", Role.OPERATOR, "tenant1");
      when(userRepository.findById("user1")).thenReturn(Optional.of(user));

      authUseCase.changePassword("user1", "oldPass", "newPass");

      verify(authentication).changePassword("user1", "oldPass", "newPass");
      verify(userRepository, never()).save(any());
      verifyNoInteractions(passwordEncoder, refreshTokenRepository, tokenBlacklistService);
    }

    @Test
    @DisplayName("Senha atual incorreta deve lançar BusinessException")
    void changePassword_withWrongCurrent_shouldThrow() {
      User user = new User("user1", "op", "oldHash", Role.OPERATOR, "tenant1");
      when(userRepository.findById("user1")).thenReturn(Optional.of(user));
      doThrow(new BusinessException("Invalid credentials"))
          .when(authentication)
          .changePassword("user1", "wrong", "newPass");

      assertThrows(
          BusinessException.class, () -> authUseCase.changePassword("user1", "wrong", "newPass"));
      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Super admin não pode alterar senha por aqui")
    void changePassword_superAdmin_shouldThrow() {
      User user = new User("sa1", "root", "hash", Role.SUPER_ADMIN, null);
      when(userRepository.findById("sa1")).thenReturn(Optional.of(user));

      assertThrows(
          BusinessException.class, () -> authUseCase.changePassword("sa1", "x", "newPass"));
      verify(userRepository, never()).save(any());
    }
  }

  private void stubMembership(
      String token, String username, Role role, String tenantId, String id) {
    when(authentication.session(token))
        .thenReturn(new AuthenticationGateway.SessionIdentity(id, username, null, tenantId));
    when(userRepository.findById(id))
        .thenReturn(Optional.of(new User(id, username, null, role, tenantId)));
  }

  private Tenant tenant(String planId) {
    Tenant tenant = new Tenant();
    tenant.setId("tenant1");
    tenant.setPlanId(planId);
    tenant.setStatus(TenantStatus.ACTIVE);
    return tenant;
  }
}
