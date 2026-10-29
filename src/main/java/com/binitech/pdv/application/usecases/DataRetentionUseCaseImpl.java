package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.DataRetentionUseCasePort;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.TenantDataPurgePort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.utils.LogSanitizer;
import com.binitech.pdv.utils.enums.TenantStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataRetentionUseCaseImpl implements DataRetentionUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(DataRetentionUseCaseImpl.class);
  private static final String PURGED_NAME = "Loja excluída";

  private final TenantRepositoryPort tenantRepository;
  private final TenantDataPurgePort tenantDataPurge;
  private final AuthenticationGateway authentication;
  private final Clock clock;

  public DataRetentionUseCaseImpl(
      TenantRepositoryPort tenantRepository,
      TenantDataPurgePort tenantDataPurge,
      AuthenticationGateway authentication,
      Clock clock) {
    this.tenantRepository = tenantRepository;
    this.tenantDataPurge = tenantDataPurge;
    this.authentication = authentication;
    this.clock = clock;
  }

  @Override
  public int purgeClosedTenants(int retentionDays) {
    if (retentionDays < 1) {
      throw new IllegalArgumentException("retentionDays deve ser maior que zero");
    }
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime cutoff = now.minusDays(retentionDays);
    List<Tenant> expired =
        Stream.of(TenantStatus.CANCELLED, TenantStatus.REJECTED)
            .flatMap(status -> tenantRepository.findAllByStatus(status).stream())
            .filter(tenant -> tenant.getDataPurgedAt() == null)
            .filter(
                tenant -> tenant.getUpdatedAt() != null && tenant.getUpdatedAt().isBefore(cutoff))
            .toList();

    int purged = 0;
    for (Tenant tenant : expired) {
      try {
        authentication.purgeTenant(tenant.getId());
        tenantDataPurge.purgeTenantData(tenant.getId());
        anonymize(tenant, now);
        tenantRepository.save(tenant);
        purged++;
        if (log.isInfoEnabled()) {
          log.info("Dados do tenant eliminados: tenantId={}", LogSanitizer.maskId(tenant.getId()));
        }
      } catch (RuntimeException e) {
        log.error(
            "Falha ao eliminar dados do tenant: tenantId={}",
            LogSanitizer.maskId(tenant.getId()),
            e);
      }
    }
    return purged;
  }

  private void anonymize(Tenant tenant, LocalDateTime now) {
    tenant.setName(PURGED_NAME);
    tenant.setSlug("excluida-" + tenant.getId());
    tenant.setBillingEmail(null);
    tenant.setBlockReason(null);
    tenant.setTermsAcceptedIp(null);
    tenant.setDataPurgedAt(now);
    tenant.setUpdatedAt(now);
  }
}
