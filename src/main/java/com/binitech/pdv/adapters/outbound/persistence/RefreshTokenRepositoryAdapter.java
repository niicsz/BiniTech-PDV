package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.persistence.document.RefreshTokenDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataRefreshTokenRepository;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.domain.RefreshToken;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

  private final SpringDataRefreshTokenRepository repository;

  public RefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository repository) {
    this.repository = repository;
  }

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    RefreshTokenDocument doc = toDocument(refreshToken);
    RefreshTokenDocument saved = repository.save(doc);
    return toDomain(saved);
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return repository.findByToken(token).map(this::toDomain);
  }

  @Override
  public void deleteByUserId(String userId) {
    repository.deleteByUserId(userId);
  }

  @Override
  public void deleteByUserIdAndTenantId(String userId, String tenantId) {
    repository.deleteByUserIdAndTenantId(userId, tenantId);
  }

  private RefreshTokenDocument toDocument(RefreshToken rt) {
    return RefreshTokenDocument.builder()
        .id(rt.getId())
        .token(rt.getToken())
        .userId(rt.getUserId())
        .tenantId(rt.getTenantId())
        .expiryDate(rt.getExpiryDate())
        .build();
  }

  private RefreshToken toDomain(RefreshTokenDocument doc) {
    return new RefreshToken(
        doc.getId(), doc.getToken(), doc.getUserId(), doc.getTenantId(), doc.getExpiryDate());
  }
}
