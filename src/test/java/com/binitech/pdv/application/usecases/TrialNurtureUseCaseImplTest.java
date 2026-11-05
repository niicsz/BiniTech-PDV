package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.SaleRepositoryPort;
import com.binitech.pdv.application.ports.outbound.SubscriptionRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.TrialEmail;
import com.binitech.pdv.utils.enums.SubscriptionStatus;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrialNurtureUseCaseImplTest {

  private static final LocalDate SIGNUP = LocalDate.of(2026, 11, 2);

  private final TenantRepositoryPort tenants = mock(TenantRepositoryPort.class);
  private final SubscriptionRepositoryPort subscriptions = mock(SubscriptionRepositoryPort.class);
  private final SaleRepositoryPort sales = mock(SaleRepositoryPort.class);
  private final EmailServicePort email = mock(EmailServicePort.class);
  private final TrialNurtureUseCaseImpl nurture =
      new TrialNurtureUseCaseImpl(tenants, subscriptions, sales, email);
  private Tenant tenant;

  @BeforeEach
  void setUp() {
    tenant = new Tenant();
    tenant.setId("t1");
    tenant.setName("Mercado do João");
    tenant.setBillingEmail("dono@loja.com");
    tenant.setPlanId("pro");
    tenant.setStatus(TenantStatus.ACTIVE);
    tenant.setCreatedAt(SIGNUP.atTime(15, 0));
    tenant.setTrialEndsAt(SIGNUP.plusDays(7));
    when(tenants.findAllByStatus(TenantStatus.ACTIVE)).thenReturn(List.of(tenant));
    when(subscriptions.findByTenantId("t1")).thenReturn(Optional.empty());
  }

  @Test
  @DisplayName("No dia seguinte ao cadastro, quem ainda não vendeu recebe o passo a passo")
  void firstSaleNudge() {
    when(sales.countByTenantIdAndTimestampBetween(eq("t1"), any(), any())).thenReturn(0L);

    assertEquals(1, nurture.sendDueEmails(SIGNUP.plusDays(1)));

    verify(email).sendTrialEmail("dono@loja.com", "Mercado do João", TrialEmail.FIRST_SALE);
    assertTrue(tenant.getTrialEmailsSent().contains("FIRST_SALE"));
    verify(tenants).save(tenant);
  }

  @Test
  @DisplayName("Quem já vendeu não recebe o passo a passo da primeira venda")
  void skipsFirstSaleWhenSold() {
    when(sales.countByTenantIdAndTimestampBetween(eq("t1"), any(), any())).thenReturn(3L);

    assertEquals(0, nurture.sendDueEmails(SIGNUP.plusDays(1)));

    verify(email, never()).sendTrialEmail(anyString(), anyString(), any());
  }

  @Test
  @DisplayName("No terceiro dia vai o e-mail de recursos, uma vez só")
  void featuresOnce() {
    when(sales.countByTenantIdAndTimestampBetween(eq("t1"), any(), any())).thenReturn(3L);

    nurture.sendDueEmails(SIGNUP.plusDays(3));
    nurture.sendDueEmails(SIGNUP.plusDays(4));

    verify(email).sendTrialEmail("dono@loja.com", "Mercado do João", TrialEmail.FEATURES);
  }

  @Test
  @DisplayName(
      "Na véspera do fim do teste vai o aviso, e depois do fim vai o e-mail de teste encerrado")
  void endingAndEnded() {
    tenant.getTrialEmailsSent().addAll(List.of("FIRST_SALE", "FEATURES"));

    nurture.sendDueEmails(SIGNUP.plusDays(6));
    nurture.sendDueEmails(SIGNUP.plusDays(7));
    nurture.sendDueEmails(SIGNUP.plusDays(8));
    nurture.sendDueEmails(SIGNUP.plusDays(9));

    verify(email).sendTrialEmail("dono@loja.com", "Mercado do João", TrialEmail.ENDING);
    verify(email).sendTrialEmail("dono@loja.com", "Mercado do João", TrialEmail.ENDED);
  }

  @Test
  @DisplayName("Loja que já assinou não recebe e-mails do teste")
  void paidTenantIsSkipped() {
    Subscription subscription = new Subscription();
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    when(subscriptions.findByTenantId("t1")).thenReturn(Optional.of(subscription));

    assertEquals(0, nurture.sendDueEmails(SIGNUP.plusDays(6)));

    verify(email, never()).sendTrialEmail(anyString(), anyString(), any());
  }

  @Test
  @DisplayName("Lojas antigas sem teste e testes encerrados há muito tempo ficam de fora")
  void legacyAndOldTrialsAreSkipped() {
    tenant.setTrialEndsAt(null);
    assertEquals(0, nurture.sendDueEmails(SIGNUP.plusDays(1)));

    tenant.setTrialEndsAt(SIGNUP.plusDays(7));
    tenant.getTrialEmailsSent().addAll(List.of("FIRST_SALE", "FEATURES", "ENDING"));
    assertEquals(0, nurture.sendDueEmails(SIGNUP.plusDays(30)));

    verify(email, never()).sendTrialEmail(anyString(), anyString(), any());
  }
}
