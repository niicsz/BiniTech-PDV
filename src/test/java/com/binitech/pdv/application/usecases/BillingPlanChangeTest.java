package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataUserRepository;
import com.binitech.pdv.application.ports.inbound.BillingUseCasePort.PlanOverview;
import com.binitech.pdv.application.ports.outbound.CheckoutSessionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.InvoiceRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.AsaasException;
import com.binitech.pdv.config.AsaasGateway;
import com.binitech.pdv.config.BillingAsaasConfig;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingPlanChangeTest {

  @Mock private SubscriptionRepositoryPort subscriptions;
  @Mock private InvoiceRepositoryPort invoices;
  @Mock private TenantRepositoryPort tenants;
  @Mock private SpringDataProductRepository products;
  @Mock private SpringDataUserRepository users;
  @Mock private AsaasGateway asaas;
  @Mock private CheckoutSessionRepositoryPort checkouts;

  private BillingUseCaseImpl billing;
  private Tenant tenant;

  @BeforeEach
  void setUp() {
    billing =
        new BillingUseCaseImpl(
            subscriptions,
            invoices,
            tenants,
            products,
            users,
            new BillingAsaasConfig("https://www.binitechpdv.com.br", asaas, checkouts));
    tenant = new Tenant();
    tenant.setId("t1");
    tenant.setName("Loja");
    tenant.setPlanId("starter");
    tenant.setStatus(TenantStatus.ACTIVE);
    when(tenants.findById("t1")).thenReturn(Optional.of(tenant));
    when(tenants.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(users.countByTenantIdAndRole("t1", "OPERATOR")).thenReturn(1L);
    when(asaas.isConfigured()).thenReturn(true);
  }

  private void cardSubscription() {
    Subscription subscription = new Subscription();
    subscription.setTenantId("t1");
    subscription.setPlanTier("starter");
    subscription.setAsaasSubscriptionId("sub_1");
    subscription.setBillingType("CREDIT_CARD");
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    when(subscriptions.findByTenantId("t1")).thenReturn(Optional.of(subscription));
  }

  @Test
  @DisplayName("Mostra o plano atual, o limite de operadores e o valor do mês")
  void overview() {
    tenant.setExtraUsers(2);
    PlanOverview overview = billing.getPlanOverview("t1");
    assertEquals("starter", overview.currentPlan());
    assertEquals(3, overview.operatorLimit());
    assertEquals(new BigDecimal("159.00"), overview.monthlyAmount());
    assertEquals(3, overview.plans().size());
  }

  @Test
  @DisplayName("Upgrade troca o plano na hora e atualiza o valor da assinatura no cartão")
  void upgradeUpdatesRecurringCard() {
    cardSubscription();
    PlanOverview overview = billing.changePlan("t1", "PRO", Role.TENANT_ADMIN);

    assertEquals("pro", overview.currentPlan());
    assertEquals("pro", tenant.getPlanId());
    verify(asaas).updateSubscriptionValue(eq("sub_1"), eq(new BigDecimal("149.00")), anyString());
    ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
    verify(subscriptions).save(saved.capture());
    assertEquals("pro", saved.getValue().getPlanTier());
  }

  @Test
  @DisplayName("Sem assinatura no cartão, só troca o plano e não chama o Asaas")
  void changeWithoutCardSubscription() {
    billing.changePlan("t1", "enterprise", Role.TENANT_ADMIN);
    assertEquals("enterprise", tenant.getPlanId());
    verify(asaas, never()).updateSubscriptionValue(anyString(), any(), anyString());
  }

  @Test
  @DisplayName("Downgrade é recusado quando há mais operadores do que o novo plano permite")
  void downgradeBlockedByOperators() {
    tenant.setPlanId("pro");
    when(users.countByTenantIdAndRole("t1", "OPERATOR")).thenReturn(3L);
    BusinessException error =
        assertThrows(
            BusinessException.class, () -> billing.changePlan("t1", "starter", Role.TENANT_ADMIN));
    assertTrue(error.getMessage().contains("3 operadores"));
    assertEquals("pro", tenant.getPlanId());
  }

  @Test
  @DisplayName("Downgrade é recusado quando há mais produtos ativos do que o novo plano permite")
  void downgradeBlockedByProducts() {
    tenant.setPlanId("pro");
    when(products.countByTenantIdAndActiveTrue("t1")).thenReturn(5000L);
    BusinessException error =
        assertThrows(
            BusinessException.class, () -> billing.changePlan("t1", "starter", Role.TENANT_ADMIN));
    assertTrue(error.getMessage().contains("5000 produtos ativos"));
    assertEquals("pro", tenant.getPlanId());
  }

  @Test
  @DisplayName("Se o Asaas recusar, o plano não muda")
  void asaasFailureKeepsPlan() {
    cardSubscription();
    doThrow(new AsaasException("HTTP 400", null))
        .when(asaas)
        .updateSubscriptionValue(anyString(), any(), anyString());
    assertThrows(BusinessException.class, () -> billing.changePlan("t1", "pro", Role.TENANT_ADMIN));
    assertEquals("starter", tenant.getPlanId());
    verify(tenants, never()).save(any());
  }

  @Test
  @DisplayName("Operador não muda plano nem usuários adicionais")
  void operatorCannotChangeBilling() {
    assertThrows(AccessDeniedException.class, () -> billing.changePlan("t1", "pro", Role.OPERATOR));
    assertThrows(AccessDeniedException.class, () -> billing.setExtraUsers("t1", 1, Role.OPERATOR));
  }

  @Test
  @DisplayName("Usuários adicionais somam R$ 30 cada na assinatura e liberam mais operadores")
  void extraUsersRaiseLimitAndValue() {
    cardSubscription();
    PlanOverview overview = billing.setExtraUsers("t1", 2, Role.TENANT_ADMIN);

    assertEquals(2, tenant.getExtraUsers());
    assertEquals(3, overview.operatorLimit());
    assertEquals(new BigDecimal("159.00"), overview.monthlyAmount());
    verify(asaas).updateSubscriptionValue(eq("sub_1"), eq(new BigDecimal("159.00")), anyString());
  }

  @Test
  @DisplayName("Não deixa reduzir usuários adicionais abaixo dos operadores em uso")
  void cannotRemoveSeatsInUse() {
    tenant.setExtraUsers(2);
    when(users.countByTenantIdAndRole("t1", "OPERATOR")).thenReturn(3L);
    assertThrows(BusinessException.class, () -> billing.setExtraUsers("t1", 1, Role.TENANT_ADMIN));
    assertEquals(2, tenant.getExtraUsers());
  }

  @Test
  @DisplayName("Plano desconhecido é recusado")
  void unknownPlan() {
    assertThrows(
        BusinessException.class, () -> billing.changePlan("t1", "free", Role.TENANT_ADMIN));
  }
}
