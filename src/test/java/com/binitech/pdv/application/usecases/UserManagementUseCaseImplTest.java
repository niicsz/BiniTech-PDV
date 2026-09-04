package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserManagementUseCaseImplTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private TenantRepositoryPort tenantRepository;
  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
  @Mock private TokenBlacklistService tokenBlacklistService;
  @Mock private PasswordEncoder passwordEncoder;

  private UserManagementUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new UserManagementUseCaseImpl(
            userRepository,
            tenantRepository,
            refreshTokenRepository,
            tokenBlacklistService,
            passwordEncoder);
  }

  @Test
  @DisplayName("ADMIN lista somente usuários do tenant informado pelo contexto")
  void listUsers_asAdmin_shouldUseAuthenticatedTenant() {
    when(userRepository.findAllByTenantId("tenant-a"))
        .thenReturn(List.of(user("op-a", Role.OPERATOR, "tenant-a")));

    List<User> result = useCase.listUsers("tenant-a", Role.ADMIN);

    assertEquals(1, result.size());
    assertEquals("tenant-a", result.getFirst().getTenantId());
    verify(userRepository).findAllByTenantId("tenant-a");
    verify(userRepository, never()).findAllByTenantId("tenant-b");
  }

  @Test
  @DisplayName("ID de outro tenant não permite leitura cross-tenant")
  void getUser_withCrossTenantId_shouldReturnNotFound() {
    when(userRepository.findByIdAndTenantId("user-b", "tenant-a")).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> useCase.getUser("user-b", "tenant-a", Role.TENANT_ADMIN));
    verify(userRepository, never()).findById("user-b");
  }

  @Test
  @DisplayName("TENANT_ADMIN desativa operador e revoga todas as sessões")
  void updateStatus_deactivateOperator_shouldRevokeSessions() {
    User operator = user("operator", Role.OPERATOR, "tenant-a");
    when(userRepository.findByIdAndTenantId(operator.getId(), "tenant-a"))
        .thenReturn(Optional.of(operator));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result =
        useCase.updateStatus(
            operator.getId(), false, "tenant-admin", "tenant-a", Role.TENANT_ADMIN);

    assertFalse(result.isActive());
    verify(refreshTokenRepository).deleteByUserId(operator.getId());
    verify(tokenBlacklistService).revokeAllForUser(operator.getId());
  }

  @Test
  @DisplayName("TENANT_ADMIN reativa operador autorizado")
  void updateStatus_reactivateOperator_shouldPersist() {
    User operator = user("operator", Role.OPERATOR, "tenant-a");
    operator.setActive(false);
    when(userRepository.findByIdAndTenantId(operator.getId(), "tenant-a"))
        .thenReturn(Optional.of(operator));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User result =
        useCase.updateStatus(operator.getId(), true, "tenant-admin", "tenant-a", Role.TENANT_ADMIN);

    assertTrue(result.isActive());
    verify(userRepository).save(operator);
  }

  @Test
  @DisplayName("Usuário sem role administrativa recebe acesso negado")
  void listUsers_asOperator_shouldBeForbidden() {
    assertThrows(AccessDeniedException.class, () -> useCase.listUsers("tenant-a", Role.OPERATOR));
    verifyNoInteractions(userRepository);
  }

  @Test
  @DisplayName("TENANT_ADMIN não pode administrar outro TENANT_ADMIN")
  void updateStatus_tenantAdminTarget_shouldBeForbidden() {
    User target = user("other-admin", Role.TENANT_ADMIN, "tenant-a");
    when(userRepository.findByIdAndTenantId(target.getId(), "tenant-a"))
        .thenReturn(Optional.of(target));

    assertThrows(
        AccessDeniedException.class,
        () ->
            useCase.updateStatus(
                target.getId(), false, "tenant-admin", "tenant-a", Role.TENANT_ADMIN));
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("ADMIN não pode atribuir SUPER_ADMIN")
  void updateRole_toSuperAdmin_shouldBeForbidden() {
    User operator = user("operator", Role.OPERATOR, "tenant-a");
    when(userRepository.findByIdAndTenantId(operator.getId(), "tenant-a"))
        .thenReturn(Optional.of(operator));

    assertThrows(
        AccessDeniedException.class,
        () ->
            useCase.updateRole(
                operator.getId(), Role.SUPER_ADMIN, "admin", "tenant-a", Role.ADMIN));
    assertEquals(Role.OPERATOR, operator.getRole());
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("Administrador não pode alterar a própria role")
  void updateRole_self_shouldBeForbidden() {
    User actor = user("admin", Role.ADMIN, "tenant-a");
    when(userRepository.findByIdAndTenantId(actor.getId(), "tenant-a"))
        .thenReturn(Optional.of(actor));

    assertThrows(
        AccessDeniedException.class,
        () ->
            useCase.updateRole(
                actor.getId(), Role.OPERATOR, actor.getId(), "tenant-a", Role.ADMIN));
  }

  @Test
  @DisplayName("Criação sempre associa o usuário ao tenant autenticado")
  void createUser_shouldUseAuthenticatedTenant() {
    when(tenantRepository.findById("tenant-a")).thenReturn(Optional.of(activeTenant("tenant-a")));
    when(userRepository.existsByUsernameAndTenantId("pedro@email.com", "tenant-a"))
        .thenReturn(false);
    when(userRepository.countByTenantIdAndRole("tenant-a", Role.OPERATOR)).thenReturn(0L);
    when(passwordEncoder.encode("password123")).thenReturn("encoded");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User saved = invocation.getArgument(0);
              saved.setId("pedro-id");
              return saved;
            });

    User result =
        useCase.createUser(
            "Pedro",
            "PEDRO@email.com",
            "password123",
            Role.OPERATOR,
            "tenant-a",
            Role.TENANT_ADMIN);

    assertEquals("tenant-a", result.getTenantId());
    assertEquals("pedro@email.com", result.getUsername());
    assertTrue(result.isActive());
  }

  private User user(String id, Role role, String tenantId) {
    return new User(id, id, "hash", role, tenantId);
  }

  private Tenant activeTenant(String id) {
    Tenant tenant = new Tenant();
    tenant.setId(id);
    tenant.setPlanId("enterprise");
    tenant.setStatus(TenantStatus.ACTIVE);
    return tenant;
  }
}
