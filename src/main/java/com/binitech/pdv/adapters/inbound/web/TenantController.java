package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.inbound.TenantUseCasePort;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.TenantStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.text.Normalizer;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class TenantController {

  private final TenantUseCasePort tenantUseCase;
  private final BillingUseCasePort billingUseCase;

  public TenantController(TenantUseCasePort tenantUseCase, BillingUseCasePort billingUseCase) {
    this.tenantUseCase = tenantUseCase;
    this.billingUseCase = billingUseCase;
  }

  @PostMapping("/api/public/tenants")
  public ResponseEntity<TenantDTO> createTenant(@Valid @RequestBody CreateTenantRequest request) {
    Tenant tenant =
        tenantUseCase.createTenant(
            request.name(), generateSlug(request.name()), request.planId(), request.billingEmail());
    return ResponseEntity.status(HttpStatus.CREATED).body(toDto(tenant));
  }

  @GetMapping("/api/public/tenants/slug/{slug}")
  public ResponseEntity<PublicTenantDTO> getTenantBySlug(@PathVariable String slug) {
    Tenant tenant = tenantUseCase.getTenantBySlug(slug);
    return ResponseEntity.ok(
        new PublicTenantDTO(tenant.getId(), tenant.getName(), tenant.getSlug()));
  }

  @GetMapping("/api/admin/tenants")
  public ResponseEntity<List<TenantDTO>> listTenants() {
    return ResponseEntity.ok(tenantUseCase.getAllTenants().stream().map(this::toDto).toList());
  }

  @GetMapping("/api/admin/tenants/{id}")
  public ResponseEntity<TenantDTO> getTenantById(@PathVariable String id) {
    return ResponseEntity.ok(toDto(tenantUseCase.getTenantById(id)));
  }

  @GetMapping("/api/admin/tenants/{id}/users")
  public ResponseEntity<List<TenantUserDTO>> getTenantUsers(@PathVariable String id) {
    List<TenantUserDTO> users =
        tenantUseCase.getUsersByTenant(id).stream()
            .map(
                user ->
                    new TenantUserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getRole() != null ? user.getRole().name() : null))
            .toList();
    return ResponseEntity.ok(users);
  }

  @PostMapping("/api/admin/tenants/{id}/approve")
  public ResponseEntity<TenantDTO> approveTenant(@PathVariable String id) {
    return ResponseEntity.ok(toDto(tenantUseCase.approveTenant(id)));
  }

  @PostMapping("/api/admin/tenants/{id}/block")
  public ResponseEntity<TenantDTO> blockTenant(
      @PathVariable String id, @Valid @RequestBody BlockTenantRequest request) {
    return ResponseEntity.ok(toDto(tenantUseCase.blockTenant(id, request.reason())));
  }

  @PostMapping("/api/admin/tenants/{id}/activate")
  public ResponseEntity<TenantDTO> activateTenant(
      @PathVariable String id, @Valid @RequestBody ManualActivationRequest request) {
    Tenant tenant = tenantUseCase.getTenantById(id);
    if (tenant.getStatus() == TenantStatus.PENDING_APPROVAL) {
      tenantUseCase.approveTenant(id);
    }
    billingUseCase.manuallyActivate(id, request.paymentMethod());
    return ResponseEntity.ok(toDto(tenantUseCase.getTenantById(id)));
  }

  private TenantDTO toDto(Tenant tenant) {
    return new TenantDTO(
        tenant.getId(),
        tenant.getName(),
        tenant.getSlug(),
        tenant.getStatus() != null ? tenant.getStatus().name() : null,
        tenant.getPlanId(),
        tenant.getBillingEmail(),
        tenant.getTrialEndsAt(),
        tenant.getBlockedAt(),
        tenant.getBlockReason(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt());
  }

  private String generateSlug(String name) {
    String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
    String slug =
        normalized
            .replaceAll("\\p{M}", "")
            .toLowerCase()
            .replaceAll("[^a-z0-9]++", "-")
            .replaceAll("(?:^-++)|(?:-++$)", "");
    if (slug.isBlank()) {
      throw new BusinessException("Não foi possível gerar um slug válido para o tenant.");
    }
    return slug;
  }

  public record CreateTenantRequest(
      @NotBlank String name,
      @NotBlank @Pattern(regexp = "starter|pro|enterprise") String planId,
      @NotBlank @Email String billingEmail) {}

  public record BlockTenantRequest(@NotBlank String reason) {}

  public record ManualActivationRequest(PaymentMethod paymentMethod) {}

  public record TenantUserDTO(String id, String username, String role) {}

  /** Campos mínimos para identificação pública da loja. */
  public record PublicTenantDTO(String id, String name, String slug) {}

  public record TenantDTO(
      String id,
      String name,
      String slug,
      String status,
      String planId,
      String billingEmail,
      java.time.LocalDate trialEndsAt,
      java.time.LocalDate blockedAt,
      String blockReason,
      java.time.LocalDateTime createdAt,
      java.time.LocalDateTime updatedAt) {}
}
