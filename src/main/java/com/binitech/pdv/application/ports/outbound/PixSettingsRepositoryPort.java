package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.PixSettings;
import java.util.Optional;

public interface PixSettingsRepositoryPort {

  Optional<PixSettings> findByTenantId(String tenantId);

  PixSettings save(PixSettings settings);
}
