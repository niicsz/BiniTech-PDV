package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.FiscalProfile;
import java.util.Optional;

public interface FiscalProfileRepositoryPort {

  FiscalProfile save(FiscalProfile profile);

  Optional<FiscalProfile> findByTenantId(String tenantId);
}
