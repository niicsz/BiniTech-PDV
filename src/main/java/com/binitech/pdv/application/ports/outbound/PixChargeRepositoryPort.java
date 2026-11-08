package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.utils.enums.PixChargeStatus;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PixChargeRepositoryPort {

  PixCharge insert(PixCharge charge);

  PixCharge save(PixCharge charge);

  Optional<PixCharge> findById(String id);

  Optional<PixCharge> findByIdAndTenantId(String id, String tenantId);

  boolean transition(String id, PixChargeStatus expected, PixChargeStatus target);

  void markChecked(String id, LocalDateTime checkedAt);
}
