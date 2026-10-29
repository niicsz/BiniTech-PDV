package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.TenantDataPurgePort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataRetentionUseCaseImplTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 29, 3, 30);

  @Mock private TenantRepositoryPort tenantRepository;
  @Mock private TenantDataPurgePort tenantDataPurge;
  @Mock private AuthenticationGateway authentication;

  private DataRetentionUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-10-29T03:30:00Z"), ZoneOffset.UTC);
    useCase =
        new DataRetentionUseCaseImpl(tenantRepository, tenantDataPurge, authentication, clock);
  }

  @Test
  void purgesCancelledTenantAfterRetentionAndAnonymizesIt() {
    Tenant tenant = tenant("t-1", TenantStatus.CANCELLED, NOW.minusDays(91));
    when(tenantRepository.findAllByStatus(TenantStatus.CANCELLED)).thenReturn(List.of(tenant));
    when(tenantRepository.findAllByStatus(TenantStatus.REJECTED)).thenReturn(List.of());

    int purged = useCase.purgeClosedTenants(90);

    assertEquals(1, purged);
    InOrder order = inOrder(authentication, tenantDataPurge, tenantRepository);
    order.verify(authentication).purgeTenant("t-1");
    order.verify(tenantDataPurge).purgeTenantData("t-1");
    order.verify(tenantRepository).save(tenant);
    assertEquals("Loja excluída", tenant.getName());
    assertEquals("excluida-t-1", tenant.getSlug());
    assertNull(tenant.getBillingEmail());
    assertNull(tenant.getTermsAcceptedIp());
    assertEquals(NOW, tenant.getDataPurgedAt());
    assertEquals(TenantStatus.CANCELLED, tenant.getStatus());
  }

  @Test
  void purgesRejectedTenantAfterRetention() {
    Tenant tenant = tenant("t-2", TenantStatus.REJECTED, NOW.minusDays(120));
    when(tenantRepository.findAllByStatus(TenantStatus.CANCELLED)).thenReturn(List.of());
    when(tenantRepository.findAllByStatus(TenantStatus.REJECTED)).thenReturn(List.of(tenant));

    assertEquals(1, useCase.purgeClosedTenants(90));
    verify(tenantDataPurge).purgeTenantData("t-2");
  }

  @Test
  void keepsTenantsInsideRetentionWindowOrAlreadyPurged() {
    Tenant recent = tenant("t-3", TenantStatus.CANCELLED, NOW.minusDays(89));
    Tenant alreadyPurged = tenant("t-4", TenantStatus.CANCELLED, NOW.minusDays(200));
    alreadyPurged.setDataPurgedAt(NOW.minusDays(100));
    Tenant withoutDate = tenant("t-5", TenantStatus.CANCELLED, null);
    when(tenantRepository.findAllByStatus(TenantStatus.CANCELLED))
        .thenReturn(List.of(recent, alreadyPurged, withoutDate));
    when(tenantRepository.findAllByStatus(TenantStatus.REJECTED)).thenReturn(List.of());

    assertEquals(0, useCase.purgeClosedTenants(90));
    verify(authentication, never()).purgeTenant(anyString());
    verify(tenantDataPurge, never()).purgeTenantData(anyString());
    verify(tenantRepository, never()).save(any());
  }

  @Test
  void neverQueriesActiveOrBlockedTenants() {
    when(tenantRepository.findAllByStatus(TenantStatus.CANCELLED)).thenReturn(List.of());
    when(tenantRepository.findAllByStatus(TenantStatus.REJECTED)).thenReturn(List.of());

    useCase.purgeClosedTenants(90);

    verify(tenantRepository, never()).findAllByStatus(TenantStatus.ACTIVE);
    verify(tenantRepository, never()).findAllByStatus(TenantStatus.BLOCKED);
    verify(tenantRepository, never()).findAllByStatus(TenantStatus.PENDING_APPROVAL);
  }

  @Test
  void keepsLocalDataWhenAuthPurgeFailsAndContinuesWithOthers() {
    Tenant failing = tenant("t-6", TenantStatus.CANCELLED, NOW.minusDays(100));
    Tenant ok = tenant("t-7", TenantStatus.CANCELLED, NOW.minusDays(100));
    when(tenantRepository.findAllByStatus(TenantStatus.CANCELLED)).thenReturn(List.of(failing, ok));
    when(tenantRepository.findAllByStatus(TenantStatus.REJECTED)).thenReturn(List.of());
    doThrow(new IllegalStateException("auth fora")).when(authentication).purgeTenant("t-6");

    assertEquals(1, useCase.purgeClosedTenants(90));
    verify(tenantDataPurge, never()).purgeTenantData("t-6");
    verify(tenantRepository, never()).save(failing);
    assertNull(failing.getDataPurgedAt());
    verify(tenantDataPurge).purgeTenantData("t-7");
  }

  @Test
  void rejectsNonPositiveRetention() {
    assertThrows(IllegalArgumentException.class, () -> useCase.purgeClosedTenants(0));
  }

  private Tenant tenant(String id, TenantStatus status, LocalDateTime updatedAt) {
    Tenant tenant = new Tenant();
    tenant.setId(id);
    tenant.setName("Mercado " + id);
    tenant.setSlug("mercado-" + id);
    tenant.setBillingEmail(id + "@loja.com");
    tenant.setTermsAcceptedIp("200.1.1.1");
    tenant.setStatus(status);
    tenant.setUpdatedAt(updatedAt);
    return tenant;
  }
}
