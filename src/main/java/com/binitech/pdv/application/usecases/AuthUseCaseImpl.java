package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.PlanConfig;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.security.crypto.password.PasswordEncoder;

/** PDV account policies; login and session issuance belong to the authentication service. */
public class AuthUseCaseImpl implements AuthUseCasePort {
  private final UserRepositoryPort userRepository;
  private final TenantRepositoryPort tenantRepository;
  private final RefreshTokenRepositoryPort refreshTokenRepository;
  private final TokenBlacklistService tokenBlacklistService;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationGateway authentication;

  public AuthUseCaseImpl(
      UserRepositoryPort userRepository,
      TenantRepositoryPort tenantRepository,
      RefreshTokenRepositoryPort refreshTokenRepository,
      TokenBlacklistService tokenBlacklistService,
      PasswordEncoder passwordEncoder,
      AuthenticationGateway authentication) {
    this.userRepository = userRepository;
    this.tenantRepository = tenantRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenBlacklistService = tokenBlacklistService;
    this.passwordEncoder = passwordEncoder;
    this.authentication = authentication;
  }

  @Override
  public AuthResult login(String username, String password, String tenantId) {
    return authentication.login(username, password, tenantId);
  }

  @Override
  public AuthResult refreshToken(String refreshToken) {
    return authentication.refresh(refreshToken);
  }

  @Override
  public void logout(String accessToken) {
    authentication.logout(accessToken);
  }

  @Override
  public AuthResult register(String username, String password, Role role, String tenantId) {
    String resolvedTenantId = tenantId == null || tenantId.isBlank() ? null : tenantId;
    if (role != Role.SUPER_ADMIN && resolvedTenantId == null) {
      throw new BusinessException("tenantId é obrigatório para usuários do tenant.");
    }
    boolean userExists =
        resolvedTenantId != null
            ? userRepository.existsByUsernameAndTenantId(username, resolvedTenantId)
            : userRepository.existsByUsername(username);
    if (userExists) {
      throw new BusinessException("Usuário já existe com o username: " + username);
    }
    validateOperatorLimit(role, resolvedTenantId);

    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    user.setTenantId(resolvedTenantId);
    user.setActive(true);
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    User saved = userRepository.save(user);
    return authentication.login(saved.getUsername(), password, saved.getTenantId());
  }

  @Override
  public void changePassword(String userId, String currentPassword, String newPassword) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
    if (user.getRole() == Role.SUPER_ADMIN) {
      throw new BusinessException(
          "A senha do super admin é gerenciada pela configuração do sistema.");
    }
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new BusinessException("Senha atual incorreta.");
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    refreshTokenRepository.deleteByUserId(userId);
    tokenBlacklistService.revokeAllForUser(userId);
  }

  private void validateOperatorLimit(Role role, String tenantId) {
    if (role != Role.OPERATOR) {
      return;
    }
    Tenant tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    if (tenant.getStatus() != TenantStatus.ACTIVE) {
      throw new BusinessException("Somente tenants ativos podem cadastrar operadores.");
    }
    PlanConfig.PlanLimits limits;
    try {
      limits = PlanConfig.getLimits(tenant.getPlanId());
    } catch (IllegalArgumentException exception) {
      throw new BusinessException("O tenant não possui um plano válido.");
    }
    long currentOperators = userRepository.countByTenantIdAndRole(tenantId, Role.OPERATOR);
    if (currentOperators >= limits.maxOperators()) {
      throw new BusinessException(
          "Limite de operadores do plano "
              + tenant.getPlanId()
              + " atingido ("
              + limits.maxOperators()
              + "). Faça upgrade para adicionar outro operador.");
    }
  }
}
