package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.UserManagementUseCasePort;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.PlanConfig;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserManagementUseCaseImpl implements UserManagementUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(UserManagementUseCaseImpl.class);

  private final UserRepositoryPort userRepository;
  private final TenantRepositoryPort tenantRepository;
  private final RefreshTokenRepositoryPort refreshTokenRepository;
  private final TokenBlacklistService tokenBlacklistService;
  private final PasswordEncoder passwordEncoder;

  public UserManagementUseCaseImpl(
      UserRepositoryPort userRepository,
      TenantRepositoryPort tenantRepository,
      RefreshTokenRepositoryPort refreshTokenRepository,
      TokenBlacklistService tokenBlacklistService,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.tenantRepository = tenantRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenBlacklistService = tokenBlacklistService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public List<User> listUsers(String tenantId, Role actorRole) {
    requireTenantManager(tenantId, actorRole);
    return userRepository.findAllByTenantId(tenantId).stream()
        .sorted(
            Comparator.comparing(
                user -> displayName(user).toLowerCase(Locale.ROOT), Comparator.naturalOrder()))
        .toList();
  }

  @Override
  public User getUser(String userId, String tenantId, Role actorRole) {
    requireTenantManager(tenantId, actorRole);
    return findScopedUser(userId, tenantId);
  }

  @Override
  public User createUser(
      String name, String email, String password, Role role, String tenantId, Role actorRole) {
    requireTenantManager(tenantId, actorRole);
    requireAssignableRole(actorRole, role);

    Tenant tenant = getActiveTenant(tenantId);
    String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    if (userRepository.existsByUsernameAndTenantId(normalizedEmail, tenantId)) {
      throw new BusinessException("Já existe um usuário com este e-mail na empresa.");
    }
    validateOperatorLimit(role, tenant);

    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    User user = new User();
    user.setName(name.trim());
    user.setEmail(normalizedEmail);
    // O fluxo atual autentica por username. Para novas contas, o e-mail é a credencial.
    user.setUsername(normalizedEmail);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    user.setTenantId(tenantId);
    user.setActive(true);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    User saved = userRepository.save(user);
    if (log.isInfoEnabled()) {
      log.info(
          "Usuário criado pelo gerenciamento: userId={} role={} tenantId={}",
          LogSanitizer.maskId(saved.getId()),
          saved.getRole(),
          LogSanitizer.maskId(tenantId));
    }
    return saved;
  }

  @Override
  public User updateStatus(
      String userId, boolean active, String actorUserId, String tenantId, Role actorRole) {
    requireTenantManager(tenantId, actorRole);
    User target = findScopedUser(userId, tenantId);
    requireCanManageTarget(actorUserId, actorRole, target);

    if (target.isActive() == active) {
      return target;
    }

    target.setActive(active);
    target.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    User saved = userRepository.save(target);
    revokeSessions(saved);
    if (log.isInfoEnabled()) {
      log.info(
          "Status de usuário alterado: userId={} active={} tenantId={}",
          LogSanitizer.maskId(saved.getId()),
          active,
          LogSanitizer.maskId(tenantId));
    }
    return saved;
  }

  @Override
  public User updateRole(
      String userId, Role role, String actorUserId, String tenantId, Role actorRole) {
    requireTenantManager(tenantId, actorRole);
    User target = findScopedUser(userId, tenantId);
    requireCanManageTarget(actorUserId, actorRole, target);
    requireAssignableRole(actorRole, role);

    if (target.getRole() == role) {
      return target;
    }
    if (role == Role.OPERATOR) {
      validateOperatorLimit(role, getActiveTenant(tenantId));
    }

    Role previousRole = target.getRole();
    target.setRole(role);
    target.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    User saved = userRepository.save(target);
    revokeSessions(saved);
    if (log.isInfoEnabled()) {
      log.info(
          "Role de usuário alterada: userId={} previousRole={} newRole={} tenantId={}",
          LogSanitizer.maskId(saved.getId()),
          previousRole,
          role,
          LogSanitizer.maskId(tenantId));
    }
    return saved;
  }

  private void requireTenantManager(String tenantId, Role actorRole) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new AccessDeniedException("Administrador sem tenant associado.");
    }
    if (actorRole != Role.ADMIN && actorRole != Role.TENANT_ADMIN) {
      throw new AccessDeniedException("Role sem permissão para gerenciar usuários.");
    }
  }

  private User findScopedUser(String userId, String tenantId) {
    return userRepository
        .findByIdAndTenantId(userId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", userId));
  }

  private void requireCanManageTarget(String actorUserId, Role actorRole, User target) {
    if (target.getId().equals(actorUserId)) {
      throw new AccessDeniedException("Não é permitido administrar a própria conta por esta tela.");
    }
    boolean allowed =
        actorRole == Role.ADMIN
            ? target.getRole() == Role.TENANT_ADMIN || target.getRole() == Role.OPERATOR
            : target.getRole() == Role.OPERATOR;
    if (!allowed) {
      throw new AccessDeniedException("Não é permitido administrar um usuário deste nível.");
    }
  }

  private void requireAssignableRole(Role actorRole, Role requestedRole) {
    boolean allowed =
        actorRole == Role.ADMIN
            ? requestedRole == Role.TENANT_ADMIN || requestedRole == Role.OPERATOR
            : requestedRole == Role.OPERATOR;
    if (!allowed) {
      throw new AccessDeniedException("Não é permitido atribuir a role solicitada.");
    }
  }

  private Tenant getActiveTenant(String tenantId) {
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    if (tenant.getStatus() != TenantStatus.ACTIVE) {
      throw new BusinessException("Somente tenants ativos podem gerenciar usuários.");
    }
    return tenant;
  }

  private void validateOperatorLimit(Role role, Tenant tenant) {
    if (role != Role.OPERATOR) {
      return;
    }
    PlanConfig.PlanLimits limits;
    try {
      limits = PlanConfig.getLimits(tenant.getPlanId());
    } catch (IllegalArgumentException exception) {
      throw new BusinessException("O tenant não possui um plano válido.");
    }
    long currentOperators = userRepository.countByTenantIdAndRole(tenant.getId(), Role.OPERATOR);
    if (currentOperators >= limits.maxOperators()) {
      throw new BusinessException(
          "Limite de operadores do plano "
              + tenant.getPlanId()
              + " atingido ("
              + limits.maxOperators()
              + "). Faça upgrade para adicionar outro operador.");
    }
  }

  private void revokeSessions(User user) {
    refreshTokenRepository.deleteByUserId(user.getId());
    tokenBlacklistService.revokeAllForUser(user.getId());
  }

  private String displayName(User user) {
    return user.getName() == null || user.getName().isBlank() ? user.getUsername() : user.getName();
  }
}
