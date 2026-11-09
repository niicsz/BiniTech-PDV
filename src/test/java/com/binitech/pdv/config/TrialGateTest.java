package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class TrialGateTest {

  private final TenantRepositoryPort tenants = mock(TenantRepositoryPort.class);
  private final SubscriptionRepositoryPort subscriptions = mock(SubscriptionRepositoryPort.class);
  private final TenantValidationFilter filter =
      new TenantValidationFilter(tenants, subscriptions, new ObjectMapper());
  private final LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
  private final Tenant tenant = new Tenant();

  @BeforeEach
  void setUp() {
    tenant.setId("t1");
    tenant.setPlanId("starter");
    tenant.setStatus(TenantStatus.ACTIVE);
    when(tenants.findById("t1")).thenReturn(Optional.of(tenant));
    when(subscriptions.findByTenantId("t1")).thenReturn(Optional.empty());
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "u1", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_OPERATOR")));
    auth.setDetails("t1");
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private MockHttpServletResponse call() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products");
    request.setRequestURI("/api/products");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response;
  }

  @Test
  @DisplayName("Durante os 7 dias grátis o caixa funciona")
  void allowsDuringTrial() throws Exception {
    tenant.setTrialEndsAt(today.plusDays(3));
    assertEquals(200, call().getStatus());
  }

  @Test
  @DisplayName("Depois do teste grátis, sem pagamento, responde 402 com a explicação")
  void blocksAfterTrialWithoutPayment() throws Exception {
    tenant.setTrialEndsAt(today.minusDays(1));
    MockHttpServletResponse response = call();
    assertEquals(402, response.getStatus());
    assertTrue(response.getContentAsString().contains("TRIAL_ENDED"));
  }

  @Test
  @DisplayName("Depois do teste grátis, com assinatura paga, o caixa continua")
  void allowsPaidAfterTrial() throws Exception {
    tenant.setTrialEndsAt(today.minusDays(10));
    Subscription paid = new Subscription();
    paid.setStatus(SubscriptionStatus.ACTIVE);
    when(subscriptions.findByTenantId("t1")).thenReturn(Optional.of(paid));
    assertEquals(200, call().getStatus());
  }

  @Test
  @DisplayName("Lojas sem data de teste (anteriores ao teste grátis) não são bloqueadas")
  void legacyTenantsWithoutTrialAreNotBlocked() throws Exception {
    assertEquals(200, call().getStatus());
  }

  @Test
  @DisplayName("O teste começa uma vez e dura 7 dias")
  void startTrialOnce() {
    Tenant fresh = new Tenant();
    fresh.startTrial(today);
    assertEquals(today.plusDays(7), fresh.getTrialEndsAt());
    fresh.startTrial(today.plusDays(30));
    assertEquals(today.plusDays(7), fresh.getTrialEndsAt());
    assertFalse(fresh.trialExpired(today.plusDays(7)));
    assertTrue(fresh.trialExpired(today.plusDays(8)));
  }
}
