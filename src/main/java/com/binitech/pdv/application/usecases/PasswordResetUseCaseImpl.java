package com.binitech.pdv.application.usecases;

import com.binitech.pdv.adapters.outbound.persistence.document.PasswordResetTokenDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataPasswordResetTokenRepository;
import com.binitech.pdv.application.ports.inbound.PasswordResetUseCasePort;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
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
  private final String frontendUrl;

  public PasswordResetUseCaseImpl(
      UserRepositoryPort userRepository,
      TenantRepositoryPort tenantRepository,
      SpringDataPasswordResetTokenRepository resetTokenRepository,
      PasswordEncoder passwordEncoder,
      EmailServicePort emailService,
      String frontendUrl) {
    this.userRepository = userRepository;
    this.tenantRepository = tenantRepository;
    this.resetTokenRepository = resetTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
    this.frontendUrl = frontendUrl;
  }

  @Override
  public void requestReset(String tenantSlug, String username) {
    if (isBlank(tenantSlug) || isBlank(username)) {
      return;
    }
    Optional<Tenant> tenantOpt = tenantRepository.findBySlug(tenantSlug.trim().toLowerCase());
    if (tenantOpt.isEmpty()) {
      log.info("Reset solicitado para loja inexistente: slug={}", tenantSlug);
      return;
    }
    Tenant tenant = tenantOpt.get();

    Optional<User> userOpt =
        userRepository.findByUsernameAndTenantId(username.trim(), tenant.getId());
    if (userOpt.isEmpty()) {
      log.info(
          "Reset solicitado para usuário inexistente no tenant={}",
          LogSanitizer.maskId(tenant.getId()));
      return;
    }
    User user = userOpt.get();

    if (user.getRole() == Role.SUPER_ADMIN || isBlank(tenant.getBillingEmail())) {
      return;
    }

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
    log.info("Senha redefinida com sucesso para userId={}", LogSanitizer.maskId(user.getId()));
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
