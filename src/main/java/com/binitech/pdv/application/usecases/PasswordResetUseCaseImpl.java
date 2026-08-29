package com.binitech.pdv.application.usecases;

import com.binitech.pdv.adapters.outbound.persistence.document.PasswordResetTokenDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataPasswordResetTokenRepository;
import com.binitech.pdv.application.ports.inbound.PasswordResetUseCasePort;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.PasswordResetConfig;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.Role;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordResetUseCaseImpl implements PasswordResetUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetUseCaseImpl.class);
  private static final long TOKEN_TTL_MILLIS = 60 * 60 * 1000L;

  private final UserRepositoryPort userRepository;
  private final TenantRepositoryPort tenantRepository;
  private final SpringDataPasswordResetTokenRepository resetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailServicePort emailService;
  private final RefreshTokenRepositoryPort refreshTokenRepository;
  private final TokenBlacklistService tokenBlacklistService;
  private final String frontendUrl;

  public PasswordResetUseCaseImpl(
      UserRepositoryPort userRepository,
      TenantRepositoryPort tenantRepository,
      SpringDataPasswordResetTokenRepository resetTokenRepository,
      EmailServicePort emailService,
      PasswordResetConfig config) {
    this.userRepository = userRepository;
    this.tenantRepository = tenantRepository;
    this.resetTokenRepository = resetTokenRepository;
    this.passwordEncoder = config.passwordEncoder();
    this.emailService = emailService;
    this.refreshTokenRepository = config.refreshTokenRepository();
    this.tokenBlacklistService = config.tokenBlacklistService();
    this.frontendUrl = config.frontendUrl();
  }

  @Override
  public void requestReset(String username) {
    if (isBlank(username)) {
      return;
    }

    java.util.List<User> candidates =
        userRepository.findAllByUsername(username.trim()).stream()
            .filter(u -> u.getRole() != Role.SUPER_ADMIN)
            .filter(u -> !isBlank(u.getTenantId()))
            .toList();

    if (candidates.size() != 1) {
      if (candidates.size() > 1) {
        log.info(
            "Reset ambíguo para username={} ({} contas)",
            LogSanitizer.maskUsername(username),
            candidates.size());
      } else {
        log.info(
            "Reset solicitado para usuário inexistente: {}", LogSanitizer.maskUsername(username));
      }
      return;
    }

    User user = candidates.get(0);
    Optional<Tenant> tenantOpt = tenantRepository.findById(user.getTenantId());
    if (tenantOpt.isEmpty() || isBlank(tenantOpt.get().getBillingEmail())) {
      return;
    }
    Tenant tenant = tenantOpt.get();

    try {
      resetTokenRepository.deleteByUserId(user.getId());
      String token = UUID.randomUUID().toString();
      resetTokenRepository.save(
          PasswordResetTokenDocument.builder()
              .token(token)
              .userId(user.getId())
              .expiryDate(Instant.now().plusMillis(TOKEN_TTL_MILLIS))
              .build());

      String resetLink = frontendUrl + "/reset-password?token=" + token;
      emailService.sendPasswordResetEmail(
          tenant.getBillingEmail(), tenant.getName(), user.getUsername(), resetLink);
      log.info(
          "Link de redefinição de senha gerado para userId={} tenantId={}",
          LogSanitizer.maskId(user.getId()),
          LogSanitizer.maskId(tenant.getId()));
    } catch (Exception e) {
      log.error("Falha ao gerar/enviar redefinição de senha: {}", e.getMessage());
    }
  }

  @Override
  public void resetPassword(String token, String newPassword) {
    PasswordResetTokenDocument resetToken =
        resetTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new BusinessException("Link de redefinição inválido."));

    if (resetToken.getExpiryDate() == null || resetToken.getExpiryDate().isBefore(Instant.now())) {
      resetTokenRepository.deleteByUserId(resetToken.getUserId());
      throw new BusinessException("Link de redefinição expirado. Solicite um novo.");
    }

    User user =
        userRepository
            .findById(resetToken.getUserId())
            .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

    if (user.getRole() == Role.SUPER_ADMIN) {
      throw new BusinessException(
          "A senha do super admin é gerenciada pela configuração do sistema.");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    resetTokenRepository.deleteByUserId(user.getId());
    refreshTokenRepository.deleteByUserId(user.getId());
    tokenBlacklistService.revokeAllForUser(user.getId());
    log.info("Senha redefinida com sucesso para userId={}", LogSanitizer.maskId(user.getId()));
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
