package com.binitech.pdv.adapters.outbound.persistence;

import com.binitech.pdv.adapters.outbound.SecretCipher;
import com.binitech.pdv.adapters.outbound.persistence.document.PixSettingsDocument;
import com.binitech.pdv.application.ports.outbound.PixSettingsRepositoryPort;
import com.binitech.pdv.domain.PixSettings;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class PixSettingsRepositoryAdapter implements PixSettingsRepositoryPort {

  private static final String PROVIDER = "MERCADO_PAGO";

  private final MongoTemplate mongo;
  private final SecretCipher cipher;

  public PixSettingsRepositoryAdapter(MongoTemplate mongo, SecretCipher cipher) {
    this.mongo = mongo;
    this.cipher = cipher;
  }

  @Override
  public Optional<PixSettings> findByTenantId(String tenantId) {
    return Optional.ofNullable(mongo.findById(tenantId, PixSettingsDocument.class))
        .map(this::toDomain);
  }

  @Override
  public PixSettings save(PixSettings settings) {
    PixSettingsDocument document =
        PixSettingsDocument.builder()
            .tenantId(settings.tenantId())
            .enabled(settings.enabled())
            .provider(PROVIDER)
            .accessTokenEncrypted(cipher.encrypt(settings.accessToken()))
            .webhookSecretEncrypted(cipher.encrypt(settings.webhookSecret()))
            .accountName(settings.accountName())
            .updatedAt(settings.updatedAt())
            .build();
    return toDomain(mongo.save(document));
  }

  private PixSettings toDomain(PixSettingsDocument document) {
    return new PixSettings(
        document.getTenantId(),
        document.isEnabled(),
        cipher.decrypt(document.getAccessTokenEncrypted()),
        cipher.decrypt(document.getWebhookSecretEncrypted()),
        document.getAccountName(),
        document.getUpdatedAt());
  }
}
