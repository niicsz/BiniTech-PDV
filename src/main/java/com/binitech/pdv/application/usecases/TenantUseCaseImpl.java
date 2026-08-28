package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.TenantUseCasePort;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TenantUseCaseImpl implements TenantUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(TenantUseCaseImpl.class);
  private static final String CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$!";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final TenantRepositoryPort tenantRepository;
  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailServicePort emailService;

  public TenantUseCaseImpl(
      TenantRepositoryPort tenantRepository,
      UserRepositoryPort userRepository,
      PasswordEncoder passwordEncoder,
      EmailServicePort emailService) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
  }

  @Override
  public Tenant createTenant(String name, String slug, String planId, String billingEmail) {
    if (tenantRepository.existsBySlug(slug)) {
      if (log.isWarnEnabled()) {
        log.warn("Tentativa de criar tenant com slug duplicado: {}", slug);
      }
      throw new BusinessException("Já existe um tenant com o slug: " + slug);
    }

    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    Tenant tenant = new Tenant();
    tenant.setName(name);
    tenant.setSlug(slug);
    tenant.setStatus(TenantStatus.PENDING_APPROVAL);
    tenant.setPlanId(planId);
    tenant.setBillingEmail(billingEmail);
    tenant.setCreatedAt(now);
    tenant.setUpdatedAt(now);

    Tenant saved = tenantRepository.save(tenant);
    if (log.isInfoEnabled()) {
      log.info(
          "Tenant criado: tenantId={} slug={} status={}",
          LogSanitizer.maskId(saved.getId()),
          saved.getSlug(),
          saved.getStatus());
    }
    return saved;
  }

  @Override
  public Tenant getTenantById(String id) {
    return tenantRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", id));
  }

  @Override
  public List<User> getUsersByTenant(String tenantId) {
    // Garante que o tenant existe antes de listar (lança 404 caso contrário).
    getTenantById(tenantId);
    return userRepository.findAllByTenantId(tenantId);
  }

  @Override
  public Tenant getTenantBySlug(String slug) {
    return tenantRepository
        .findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Tenant", "slug", slug));
  }

  @Override
  public List<Tenant> getAllTenants() {
    return tenantRepository.findAll();
  }

  @Override
  public List<Tenant> getTenantsByStatus(TenantStatus status) {
    return tenantRepository.findAllByStatus(status);
  }

  @Override
  public Tenant approveTenant(String tenantId) {
    Tenant tenant = getTenantById(tenantId);
    requirePendingApproval(tenant);

    tenant.setStatus(TenantStatus.ACTIVE);
    tenant.setBlockedAt(null);
    tenant.setBlockReason(null);
    tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    Tenant saved = tenantRepository.save(tenant);
    if (log.isInfoEnabled()) {
      log.info("Tenant aprovado: tenantId={}", LogSanitizer.maskId(saved.getId()));
    }

    provisionTenantAdmin(saved);

    return saved;
  }

  @Override
  public Tenant rejectTenant(String tenantId, String reason) {
    Tenant tenant = getTenantById(tenantId);
    requirePendingApproval(tenant);

    tenant.setStatus(TenantStatus.REJECTED);
    tenant.setBlockedAt(null);
    tenant.setBlockReason(reason);
    tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    Tenant saved = tenantRepository.save(tenant);
    if (log.isInfoEnabled()) {
      log.info("Tenant reprovado: tenantId={}", LogSanitizer.maskId(saved.getId()));
    }
    return saved;
  }

  @Override
  public Tenant blockTenant(String tenantId, String reason) {
    Tenant tenant = getTenantById(tenantId);
    tenant.setStatus(TenantStatus.BLOCKED);
    tenant.setBlockedAt(LocalDate.now(ZoneId.systemDefault()));
    tenant.setBlockReason(reason);
    tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    Tenant saved = tenantRepository.save(tenant);
    if (log.isInfoEnabled()) {
      log.info("Tenant bloqueado: tenantId={}", LogSanitizer.maskId(saved.getId()));
    }
    return saved;
  }

  @Override
  public Tenant updateTenantStatus(String tenantId, TenantStatus newStatus) {
    Tenant tenant = getTenantById(tenantId);
    tenant.setStatus(newStatus);
    if (newStatus == TenantStatus.ACTIVE) {
      tenant.setBlockedAt(null);
      tenant.setBlockReason(null);
    }
    if (newStatus == TenantStatus.BLOCKED && tenant.getBlockedAt() == null) {
      tenant.setBlockedAt(LocalDate.now(ZoneId.systemDefault()));
    }
    tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    Tenant saved = tenantRepository.save(tenant);
    if (log.isInfoEnabled()) {
      log.info(
          "Status do tenant atualizado: tenantId={} status={}",
          LogSanitizer.maskId(saved.getId()),
          saved.getStatus());
    }
    return saved;
  }

  private void provisionTenantAdmin(Tenant tenant) {
    try {
      String tenantAdminUsername = tenant.getSlug();
      if (userRepository.existsByUsernameAndTenantId(tenantAdminUsername, tenant.getId())) {
        log.info(
            "Usuário admin já existe para tenantId={}, pulando criação.",
            LogSanitizer.maskId(tenant.getId()));
        return;
      }
      String tempPassword = generateTempPassword();
      User adminUser = new User();
      adminUser.setUsername(tenantAdminUsername);
      adminUser.setPassword(passwordEncoder.encode(tempPassword));
      adminUser.setRole(Role.TENANT_ADMIN);
      adminUser.setTenantId(tenant.getId());
      userRepository.save(adminUser);
      log.info("Usuário TENANT_ADMIN criado para tenantId={}", LogSanitizer.maskId(tenant.getId()));

      sendApprovalEmailQuietly(tenant, tenantAdminUsername, tempPassword);
    } catch (Exception e) {
      log.error(
          "Falha ao provisionar admin do tenant tenantId={}: {}",
          LogSanitizer.maskId(tenant.getId()),
          e.getMessage());
    }
  }

  private void requirePendingApproval(Tenant tenant) {
    if (tenant.getStatus() != TenantStatus.PENDING_APPROVAL) {
      throw new BusinessException("Somente solicitações pendentes podem ser avaliadas.");
    }
  }

  private void sendApprovalEmailQuietly(Tenant tenant, String username, String tempPassword) {
    try {
      emailService.sendTenantApprovalEmail(
          tenant.getBillingEmail(), tenant.getName(), tenant.getSlug(), username, tempPassword);
    } catch (Exception e) {
      log.error(
          "Falha ao enviar e-mail de boas-vindas para tenantId={}: {}",
          LogSanitizer.maskId(tenant.getId()),
          e.getMessage());
    }
  }

  private String generateTempPassword() {
    StringBuilder sb = new StringBuilder(12);
    for (int i = 0; i < 12; i++) {
      sb.append(CHARS.charAt(SECURE_RANDOM.nextInt(CHARS.length())));
    }
    return sb.toString();
  }
}
