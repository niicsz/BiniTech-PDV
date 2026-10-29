package com.binitech.pdv.adapters.inbound.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import com.binitech.pdv.application.ports.inbound.TenantUseCasePort;
import com.binitech.pdv.domain.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

  @Mock private TenantUseCasePort tenantUseCase;

  @Mock private BillingUseCasePort billingUseCase;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TenantController(tenantUseCase, billingUseCase))
            .build();
  }

  @Test
  void publicTenantLookupReturnsOnlyPublicIdentificationFields() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setId("internal-tenant-id");
    tenant.setName("Mercado do Bairro");
    tenant.setSlug("mercado-do-bairro");
    tenant.setBillingEmail("billing@example.com");

    when(tenantUseCase.getTenantBySlug("mercado-do-bairro")).thenReturn(tenant);

    mockMvc
        .perform(get("/api/public/tenants/slug/{slug}", "mercado-do-bairro"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Mercado do Bairro"))
        .andExpect(jsonPath("$.slug").value("mercado-do-bairro"))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.billingEmail").doesNotExist())
        .andExpect(jsonPath("$.status").doesNotExist())
        .andExpect(jsonPath("$.trialEndsAt").doesNotExist())
        .andExpect(jsonPath("$.blockedAt").doesNotExist())
        .andExpect(jsonPath("$.blockReason").doesNotExist())
        .andExpect(jsonPath("$.createdAt").doesNotExist())
        .andExpect(jsonPath("$.updatedAt").doesNotExist());
  }

  @Test
  void rejectTenantRequiresReasonAndReturnsUpdatedTenant() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setId("tenant-1");
    tenant.setName("Loja pendente");
    tenant.setStatus(com.binitech.pdv.utils.enums.TenantStatus.REJECTED);
    tenant.setBlockReason("Cadastro inconsistente");
    when(tenantUseCase.rejectTenant("tenant-1", "Cadastro inconsistente")).thenReturn(tenant);

    mockMvc
        .perform(
            post("/api/admin/tenants/{id}/reject", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Cadastro inconsistente\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.blockReason").value("Cadastro inconsistente"));
  }

  @Test
  void rejectTenantWithoutReasonReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/tenants/{id}/reject", "tenant-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void signupRecordsTermsAcceptanceWithClientIp() throws Exception {
    Tenant tenant = new Tenant();
    tenant.setId("tenant-1");
    tenant.setName("Mercado do João");
    when(tenantUseCase.createTenant(
            eq("Mercado do João"),
            eq("mercado-do-joao"),
            eq("starter"),
            eq("contato@mercado.com"),
            any(TenantUseCasePort.TermsAcceptance.class)))
        .thenReturn(tenant);

    mockMvc
        .perform(
            post("/api/public/tenants")
                .header("X-Forwarded-For", "200.10.20.30, 10.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupBody("\"acceptedTerms\":true,")))
        .andExpect(status().isCreated());

    verify(tenantUseCase)
        .createTenant(
            "Mercado do João",
            "mercado-do-joao",
            "starter",
            "contato@mercado.com",
            new TenantUseCasePort.TermsAcceptance("2026-10-29", "200.10.20.30"));
  }

  @Test
  void signupWithoutAcceptedTermsReturnsBadRequest() throws Exception {
    for (String accepted : new String[] {"\"acceptedTerms\":false,", ""}) {
      mockMvc
          .perform(
              post("/api/public/tenants")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(signupBody(accepted)))
          .andExpect(status().isBadRequest());
    }
    verify(tenantUseCase, never())
        .createTenant(anyString(), anyString(), anyString(), anyString(), any());
  }

  private String signupBody(String acceptedTerms) {
    return "{\"name\":\"Mercado do João\",\"planId\":\"starter\","
        + "\"billingEmail\":\"contato@mercado.com\","
        + acceptedTerms
        + "\"termsVersion\":\"2026-10-29\"}";
  }
}
