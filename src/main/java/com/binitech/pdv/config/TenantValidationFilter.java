package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class TenantValidationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(TenantValidationFilter.class);

  private final TenantRepositoryPort tenantRepository;
  private final SubscriptionRepositoryPort subscriptionRepository;
  private final ObjectMapper objectMapper;

  public TenantValidationFilter(
      TenantRepositoryPort tenantRepository,
      SubscriptionRepositoryPort subscriptionRepository,
      ObjectMapper objectMapper) {
    this.subscriptionRepository = subscriptionRepository;
    this.tenantRepository = tenantRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !path.startsWith("/api/")
        || path.startsWith("/api/auth/")
        || path.startsWith("/api/admin/")
        || path.startsWith("/api/public/")
        || path.startsWith("/api/billing/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    Object details = authentication.getDetails();
    if (!(details instanceof String tenantId) || tenantId == null || tenantId.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
    if (tenant == null) {
      log.warn("Tenant não encontrado para a sessão: tenantId={}", LogSanitizer.maskId(tenantId));
      writeError(
          response,
          HttpStatus.FORBIDDEN,
          "TENANT_NOT_FOUND",
          "O tenant associado à sessão não foi encontrado.");
      return;
    }

    if (tenant.getStatus() == TenantStatus.BLOCKED
        || tenant.getStatus() == TenantStatus.CANCELLED) {
      writeError(
          response,
          HttpStatus.PAYMENT_REQUIRED,
          "TENANT_BLOCKED",
          "Sua assinatura está suspensa. Acesse /billing para regularizar.");
      return;
    }

    if (tenant.getStatus() == TenantStatus.PENDING_APPROVAL) {
      writeError(
          response,
          HttpStatus.FORBIDDEN,
          "TENANT_PENDING",
          "Sua conta ainda está aguardando aprovação.");
      return;
    }

    if (tenant.getStatus() == TenantStatus.REJECTED) {
      writeError(
          response,
          HttpStatus.FORBIDDEN,
          "TENANT_REJECTED",
          "A solicitação de cadastro deste tenant foi reprovada.");
      return;
    }

    if (trialEndedWithoutPayment(tenant)) {
      writeError(
          response,
          HttpStatus.PAYMENT_REQUIRED,
          "TRIAL_ENDED",
          "Seu teste grátis de "
              + Tenant.TRIAL_DAYS
              + " dias terminou. Assine em Assinatura para continuar usando o caixa.");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void writeError(
      HttpServletResponse response, HttpStatus status, String code, String message)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getWriter(),
        new TenantErrorResponse(code, message, OffsetDateTime.now(ZoneId.systemDefault())));
  }

  private record TenantErrorResponse(String code, String message, OffsetDateTime timestamp) {}

  private boolean trialEndedWithoutPayment(Tenant tenant) {
    if (tenant.getStatus() != TenantStatus.ACTIVE
        || PlanConfig.isFree(tenant.getPlanId())
        || !tenant.trialExpired(LocalDate.now(ZoneId.of("America/Sao_Paulo")))) {
      return false;
    }
    return subscriptionRepository
        .findByTenantId(tenant.getId())
        .map(s -> s.getStatus())
        .filter(s -> s == SubscriptionStatus.ACTIVE || s == SubscriptionStatus.PAST_DUE)
        .isEmpty();
  }
}
