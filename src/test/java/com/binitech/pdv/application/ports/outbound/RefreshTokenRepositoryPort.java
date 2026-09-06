package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {

  RefreshToken save(RefreshToken refreshToken);

  Optional<RefreshToken> findByToken(String token);

  void deleteByUserId(String userId);

  void deleteByUserIdAndTenantId(String userId, String tenantId);
}
