package com.binitech.pdv.adapters.inbound.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
