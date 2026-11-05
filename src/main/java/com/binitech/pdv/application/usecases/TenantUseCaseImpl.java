package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.TenantUseCasePort;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.PlanConfig;
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
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantUseCaseImpl implements TenantUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(TenantUseCaseImpl.class);
  private static final String CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$!";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final boolean autoApprove;
  private final TenantRepositoryPort tenantRepository;
  private final UserRepositoryPort userRepository;
  private final AuthenticationGateway authentication;
  private final EmailServicePort emailService;

  public TenantUseCaseImpl(
      TenantRepositoryPort tenantRepository,
      UserRepositoryPort userRepository,
      AuthenticationGateway authentication,
      EmailServicePort emailService) {
    this(tenantRepository, userRepository, authentication, emailService, true);
  }

  public TenantUseCaseImpl(
      TenantRepositoryPort tenantRepository,
      UserRepositoryPort userRepository,
      AuthenticationGateway authentication,
      EmailServicePort emailService,
      boolean autoApprove) {
    this.autoApprove = autoApprove;
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.authentication = authentication;
    this.emailService = emailService;
  }

  @Override
  public SignupResult signUp(SelfSignup signup) {
    String email = signup.email() == null ? "" : signup.email().trim().toLowerCase(Locale.ROOT);
    if (DisposableEmail.isDisposable(email)) {
      throw new BusinessException("Use um e-mail de verdade para criar a conta.");
    }
    if (tenantRepository.findByBillingEmail(email).isPresent()
        || userRepository.existsByUsername(email)) {
      throw new BusinessException(
          "Já existe uma conta com esse e-mail. Entre ou use Esqueci minha senha.");
    }
    Tenant tenant =
        createTenant(signup.name(), signup.slug(), signup.planId(), email, signup.terms());
    tenant.setContactPhone(digitsOrNull(signup.phone()));
    if (!autoApprove) {
      tenantRepository.save(tenant);
      return new SignupResult(tenant, false, null);
    }
    tenant.startTrial(LocalDate.now(ZoneId.systemDefault()));
    tenant.setStatus(TenantStatus.ACTIVE);
    tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    Tenant saved = tenantRepository.save(tenant);
    provisionSelfServiceAdmin(saved, email, signup.password());
    if (log.isInfoEnabled()) {
      log.info(
          "Tenant ativado no cadastro: tenantId={} teste até {}",
          LogSanitizer.maskId(saved.getId()),
          saved.getTrialEndsAt());
    }
    return new SignupResult(saved, true, email);
  }

  private void provisionSelfServiceAdmin(Tenant tenant, String email, String password) {
    User adminUser = new User();
    adminUser.setUsername(email);
    adminUser.setRole(Role.TENANT_ADMIN);
    adminUser.setTenantId(tenant.getId());
    adminUser.setName(tenant.getName());
    adminUser.setEmail(email);
    adminUser.setActive(true);
    adminUser.setCreatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    adminUser.setUpdatedAt(adminUser.getCreatedAt());
    new IdentityProvisioningUseCase(userRepository, authentication).provision(adminUser, password);
    try {
      emailService.sendWelcomeEmail(email, tenant.getName(), email);
    } catch (Exception e) {
      log.error(
          "Falha ao enviar boas-vindas para tenantId={}: {}",
          LogSanitizer.maskId(tenant.getId()),
          e.getMessage());
    }
  }

  private static String digitsOrNull(String value) {
    if (value == null) {
      return null;
    }
    String digits = value.replaceAll("\\D", "");
    return digits.isEmpty() ? null : digits;
  }

  @Override
  public Tenant createTenant(
      String name, String slug, String planId, String billingEmail, TermsAcceptance terms) {
    if (terms == null || terms.version() == null || terms.version().isBlank()) {
      throw new BusinessException("É necessário aceitar os Termos de uso.");
    }
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
    tenant.setTermsAcceptedAt(now);
    tenant.setTermsVersion(terms.version());
    tenant.setTermsAcceptedIp(terms.ip());

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
    provisionTenantAdmin(tenant);

    if (!PlanConfig.isFree(tenant.getPlanId())) {
      tenant.startTrial(LocalDate.now(ZoneId.systemDefault()));
    }
    tenant.setStatus(TenantStatus.ACTIVE);
    tenant.setBlockedAt(null);
    tenant.setBlockReason(null);
    tenant.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
    Tenant saved = tenantRepository.save(tenant);
    if (log.isInfoEnabled()) {
      log.info("Tenant aprovado: tenantId={}", LogSanitizer.maskId(saved.getId()));
    }

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

      adminUser.setRole(Role.TENANT_ADMIN);
      adminUser.setTenantId(tenant.getId());
      adminUser.setName(tenant.getName());
      adminUser.setEmail(tenant.getBillingEmail());
      adminUser.setActive(true);
      adminUser.setCreatedAt(LocalDateTime.now(ZoneId.systemDefault()));
      adminUser.setUpdatedAt(adminUser.getCreatedAt());
      new IdentityProvisioningUseCase(userRepository, authentication)
          .provision(adminUser, tempPassword);
      log.info("Usuário TENANT_ADMIN criado para tenantId={}", LogSanitizer.maskId(tenant.getId()));

      sendApprovalEmailQuietly(tenant, tenantAdminUsername, tempPassword);
    } catch (Exception e) {
      log.error(
          "Falha ao provisionar admin do tenant tenantId={}: {}",
          LogSanitizer.maskId(tenant.getId()),
          e.getMessage());
      throw new com.binitech.pdv.domain.exception.AuthenticationUnavailableException();
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
