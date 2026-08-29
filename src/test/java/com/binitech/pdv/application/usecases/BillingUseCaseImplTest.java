package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataUserRepository;
import com.binitech.pdv.application.ports.outbound.InvoiceRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.BillingStripeConfig;
import com.binitech.pdv.config.StripeGateway;
import com.binitech.pdv.config.StripeProperties;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingUseCaseImplTest {

  private static final String FRONTEND_URL = "http://localhost:4200";

  @Mock private SubscriptionRepositoryPort subscriptionRepository;
  @Mock private InvoiceRepositoryPort invoiceRepository;
  @Mock private TenantRepositoryPort tenantRepository;
  @Mock private SpringDataProductRepository productRepository;
  @Mock private SpringDataUserRepository userRepository;
  @Mock private StripeGateway stripeGateway;
  @Mock private StripeProperties stripeProperties;

  private BillingUseCaseImpl billingUseCase;

  @BeforeEach
  void setUp() {
    billingUseCase =
        new BillingUseCaseImpl(
            subscriptionRepository,
            invoiceRepository,
            tenantRepository,
            productRepository,
            userRepository,
            new BillingStripeConfig(FRONTEND_URL, stripeGateway, stripeProperties));
  }

  private Tenant tenant(String id, String planId, TenantStatus status) {
    Tenant t = new Tenant();
    t.setId(id);
    t.setName("Loja " + id);
    t.setSlug("loja-" + id);
    t.setPlanId(planId);
    t.setBillingEmail(id + "@loja.com");
    t.setStatus(status);
    return t;
  }

  @Nested
  @DisplayName("createCheckoutUrl")
  class CreateCheckoutUrlTests {

    @Test
    @DisplayName("Deve devolver a URL da sessão de Checkout do Stripe")
    void createCheckoutUrl_shouldReturnSessionUrl() throws Exception {
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "pro", TenantStatus.ACTIVE)));
      when(stripeGateway.isConfigured()).thenReturn(true);
      when(stripeProperties.priceForTier("pro")).thenReturn("price_pro");
      when(stripeGateway.createSubscriptionCheckoutSession(
              any(Tenant.class),
              eq("price_pro"),
              eq("http://localhost:4200/billing?status=success"),
              eq("http://localhost:4200/billing")))
          .thenReturn("https://checkout.stripe.com/c/sess_1");

      assertEquals("https://checkout.stripe.com/c/sess_1", billingUseCase.createCheckoutUrl("t1"));
    }

    @Test
    @DisplayName("Deve lançar 404 quando o tenant não existe")
    void createCheckoutUrl_tenantNotFound_shouldThrow() {
      when(tenantRepository.findById("x")).thenReturn(Optional.empty());
      assertThrows(ResourceNotFoundException.class, () -> billingUseCase.createCheckoutUrl("x"));
    }

    @Test
    @DisplayName("Plano cortesia não gera checkout")
    void createCheckoutUrl_freePlan_shouldThrowBusiness() {
      when(tenantRepository.findById("tf"))
          .thenReturn(Optional.of(tenant("tf", "free", TenantStatus.ACTIVE)));
      assertThrows(BusinessException.class, () -> billingUseCase.createCheckoutUrl("tf"));
    }

    @Test
    @DisplayName("Sem Stripe configurado, falha explicitamente")
    void createCheckoutUrl_notConfigured_shouldThrowBusiness() {
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "pro", TenantStatus.ACTIVE)));
      when(stripeGateway.isConfigured()).thenReturn(false);
      BusinessException exception =
          assertThrows(BusinessException.class, () -> billingUseCase.createCheckoutUrl("t1"));

      assertTrue(exception.getMessage().contains("Entre em contato com o suporte"));
    }

    @Test
    @DisplayName("Plano sem price orienta o usuário a procurar o suporte")
    void createCheckoutUrl_missingPrice_shouldReferToSupport() {
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "starter", TenantStatus.ACTIVE)));
      when(stripeGateway.isConfigured()).thenReturn(true);
      when(stripeProperties.priceForTier("starter")).thenReturn(null);

      BusinessException exception =
          assertThrows(BusinessException.class, () -> billingUseCase.createCheckoutUrl("t1"));

      assertTrue(exception.getMessage().contains("Entre em contato com o suporte"));
    }
  }

  @Nested
  @DisplayName("createPortalUrl")
  class CreatePortalUrlTests {

    @Test
    @DisplayName("Deve devolver a URL do Customer Portal")
    void createPortalUrl_shouldReturnPortalUrl() throws Exception {
      Subscription sub = new Subscription();
      sub.setTenantId("t1");
      sub.setStripeCustomerId("cus_1");
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.of(sub));
      when(stripeGateway.isConfigured()).thenReturn(true);
      when(stripeGateway.createBillingPortalSession("cus_1", "http://localhost:4200/billing"))
          .thenReturn("https://billing.stripe.com/p/sess_1");

      assertEquals("https://billing.stripe.com/p/sess_1", billingUseCase.createPortalUrl("t1"));
    }

    @Test
    @DisplayName("Sem customer vinculado, falha")
    void createPortalUrl_noCustomer_shouldThrow() {
      Subscription sub = new Subscription();
      sub.setTenantId("t1");
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.of(sub));
      BusinessException exception =
          assertThrows(BusinessException.class, () -> billingUseCase.createPortalUrl("t1"));

      assertTrue(exception.getMessage().contains("Entre em contato com o suporte"));
    }
  }

  @Nested
  @DisplayName("activateFromCheckout")
  class ActivateFromCheckoutTests {

    @Test
    @DisplayName("Deve ativar assinatura e tenant resolvendo por client_reference_id (tenantId)")
    void activateFromCheckout_byExternalReference_shouldActivate() {
      Tenant t = tenant("t1", "starter", TenantStatus.BLOCKED);
      when(tenantRepository.findById("t1")).thenReturn(Optional.of(t));
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.empty());
      when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(returnsFirstArg());
      when(stripeProperties.tierForPrice("price_pro")).thenReturn("pro");

      Optional<Subscription> result =
          billingUseCase.activateFromCheckout("sub_1", "cus_1", "t1", "price_pro");

      assertTrue(result.isPresent());
      Subscription saved = result.get();
      assertEquals("sub_1", saved.getStripeSubscriptionId());
      assertEquals("cus_1", saved.getStripeCustomerId());
      assertEquals(SubscriptionStatus.ACTIVE, saved.getStatus());
      assertEquals("pro", saved.getPlanTier());
      assertNotNull(saved.getLastPaymentDate());

      ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
      verify(tenantRepository).save(tenantCaptor.capture());
      assertEquals(TenantStatus.ACTIVE, tenantCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Sem price conhecido, usa o tier atual do tenant")
    void activateFromCheckout_unknownPrice_shouldFallbackToTenantPlan() {
      Tenant t = tenant("t2", "enterprise", TenantStatus.BLOCKED);
      when(tenantRepository.findById("t2")).thenReturn(Optional.of(t));
      when(subscriptionRepository.findByTenantId("t2")).thenReturn(Optional.empty());
      when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(returnsFirstArg());
      when(stripeProperties.tierForPrice("price_x")).thenReturn(null);

      Optional<Subscription> result =
          billingUseCase.activateFromCheckout("sub_2", "cus_2", "t2", "price_x");

      assertTrue(result.isPresent());
      assertEquals("enterprise", result.get().getPlanTier());
    }

    @Test
    @DisplayName("Deve retornar vazio e não persistir quando não identifica o tenant")
    void activateFromCheckout_unresolvedTenant_shouldReturnEmpty() {
      when(tenantRepository.findById("desconhecido")).thenReturn(Optional.empty());

      Optional<Subscription> result =
          billingUseCase.activateFromCheckout("sub_3", "cus_3", "desconhecido", null);

      assertTrue(result.isEmpty());
      verify(subscriptionRepository, never()).save(any());
      verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sem client_reference_id, retorna vazio")
    void activateFromCheckout_noExternalReference_shouldReturnEmpty() {
      Optional<Subscription> result =
          billingUseCase.activateFromCheckout("sub_4", "cus_4", null, "price_pro");

      assertTrue(result.isEmpty());
      verify(subscriptionRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("recordPaymentFailure")
  class RecordPaymentFailureTests {

    @Test
    @DisplayName("Deve marcar a assinatura como PAST_DUE e incrementar falhas")
    void recordPaymentFailure_shouldMarkPastDue() {
      Subscription sub = new Subscription();
      sub.setId("s1");
      sub.setTenantId("t1");
      sub.setStripeSubscriptionId("sub_1");
      sub.setStatus(SubscriptionStatus.ACTIVE);
      sub.setFailedPaymentCount(0);
      when(subscriptionRepository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(sub));
      when(invoiceRepository.findAllByTenantIdOrderByDueDateDesc("t1")).thenReturn(List.of());

      billingUseCase.recordPaymentFailure("sub_1");

      ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
      verify(subscriptionRepository).save(captor.capture());
      assertEquals(SubscriptionStatus.PAST_DUE, captor.getValue().getStatus());
      assertEquals(1, captor.getValue().getFailedPaymentCount());
    }

    @Test
    @DisplayName("Assinatura inexistente lança 404")
    void recordPaymentFailure_unknownSubscription_shouldThrow() {
      when(subscriptionRepository.findByStripeSubscriptionId("nope")).thenReturn(Optional.empty());
      assertThrows(
          ResourceNotFoundException.class, () -> billingUseCase.recordPaymentFailure("nope"));
    }
  }

  @Nested
  @DisplayName("manuallyActivate")
  class ManuallyActivateTests {

    @Test
    @DisplayName("Deve criar assinatura ativa e desbloquear o tenant")
    void manuallyActivate_shouldActivateSubscriptionAndTenant() {
      Tenant t = tenant("t9", "enterprise", TenantStatus.BLOCKED);
      when(tenantRepository.findById("t9")).thenReturn(Optional.of(t));
      when(subscriptionRepository.findByTenantId("t9")).thenReturn(Optional.empty());
      when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(returnsFirstArg());

      Subscription result = billingUseCase.manuallyActivate("t9", PaymentMethod.PIX);

      assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
      assertEquals("enterprise", result.getPlanTier());
      assertNotNull(result.getLastPaymentDate());

      ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
      verify(tenantRepository).save(tenantCaptor.capture());
      assertEquals(TenantStatus.ACTIVE, tenantCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Deve lançar 404 quando o tenant não existe")
    void manuallyActivate_tenantNotFound_shouldThrow() {
      when(tenantRepository.findById("none")).thenReturn(Optional.empty());
      assertThrows(
          ResourceNotFoundException.class,
          () -> billingUseCase.manuallyActivate("none", PaymentMethod.CASH));
    }
  }
}
