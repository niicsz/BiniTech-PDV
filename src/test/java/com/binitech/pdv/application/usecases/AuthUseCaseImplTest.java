package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.AuthResult;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.RefreshToken;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.Enum.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private TokenBlacklistService tokenBlacklistService;
  @Mock private PasswordEncoder passwordEncoder;

  private AuthUseCaseImpl authUseCase;

  @BeforeEach
  void setUp() {
    authUseCase =
        new AuthUseCaseImpl(
            userRepository,
            refreshTokenRepository,
            jwtTokenProvider,
            tokenBlacklistService,
            passwordEncoder,
            86400000L,
            "test-dummy-hash");
  }

  @Nested
  @DisplayName("Login")
  class LoginTests {

    @Test
    @DisplayName("Login com credenciais válidas (tenant) deve retornar tokens")
    void login_withValidCredentials_shouldReturnTokens() {
      User user = new User("user1", "admin", "encodedPass", Role.TENANT_ADMIN, "tenant1");
      when(userRepository.findByUsernameAndTenantId("admin", "tenant1"))
          .thenReturn(Optional.of(user));
      when(passwordEncoder.matches("password", "encodedPass")).thenReturn(true);
      when(jwtTokenProvider.generateAccessToken("user1", "admin", "TENANT_ADMIN", "tenant1"))
          .thenReturn("access-token");
      when(refreshTokenRepository.save(any(RefreshToken.class)))
          .thenAnswer(
              inv -> {
                RefreshToken rt = inv.getArgument(0);
                rt.setId("rt1");
                return rt;
              });

      AuthResult result = authUseCase.login("admin", "password", "tenant1");

      assertNotNull(result);
      assertEquals("access-token", result.accessToken());
      assertNotNull(result.refreshToken());
      assertEquals("admin", result.username());
      assertEquals("TENANT_ADMIN", result.role());
      assertEquals("tenant1", result.tenantId());
      verify(refreshTokenRepository).deleteByUserIdAndTenantId("user1", "tenant1");
    }

    @Test
    @DisplayName("Login do super admin (sem tenant) deve usar lookup com tenantId nulo")
    void login_superAdmin_shouldUseTenantIdIsNull() {
      User user = new User("sa1", "root", "encodedPass", Role.SUPER_ADMIN, null);
      when(userRepository.findByUsernameAndTenantIdIsNull("root")).thenReturn(Optional.of(user));
      when(passwordEncoder.matches("password", "encodedPass")).thenReturn(true);
      when(jwtTokenProvider.generateAccessToken("sa1", "root", "SUPER_ADMIN", null))
          .thenReturn("access-token");
      when(refreshTokenRepository.save(any(RefreshToken.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      AuthResult result = authUseCase.login("root", "password", null);

      assertEquals("access-token", result.accessToken());
      assertEquals("SUPER_ADMIN", result.role());
      assertNull(result.tenantId());
    }

    @Test
    @DisplayName("Login com usuário inexistente deve lançar BusinessException")
    void login_withUnknownUser_shouldThrowException() {
      when(userRepository.findByUsernameAndTenantId("unknown", "tenant1"))
          .thenReturn(Optional.empty());

      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> authUseCase.login("unknown", "pass", "tenant1"));

      assertTrue(exception.getMessage().contains("Credenciais inválidas"));
    }

    @Test
    @DisplayName("Login com senha incorreta deve lançar BusinessException")
    void login_withWrongPassword_shouldThrowException() {
      User user = new User("user1", "admin", "encodedPass", Role.TENANT_ADMIN, "tenant1");
      when(userRepository.findByUsernameAndTenantId("admin", "tenant1"))
          .thenReturn(Optional.of(user));
      when(passwordEncoder.matches("wrongpass", "encodedPass")).thenReturn(false);

      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> authUseCase.login("admin", "wrongpass", "tenant1"));

      assertTrue(exception.getMessage().contains("Credenciais inválidas"));
    }
  }

  @Nested
  @DisplayName("Register")
  class RegisterTests {

    @Test
    @DisplayName("Registro com dados válidos deve criar usuário e retornar tokens")
    void register_withValidData_shouldCreateUserAndReturnTokens() {
      when(userRepository.existsByUsernameAndTenantId("newuser", "tenant1")).thenReturn(false);
      when(passwordEncoder.encode("password")).thenReturn("encodedPass");
      when(userRepository.save(any(User.class)))
          .thenAnswer(
              inv -> {
                User u = inv.getArgument(0);
                u.setId("new-id");
                return u;
              });
      when(jwtTokenProvider.generateAccessToken("new-id", "newuser", "OPERATOR", "tenant1"))
          .thenReturn("access-token");
      when(refreshTokenRepository.save(any(RefreshToken.class)))
          .thenAnswer(
              inv -> {
                RefreshToken rt = inv.getArgument(0);
                rt.setId("rt1");
                return rt;
              });

      AuthResult result = authUseCase.register("newuser", "password", Role.OPERATOR, "tenant1");

      assertNotNull(result);
      assertEquals("access-token", result.accessToken());
      assertEquals("newuser", result.username());
      assertEquals("OPERATOR", result.role());
      assertEquals("tenant1", result.tenantId());
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
  @DisplayName("Refresh Token")
  class RefreshTokenTests {

    @Test
    @DisplayName("Refresh token válido deve rotacionar tokens")
    void refreshToken_withValidToken_shouldRotateTokens() {
      RefreshToken rt =
          new RefreshToken(
              "rt1", "valid-token", "user1", "tenant1", Instant.now().plus(1, ChronoUnit.HOURS));
      User user = new User("user1", "admin", "pass", Role.TENANT_ADMIN, "tenant1");

      when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(rt));
      when(userRepository.findById("user1")).thenReturn(Optional.of(user));
      when(jwtTokenProvider.generateAccessToken("user1", "admin", "TENANT_ADMIN", "tenant1"))
          .thenReturn("new-access-token");
      when(refreshTokenRepository.save(any(RefreshToken.class)))
          .thenAnswer(
              inv -> {
                RefreshToken newRt = inv.getArgument(0);
                newRt.setId("rt2");
                return newRt;
              });

      AuthResult result = authUseCase.refreshToken("valid-token");

      assertNotNull(result);
      assertEquals("new-access-token", result.accessToken());
      assertEquals("admin", result.username());
      verify(refreshTokenRepository).deleteByUserIdAndTenantId("user1", "tenant1");
    }

    @Test
    @DisplayName("Refresh token inválido deve lançar BusinessException")
    void refreshToken_withInvalidToken_shouldThrowException() {
      when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

      BusinessException exception =
          assertThrows(BusinessException.class, () -> authUseCase.refreshToken("invalid-token"));

      assertTrue(exception.getMessage().contains("Refresh token inválido"));
    }

    @Test
    @DisplayName("Refresh token expirado deve deletar e lançar BusinessException")
    void refreshToken_withExpiredToken_shouldDeleteAndThrowException() {
      RefreshToken rt =
          new RefreshToken(
              "rt1", "expired-token", "user1", "tenant1", Instant.now().minus(1, ChronoUnit.HOURS));
      when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(rt));

      BusinessException exception =
          assertThrows(BusinessException.class, () -> authUseCase.refreshToken("expired-token"));

      assertTrue(exception.getMessage().contains("expirado"));
      verify(refreshTokenRepository).deleteByUserIdAndTenantId("user1", "tenant1");
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
      when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
      when(passwordEncoder.encode("newPass")).thenReturn("newHash");

      authUseCase.changePassword("user1", "oldPass", "newPass");

      assertEquals("newHash", user.getPassword());
      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Senha atual incorreta deve lançar BusinessException")
    void changePassword_withWrongCurrent_shouldThrow() {
      User user = new User("user1", "op", "oldHash", Role.OPERATOR, "tenant1");
      when(userRepository.findById("user1")).thenReturn(Optional.of(user));
      when(passwordEncoder.matches("wrong", "oldHash")).thenReturn(false);

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
}
