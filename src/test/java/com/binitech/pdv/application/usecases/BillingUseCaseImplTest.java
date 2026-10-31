package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataProductRepository;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataUserRepository;
import com.binitech.pdv.application.ports.outbound.CheckoutSessionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.InvoiceRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.config.AsaasException;
import com.binitech.pdv.config.AsaasGateway;
import com.binitech.pdv.config.AsaasGateway.CheckoutSession;
import com.binitech.pdv.config.BillingAsaasConfig;
import com.binitech.pdv.domain.Invoice;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.domain.exception.ResourceNotFoundException;
import com.binitech.pdv.utils.enums.InvoiceStatus;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
  @Mock private AsaasGateway asaasGateway;
  @Mock private CheckoutSessionRepositoryPort checkoutSessionRepository;

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
            new BillingAsaasConfig(FRONTEND_URL, asaasGateway, checkoutSessionRepository));
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
    @DisplayName("Deve devolver a URL do Checkout do Asaas com o valor do plano")
    void createCheckoutUrl_shouldReturnCheckoutUrl() {
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "pro", TenantStatus.ACTIVE)));
      when(asaasGateway.isConfigured()).thenReturn(true);
      when(asaasGateway.createSubscriptionCheckout(
              any(Tenant.class),
              eq("Pro"),
              eq(new BigDecimal("199.00")),
              eq("http://localhost:4200/billing?status=success"),
              eq("http://localhost:4200/billing")))
          .thenReturn(
              new CheckoutSession("chk_1", "https://asaas.com/checkoutSession/show?id=chk_1"));

      assertEquals(
          "https://asaas.com/checkoutSession/show?id=chk_1",
          billingUseCase.createCheckoutUrl("t1"));
      verify(checkoutSessionRepository).save("chk_1", "t1");
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
    @DisplayName("Sem Asaas configurado, falha explicitamente")
    void createCheckoutUrl_notConfigured_shouldThrowBusiness() {
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "pro", TenantStatus.ACTIVE)));
      when(asaasGateway.isConfigured()).thenReturn(false);
      BusinessException exception =
          assertThrows(BusinessException.class, () -> billingUseCase.createCheckoutUrl("t1"));

      assertTrue(exception.getMessage().contains("Entre em contato com o suporte"));
    }

    @Test
    @DisplayName("Falha no Asaas orienta o usuário a procurar o suporte")
    void createCheckoutUrl_gatewayFailure_shouldReferToSupport() {
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "starter", TenantStatus.ACTIVE)));
      when(asaasGateway.isConfigured()).thenReturn(true);
      when(asaasGateway.createSubscriptionCheckout(any(), any(), any(), any(), any()))
          .thenThrow(new AsaasException("falha", null));

      BusinessException exception =
          assertThrows(BusinessException.class, () -> billingUseCase.createCheckoutUrl("t1"));

      assertTrue(exception.getMessage().contains("Entre em contato com o suporte"));
    }
  }

  @Nested
  @DisplayName("createPortalUrl")
  class CreatePortalUrlTests {

    @Test
    @DisplayName("Deve devolver a URL da fatura em aberto no Asaas")
    void createPortalUrl_shouldReturnOpenInvoiceUrl() {
      Subscription sub = new Subscription();
      sub.setTenantId("t1");
      sub.setAsaasSubscriptionId("sub_1");
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.of(sub));
      when(asaasGateway.isConfigured()).thenReturn(true);
      when(asaasGateway.findOpenPaymentUrl("sub_1"))
          .thenReturn(Optional.of("https://www.asaas.com/i/123"));

      assertEquals("https://www.asaas.com/i/123", billingUseCase.createPortalUrl("t1"));
    }

    @Test
    @DisplayName("Sem fatura em aberto, informa o usuário")
    void createPortalUrl_noOpenInvoice_shouldThrow() {
      Subscription sub = new Subscription();
      sub.setTenantId("t1");
      sub.setAsaasSubscriptionId("sub_1");
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.of(sub));
      when(asaasGateway.isConfigured()).thenReturn(true);
      when(asaasGateway.findOpenPaymentUrl("sub_1")).thenReturn(Optional.empty());

      BusinessException exception =
          assertThrows(BusinessException.class, () -> billingUseCase.createPortalUrl("t1"));

      assertEquals("Nenhuma fatura em aberto no momento.", exception.getMessage());
    }

    @Test
    @DisplayName("Sem assinatura Asaas vinculada, falha")
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
    @DisplayName("Deve ativar assinatura e tenant resolvendo por externalReference (tenantId)")
    void activateFromCheckout_byExternalReference_shouldActivate() {
      Tenant t = tenant("t1", "pro", TenantStatus.BLOCKED);
      when(tenantRepository.findById("t1")).thenReturn(Optional.of(t));
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.empty());
      when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(returnsFirstArg());

      Optional<Subscription> result = billingUseCase.activateFromCheckout("sub_1", "cus_1", "t1");

      assertTrue(result.isPresent());
      Subscription saved = result.get();
      assertEquals("sub_1", saved.getAsaasSubscriptionId());
      assertEquals("cus_1", saved.getAsaasCustomerId());
      assertEquals(SubscriptionStatus.ACTIVE, saved.getStatus());
      assertEquals("pro", saved.getPlanTier());
      assertNotNull(saved.getLastPaymentDate());

      ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
      verify(tenantRepository).save(tenantCaptor.capture());
      assertEquals(TenantStatus.ACTIVE, tenantCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Deve retornar vazio e não persistir quando não identifica o tenant")
    void activateFromCheckout_unresolvedTenant_shouldReturnEmpty() {
      when(tenantRepository.findById("desconhecido")).thenReturn(Optional.empty());

      Optional<Subscription> result =
          billingUseCase.activateFromCheckout("sub_3", "cus_3", "desconhecido");

      assertTrue(result.isEmpty());
      verify(subscriptionRepository, never()).save(any());
      verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sem externalReference, retorna vazio")
    void activateFromCheckout_noExternalReference_shouldReturnEmpty() {
      Optional<Subscription> result = billingUseCase.activateFromCheckout("sub_4", "cus_4", null);

      assertTrue(result.isEmpty());
      verify(subscriptionRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("confirmSubscriptionPayment")
  class ConfirmSubscriptionPaymentTests {

    @Test
    @DisplayName("Primeiro pagamento do checkout ativa o tenant resolvido pela sessão salva")
    void firstPayment_shouldActivateTenantFromCheckoutSession() {
      Tenant t = tenant("t1", "pro", TenantStatus.PENDING_APPROVAL);
      when(subscriptionRepository.findByAsaasSubscriptionId("sub_1")).thenReturn(Optional.empty());
      when(checkoutSessionRepository.findTenantId("chk_1")).thenReturn(Optional.of("t1"));
      when(tenantRepository.findById("t1")).thenReturn(Optional.of(t));
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.empty());
      when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(returnsFirstArg());

      billingUseCase.confirmSubscriptionPayment("pay_1", "sub_1", "cus_1", "chk_1");

      ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
      verify(subscriptionRepository, atLeastOnce()).save(captor.capture());
      Subscription saved = captor.getValue();
      assertEquals("sub_1", saved.getAsaasSubscriptionId());
      assertEquals("cus_1", saved.getAsaasCustomerId());
      assertEquals(SubscriptionStatus.ACTIVE, saved.getStatus());
      assertEquals(TenantStatus.ACTIVE, t.getStatus());
      verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pagamento de assinatura já vinculada marca a fatura como paga")
    void knownSubscription_shouldMarkInvoicePaid() {
      Subscription sub = new Subscription();
      sub.setTenantId("t1");
      sub.setAsaasSubscriptionId("sub_1");
      when(subscriptionRepository.findByAsaasSubscriptionId("sub_1")).thenReturn(Optional.of(sub));
      when(invoiceRepository.findByAsaasPaymentId("pay_2")).thenReturn(Optional.empty());
      when(invoiceRepository.findAllByTenantIdOrderByDueDateDesc("t1")).thenReturn(List.of());

      billingUseCase.confirmSubscriptionPayment("pay_2", "sub_1", "cus_1", "chk_1");

      verify(checkoutSessionRepository, never()).findTenantId(any());
      assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
    }

    @Test
    @DisplayName("Checkout sem sessão conhecida não ativa nada")
    void unknownCheckoutSession_shouldNotActivate() {
      when(subscriptionRepository.findByAsaasSubscriptionId("sub_9")).thenReturn(Optional.empty());
      when(checkoutSessionRepository.findTenantId("chk_9")).thenReturn(Optional.empty());

      billingUseCase.confirmSubscriptionPayment("pay_9", "sub_9", "cus_9", "chk_9");

      verify(subscriptionRepository, never()).save(any());
      verify(tenantRepository, never()).save(any());
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
      sub.setAsaasSubscriptionId("sub_1");
      sub.setStatus(SubscriptionStatus.ACTIVE);
      sub.setFailedPaymentCount(0);
      when(subscriptionRepository.findByAsaasSubscriptionId("sub_1")).thenReturn(Optional.of(sub));
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
      when(subscriptionRepository.findByAsaasSubscriptionId("nope")).thenReturn(Optional.empty());
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

  @Nested
  @DisplayName("getSubscriptionForTenant")
  class GetSubscriptionForTenant {

    @Test
    @DisplayName("sem assinatura gravada deve devolver o plano do cadastro como pendente")
    void withoutSubscription_shouldReturnPendingFromTenantPlan() {
      Tenant tenant = new Tenant();
      tenant.setId("t1");
      tenant.setPlanId("starter");
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.empty());
      when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));

      Subscription result = billingUseCase.getSubscriptionForTenant("t1").orElseThrow();

      assertEquals("starter", result.getPlanTier());
      assertEquals(SubscriptionStatus.PENDING, result.getStatus());
      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("com assinatura gravada deve devolvê-la sem consultar o tenant")
    void withSubscription_shouldReturnIt() {
      Subscription stored = new Subscription();
      stored.setPlanTier("pro");
      stored.setStatus(SubscriptionStatus.ACTIVE);
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.of(stored));

      assertEquals(stored, billingUseCase.getSubscriptionForTenant("t1").orElseThrow());
      verify(tenantRepository, never()).findById(any());
    }
  }

  @Nested
  @DisplayName("Pix avulso")
  class PixTests {

    @Test
    @DisplayName("Deve criar checkout Pix com o valor do plano e registrar a sessão como PIX")
    void createPixCheckoutUrl_shouldUsePlanFeeAndStoreSession() {
      Tenant t = tenant("t1", "starter", TenantStatus.ACTIVE);
      when(tenantRepository.findById("t1")).thenReturn(Optional.of(t));
      when(asaasGateway.isConfigured()).thenReturn(true);
      when(invoiceRepository.findAllByTenantIdOrderByDueDateDesc("t1")).thenReturn(List.of());
      when(asaasGateway.createPixCheckout(
              eq(t), eq("Starter"), eq(new BigDecimal("99.00")), any(), any()))
          .thenReturn(
              new CheckoutSession("chk_pix", "https://asaas.com/checkoutSession/show?id=chk_pix"));

      String url = billingUseCase.createPixCheckoutUrl("t1");

      assertEquals("https://asaas.com/checkoutSession/show?id=chk_pix", url);
      verify(checkoutSessionRepository).save("chk_pix", "t1", "PIX");
    }

    @Test
    @DisplayName("Deve cobrar no Pix o valor da fatura em aberto")
    void createPixCheckoutUrl_shouldUseOpenInvoiceAmount() {
      Tenant t = tenant("t1", "starter", TenantStatus.BLOCKED);
      Invoice overdue = new Invoice();
      overdue.setStatus(InvoiceStatus.OVERDUE);
      overdue.setAmount(new BigDecimal("129.00"));
      when(tenantRepository.findById("t1")).thenReturn(Optional.of(t));
      when(asaasGateway.isConfigured()).thenReturn(true);
      when(invoiceRepository.findAllByTenantIdOrderByDueDateDesc("t1"))
          .thenReturn(List.of(overdue));
      when(asaasGateway.createPixCheckout(eq(t), any(), eq(new BigDecimal("129.00")), any(), any()))
          .thenReturn(new CheckoutSession("chk_pix", "url"));

      billingUseCase.createPixCheckoutUrl("t1");

      verify(asaasGateway)
          .createPixCheckout(eq(t), any(), eq(new BigDecimal("129.00")), any(), any());
    }

    @Test
    @DisplayName("Plano cortesia não gera Pix")
    void createPixCheckoutUrl_freePlan_shouldThrow() {
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "free", TenantStatus.ACTIVE)));
      assertThrows(BusinessException.class, () -> billingUseCase.createPixCheckoutUrl("t1"));
      verify(asaasGateway, never()).createPixCheckout(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Pagamento do checkout Pix ativa 30 dias, quita faturas e desbloqueia o tenant")
    void pixCheckoutPaid_shouldActivatePeriodSettleInvoicesAndUnblock() {
      Tenant t = tenant("t1", "pro", TenantStatus.BLOCKED);
      Invoice overdue = new Invoice();
      overdue.setStatus(InvoiceStatus.OVERDUE);
      when(checkoutSessionRepository.findBillingType("chk_pix")).thenReturn(Optional.of("PIX"));
      when(checkoutSessionRepository.findTenantId("chk_pix")).thenReturn(Optional.of("t1"));
      when(checkoutSessionRepository.markPaid("chk_pix")).thenReturn(true);
      when(tenantRepository.findById("t1")).thenReturn(Optional.of(t));
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.empty());
      when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(returnsFirstArg());
      when(invoiceRepository.findAllByTenantIdOrderByDueDateDesc("t1"))
          .thenReturn(List.of(overdue));

      Subscription result =
          billingUseCase.activateFromCheckoutSession("chk_pix", null, "cus_1", null).orElseThrow();

      LocalDate today = LocalDate.now(ZoneId.systemDefault());
      assertEquals("PIX", result.getBillingType());
      assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
      assertEquals(today.plusDays(30), result.getCurrentPeriodEnd());
      assertEquals(InvoiceStatus.PAID, overdue.getStatus());
      assertEquals(TenantStatus.ACTIVE, t.getStatus());
    }

    @Test
    @DisplayName("Pix pago antes do vencimento soma 30 dias ao fim do período atual")
    void pixCheckoutPaid_activeSubscription_shouldExtendFromPeriodEnd() {
      LocalDate today = LocalDate.now(ZoneId.systemDefault());
      Subscription current = new Subscription();
      current.setTenantId("t1");
      current.setStatus(SubscriptionStatus.ACTIVE);
      current.setCurrentPeriodEnd(today.plusDays(5));
      when(checkoutSessionRepository.findBillingType("chk_pix")).thenReturn(Optional.of("PIX"));
      when(checkoutSessionRepository.findTenantId("chk_pix")).thenReturn(Optional.of("t1"));
      when(checkoutSessionRepository.markPaid("chk_pix")).thenReturn(true);
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "pro", TenantStatus.ACTIVE)));
      when(subscriptionRepository.findByTenantId("t1")).thenReturn(Optional.of(current));
      when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(returnsFirstArg());

      Subscription result =
          billingUseCase.activateFromCheckoutSession("chk_pix", null, null, null).orElseThrow();

      assertEquals(today.plusDays(35), result.getCurrentPeriodEnd());
    }

    @Test
    @DisplayName("Webhook repetido do mesmo checkout Pix não estende o período de novo")
    void pixCheckoutPaid_duplicate_shouldNotExtendAgain() {
      when(checkoutSessionRepository.findBillingType("chk_pix")).thenReturn(Optional.of("PIX"));
      when(checkoutSessionRepository.findTenantId("chk_pix")).thenReturn(Optional.of("t1"));
      when(checkoutSessionRepository.markPaid("chk_pix")).thenReturn(false);
      when(tenantRepository.findById("t1"))
          .thenReturn(Optional.of(tenant("t1", "pro", TenantStatus.ACTIVE)));

      billingUseCase.activateFromCheckoutSession("chk_pix", null, null, null);

      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear somente assinaturas Pix vencidas além da carência")
    void blockExpiredPixSubscriptions_shouldBlockOnlyExpiredPix() {
      LocalDate today = LocalDate.now(ZoneId.systemDefault());
      Subscription expiredPix = new Subscription();
      expiredPix.setTenantId("t1");
      expiredPix.setBillingType("PIX");
      expiredPix.setStatus(SubscriptionStatus.ACTIVE);
      expiredPix.setCurrentPeriodEnd(today.minusDays(4));
      Subscription recentPix = new Subscription();
      recentPix.setTenantId("t2");
      recentPix.setBillingType("PIX");
      recentPix.setStatus(SubscriptionStatus.ACTIVE);
      recentPix.setCurrentPeriodEnd(today.minusDays(2));
      Subscription manual = new Subscription();
      manual.setTenantId("t3");
      manual.setStatus(SubscriptionStatus.ACTIVE);
      manual.setCurrentPeriodEnd(today.minusDays(60));
      Tenant t1 = tenant("t1", "pro", TenantStatus.ACTIVE);
      when(subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE))
          .thenReturn(List.of(expiredPix, recentPix, manual));
      when(tenantRepository.findById("t1")).thenReturn(Optional.of(t1));

      billingUseCase.blockExpiredPixSubscriptions(3);

      assertEquals(SubscriptionStatus.PAST_DUE, expiredPix.getStatus());
      assertEquals(SubscriptionStatus.ACTIVE, recentPix.getStatus());
      assertEquals(SubscriptionStatus.ACTIVE, manual.getStatus());
      assertEquals(TenantStatus.BLOCKED, t1.getStatus());
      verify(tenantRepository, never()).findById("t2");
      verify(tenantRepository, never()).findById("t3");
    }
  }
}
