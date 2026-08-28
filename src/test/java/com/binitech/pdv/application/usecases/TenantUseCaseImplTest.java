package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TenantUseCaseImplTest {

  @Mock private TenantRepositoryPort tenantRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailServicePort emailService;

  private TenantUseCaseImpl tenantUseCase;

  @BeforeEach
  void setUp() {
    tenantUseCase =
        new TenantUseCaseImpl(tenantRepository, userRepository, passwordEncoder, emailService);
  }

  @Test
  void rejectPendingTenantStoresReasonAndRejectedStatus() {
    Tenant tenant = tenant(TenantStatus.PENDING_APPROVAL);
    when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));
    when(tenantRepository.save(tenant)).thenReturn(tenant);

    Tenant rejected = tenantUseCase.rejectTenant("tenant-1", "Dados cadastrais inválidos");

    assertEquals(TenantStatus.REJECTED, rejected.getStatus());
    assertEquals("Dados cadastrais inválidos", rejected.getBlockReason());
    assertNull(rejected.getBlockedAt());
    verify(tenantRepository).save(tenant);
  }

  @Test
  void rejectNonPendingTenantIsNotAllowed() {
    Tenant tenant = tenant(TenantStatus.ACTIVE);
    when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));

    assertThrows(
        BusinessException.class,
        () -> tenantUseCase.rejectTenant("tenant-1", "Motivo da reprovação"));

    verify(tenantRepository, never()).save(tenant);
  }

  @Test
  void approveRejectedTenantIsNotAllowed() {
    Tenant tenant = tenant(TenantStatus.REJECTED);
    when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));

    assertThrows(BusinessException.class, () -> tenantUseCase.approveTenant("tenant-1"));

    verify(tenantRepository, never()).save(tenant);
  }

  private Tenant tenant(TenantStatus status) {
    Tenant tenant = new Tenant();
    tenant.setId("tenant-1");
    tenant.setStatus(status);
    return tenant;
  }
}
