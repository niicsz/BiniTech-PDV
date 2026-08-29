package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.AuthSessionConfig;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.config.PlanConfig;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.RefreshToken;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthUseCaseImpl implements AuthUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(AuthUseCaseImpl.class);

  private final UserRepositoryPort userRepository;
  private final TenantRepositoryPort tenantRepository;
  private final RefreshTokenRepositoryPort refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final TokenBlacklistService tokenBlacklistService;
  private final PasswordEncoder passwordEncoder;
  private final long refreshExpiration;
  private final String dummyPasswordHash;

  public AuthUseCaseImpl(
      UserRepositoryPort userRepository,
      TenantRepositoryPort tenantRepository,
      RefreshTokenRepositoryPort refreshTokenRepository,
      JwtTokenProvider jwtTokenProvider,
      TokenBlacklistService tokenBlacklistService,
      PasswordEncoder passwordEncoder,
      AuthSessionConfig sessionConfig) {
    this.userRepository = userRepository;
    this.tenantRepository = tenantRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.tokenBlacklistService = tokenBlacklistService;
    this.passwordEncoder = passwordEncoder;
    this.refreshExpiration = sessionConfig.refreshExpiration();
    this.dummyPasswordHash = sessionConfig.dummyPasswordHash();
  }

  @Override
  public AuthResult login(String username, String password, String tenantId) {
    if (log.isInfoEnabled()) {
      log.info("Tentativa de login para usuário: {}", LogSanitizer.maskUsername(username));
    }

    Optional<User> userOpt = resolveLoginUser(username, password, tenantId);

    if (userOpt.isEmpty()) {
      if (log.isWarnEnabled()) {
        log.warn("Login falhou para usuário: {}", LogSanitizer.maskUsername(username));
      }
      throw new BusinessException("Credenciais inválidas.");
    }

    User user = userOpt.get();

    String accessToken = generateAccessToken(user);

    refreshTokenRepository.deleteByUserIdAndTenantId(user.getId(), user.getTenantId());
    RefreshToken refreshToken = createRefreshToken(user.getId(), user.getTenantId());

    if (log.isInfoEnabled()) {
      log.info(
          "Login realizado com sucesso: userId={} role={} tenantId={}",
          LogSanitizer.maskId(user.getId()),
          user.getRole(),
          LogSanitizer.maskId(user.getTenantId()));
    }
    return new AuthResult(
        accessToken,
        refreshToken.getToken(),
        user.getUsername(),
        user.getRole().name(),
        user.getTenantId());
  }

  /**
   * Com {@code tenantId}, resolve no tenant informado. Sem {@code tenantId}, busca todas as contas
   * com o username e autentica a única cuja senha confere (super admin ou loja).
   */
  private Optional<User> resolveLoginUser(String username, String password, String tenantId) {
    java.util.List<User> candidates;
    if (tenantId != null && !tenantId.isBlank()) {
      candidates =
          userRepository
              .findByUsernameAndTenantId(username, tenantId)
              .map(java.util.List::of)
              .orElseGet(java.util.List::of);
    } else {
      candidates = userRepository.findAllByUsername(username);
    }

    if (candidates.isEmpty()) {
      performDummyPasswordCheck(password);
      return Optional.empty();
    }

    User matched = null;
    int matchCount = 0;
    for (User candidate : candidates) {
      if (passwordMatches(candidate, password, username)) {
        matched = candidate;
        matchCount++;
      }
    }

    if (matchCount == 1) {
      return Optional.of(matched);
    }
    if (matchCount > 1 && log.isWarnEnabled()) {
      log.warn(
          "Login ambíguo: mais de uma conta com o mesmo username e senha: {}",
          LogSanitizer.maskUsername(username));
    }
    return Optional.empty();
  }

  private boolean passwordMatches(User user, String rawPassword, String username) {
    try {
      return passwordEncoder.matches(rawPassword, user.getPassword());
    } catch (IllegalArgumentException e) {
      log.error(
          "Hash de senha inválido para usuário: {} — o usuário deve ser recriado.",
          LogSanitizer.maskUsername(username));
      return false;
    }
  }

  @Override
  public AuthResult register(String username, String password, Role role, String tenantId) {
    if (log.isInfoEnabled()) {
      log.info(
          "Tentativa de registro de novo usuário: {} role={}",
          LogSanitizer.maskUsername(username),
          role);
    }

    String resolvedTenantId = normalizeTenantId(tenantId);
    validateTenantAssociation(role, resolvedTenantId);

    boolean userExists =
        resolvedTenantId != null
            ? userRepository.existsByUsernameAndTenantId(username, resolvedTenantId)
            : userRepository.existsByUsername(username);
    if (userExists) {
      if (log.isWarnEnabled()) {
        log.warn("Registro falhou - usuário já existe: {}", LogSanitizer.maskUsername(username));
      }
      throw new BusinessException("Usuário já existe com o username: " + username);
    }

    validateOperatorLimit(role, resolvedTenantId);

    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    user.setTenantId(resolvedTenantId);
    User saved = userRepository.save(user);

    String accessToken = generateAccessToken(saved);

    RefreshToken refreshToken = createRefreshToken(saved.getId(), saved.getTenantId());

    if (log.isInfoEnabled()) {
      log.info(
          "Usuário registrado com sucesso: userId={} role={} tenantId={}",
          LogSanitizer.maskId(saved.getId()),
          saved.getRole(),
          LogSanitizer.maskId(saved.getTenantId()));
    }
    return new AuthResult(
        accessToken,
        refreshToken.getToken(),
        saved.getUsername(),
        saved.getRole().name(),
        saved.getTenantId());
  }

  @Override
  public AuthResult refreshToken(String refreshTokenStr) {
    log.info("Tentativa de refresh de token");
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenStr)
            .orElseThrow(
                () -> {
                  log.warn("Refresh token inválido fornecido");
                  return new BusinessException("Refresh token inválido.");
                });

    if (refreshToken.isExpired()) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Refresh token expirado para userId={}", LogSanitizer.maskId(refreshToken.getUserId()));
      }
      refreshTokenRepository.deleteByUserIdAndTenantId(
          refreshToken.getUserId(), refreshToken.getTenantId());
      throw new BusinessException("Refresh token expirado. Faça login novamente.");
    }

    User user =
        userRepository
            .findById(refreshToken.getUserId())
            .orElseThrow(
                () -> {
                  if (log.isErrorEnabled()) {
                    log.error(
                        "Usuário não encontrado durante refresh de token: userId={}",
                        LogSanitizer.maskId(refreshToken.getUserId()));
                  }
                  return new BusinessException("Usuário não encontrado.");
                });

    String accessToken = generateAccessToken(user);

    refreshTokenRepository.deleteByUserIdAndTenantId(user.getId(), user.getTenantId());
    RefreshToken newRefreshToken = createRefreshToken(user.getId(), user.getTenantId());

    if (log.isInfoEnabled()) {
      log.info("Token renovado com sucesso para userId={}", LogSanitizer.maskId(user.getId()));
    }
    return new AuthResult(
        accessToken,
        newRefreshToken.getToken(),
        user.getUsername(),
        user.getRole().name(),
        user.getTenantId());
  }

  @Override
  public void changePassword(String userId, String currentPassword, String newPassword) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "Troca de senha falhou - usuário não encontrado: userId={}",
                      LogSanitizer.maskId(userId));
                  return new BusinessException("Usuário não encontrado.");
                });

    if (user.getRole() == Role.SUPER_ADMIN) {
      throw new BusinessException(
          "A senha do super admin é gerenciada pela configuração do sistema.");
    }

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Troca de senha falhou - senha atual incorreta para userId={}",
            LogSanitizer.maskId(userId));
      }
      throw new BusinessException("Senha atual incorreta.");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    refreshTokenRepository.deleteByUserId(userId);
    tokenBlacklistService.revokeAllForUser(userId);
    if (log.isInfoEnabled()) {
      log.info("Senha alterada com sucesso para userId={}", LogSanitizer.maskId(userId));
    }
  }

  @Override
  public void logout(String accessToken) {
    log.info("Realizando logout e invalidando token");
    tokenBlacklistService.blacklist(accessToken);
    if (jwtTokenProvider.validateToken(accessToken)) {
      String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
      String tenantId = jwtTokenProvider.getTenantIdFromToken(accessToken);
      refreshTokenRepository.deleteByUserIdAndTenantId(userId, tenantId);
      if (log.isInfoEnabled()) {
        log.info("Logout realizado com sucesso para userId={}", LogSanitizer.maskId(userId));
      }
    }
  }

  private void performDummyPasswordCheck(String rawPassword) {
    if (dummyPasswordHash == null || dummyPasswordHash.isBlank()) {
      return;
    }
    try {
      passwordEncoder.matches(rawPassword, dummyPasswordHash);
    } catch (RuntimeException e) {
      log.trace("Verificação fictícia de senha falhou; resultado ignorado.", e);
    }
  }

  private void validateTenantAssociation(Role role, String tenantId) {
    if (role == Role.SUPER_ADMIN) {
      return;
    }
    if (tenantId == null || tenantId.isBlank()) {
      throw new BusinessException("tenantId é obrigatório para usuários do tenant.");
    }
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

  private String normalizeTenantId(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return null;
    }
    return tenantId;
  }

  private RefreshToken createRefreshToken(String userId, String tenantId) {
    RefreshToken rt = new RefreshToken();
    rt.setToken(UUID.randomUUID().toString());
    rt.setUserId(userId);
    rt.setTenantId(tenantId);
    rt.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
    if (log.isDebugEnabled()) {
      log.debug("Refresh token criado para userId={}", LogSanitizer.maskId(userId));
    }
    return refreshTokenRepository.save(rt);
  }

  private String generateAccessToken(User user) {
    long sessionVersion = tokenBlacklistService.getSessionVersion(user.getId());
    return jwtTokenProvider.generateAccessToken(
        user.getId(),
        user.getUsername(),
        user.getRole().name(),
        user.getTenantId(),
        sessionVersion);
  }
}
