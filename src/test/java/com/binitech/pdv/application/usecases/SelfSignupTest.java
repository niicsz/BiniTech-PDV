package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.inbound.TenantUseCasePort.AdTracking;
import com.binitech.pdv.application.ports.inbound.TenantUseCasePort.SelfSignup;
import com.binitech.pdv.application.ports.inbound.TenantUseCasePort.SignupResult;
import com.binitech.pdv.application.ports.inbound.TenantUseCasePort.TermsAcceptance;
import com.binitech.pdv.application.ports.outbound.AdConversionsPort;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.Role;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SelfSignupTest {

  private final TenantRepositoryPort tenants = mock(TenantRepositoryPort.class);
  private final UserRepositoryPort users = mock(UserRepositoryPort.class);
  private final AuthenticationGateway authentication = mock(AuthenticationGateway.class);
  private final EmailServicePort email = mock(EmailServicePort.class);

  @BeforeEach
  void setUp() {
    when(tenants.save(any(Tenant.class)))
        .thenAnswer(
            inv -> {
              Tenant t = inv.getArgument(0);
              if (t.getId() == null) {
                t.setId("t-new");
              }
              return t;
            });
    when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(tenants.findByBillingEmail(anyString())).thenReturn(Optional.empty());
  }

  private static SelfSignup signup(String mail) {
    return new SelfSignup(
        "Mercado do João",
        "mercado-do-joao",
        "pro",
        mail,
        "senha-forte-123",
        "(11) 91234-5678",
        new TermsAcceptance("2026-11-04", "200.1.1.1"),
        null);
  }

  @Test
  @DisplayName(
      "Cadastro ativa a loja, começa o teste de 7 dias e cria o admin com o e-mail e a senha escolhida")
  void activatesImmediately() {
    TenantUseCaseImpl useCase = new TenantUseCaseImpl(tenants, users, authentication, email, true);

    SignupResult result = useCase.signUp(signup(" Joao@Mercado.COM "));

    assertTrue(result.active());
    assertEquals("joao@mercado.com", result.username());
    assertEquals(TenantStatus.ACTIVE, result.tenant().getStatus());
    assertEquals(LocalDate.now().plusDays(7), result.tenant().getTrialEndsAt());
    assertEquals("11912345678", result.tenant().getContactPhone());
    verify(authentication)
        .provision(
            anyString(),
            eq("joao@mercado.com"),
            eq("senha-forte-123"),
            eq("t-new"),
            eq("joao@mercado.com"));
    ArgumentCaptor<User> admin = ArgumentCaptor.forClass(User.class);
    verify(users).save(admin.capture());
    assertEquals(Role.TENANT_ADMIN, admin.getValue().getRole());
    verify(email).sendWelcomeEmail("joao@mercado.com", "Mercado do João", "joao@mercado.com");
  }

  @Test
  @DisplayName("E-mail já usado ou descartável é recusado sem criar a loja")
  void rejectsDuplicateAndDisposableEmails() {
    TenantUseCaseImpl useCase = new TenantUseCaseImpl(tenants, users, authentication, email, true);
    when(users.existsByUsername("dono@loja.com")).thenReturn(true);
    SelfSignup duplicate = signup("dono@loja.com");
    SelfSignup disposable = signup("x@mailinator.com");

    assertThrows(BusinessException.class, () -> useCase.signUp(duplicate));
    assertThrows(BusinessException.class, () -> useCase.signUp(disposable));
    verify(tenants, never()).save(any());
  }

  @Test
  @DisplayName("Com o cadastro automático desligado, a loja fica aguardando aprovação")
  void manualApprovalWhenDisabled() {
    TenantUseCaseImpl useCase = new TenantUseCaseImpl(tenants, users, authentication, email, false);

    SignupResult result = useCase.signUp(signup("dono@loja.com"));

    assertFalse(result.active());
    assertNull(result.username());
    assertEquals(TenantStatus.PENDING_APPROVAL, result.tenant().getStatus());
    verify(authentication, never())
        .provision(anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Com consentimento, o cadastro é enviado à Meta com o mesmo eventId do navegador")
  void reportsSignupWhenConsented() {
    AdConversionsPort ads = mock(AdConversionsPort.class);
    TenantUseCaseImpl useCase =
        new TenantUseCaseImpl(tenants, users, authentication, email, true, ads);
    SelfSignup consented =
        new SelfSignup(
            "Mercado do João",
            "mercado-do-joao",
            "pro",
            "dono@loja.com",
            "senha-forte-123",
            "11912345678",
            new TermsAcceptance("2026-11-04", "200.1.1.1"),
            new AdTracking(
                "evt-1", "fb.1.1.2", null, "Mozilla", "https://www.binitechpdv.com.br/signup"));

    useCase.signUp(consented);

    ArgumentCaptor<AdConversionsPort.SignupConversion> sent =
        ArgumentCaptor.forClass(AdConversionsPort.SignupConversion.class);
    verify(ads).sendSignup(sent.capture());
    assertEquals("evt-1", sent.getValue().eventId());
    assertEquals("dono@loja.com", sent.getValue().email());
    assertEquals("200.1.1.1", sent.getValue().ip());
    assertEquals(new java.math.BigDecimal("149.00"), sent.getValue().monthlyValue());
  }

  @Test
  @DisplayName("Sem consentimento, nada é enviado à Meta")
  void silentWithoutConsent() {
    AdConversionsPort ads = mock(AdConversionsPort.class);
    TenantUseCaseImpl useCase =
        new TenantUseCaseImpl(tenants, users, authentication, email, true, ads);
    useCase.signUp(signup("dono@loja.com"));
    verify(ads, never()).sendSignup(any());
  }
}
