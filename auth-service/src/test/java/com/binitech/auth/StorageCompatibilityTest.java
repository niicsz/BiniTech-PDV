package com.binitech.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.binitech.auth.IdentityStore.Identity;
import com.binitech.auth.SessionStore.RefreshSession;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;

class StorageCompatibilityTest {
  private MappingMongoConverter converter() {
    MongoMappingContext context = new MongoMappingContext();
    context.setSimpleTypeHolder(
        org.springframework.data.mongodb.core.convert.MongoCustomConversions.create(adapter -> {})
            .getSimpleTypeHolder());
    MappingMongoConverter converter =
        new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
    converter.afterPropertiesSet();
    return converter;
  }

  @Test
  void readsLegacyPdvIdentityAndRefreshDocuments() {
    ObjectId id = new ObjectId();
    Document user =
        new Document("_id", id)
            .append(
                "_class", "com.binitech.pdv.adapters.outbound.persistence.document.UserDocument")
            .append("username", "admin")
            .append("password", "existing-hash")
            .append("role", "TENANT_ADMIN")
            .append("tenantId", "tenant1");
    Identity identity = converter().read(Identity.class, user);
    assertEquals(id.toHexString(), identity.id());
    assertTrue(identity.isActive());
    assertEquals("existing-hash", identity.password());

    Document refresh =
        new Document("_id", new ObjectId())
            .append(
                "_class",
                "com.binitech.pdv.adapters.outbound.persistence.document.RefreshTokenDocument")
            .append("token", "legacy-token")
            .append("userId", id.toHexString())
            .append("tenantId", "tenant1")
            .append("expiryDate", Date.from(Instant.now()));
    RefreshSession session = converter().read(RefreshSession.class, refresh);
    assertEquals("legacy-token", session.token());
    assertNull(session.sessionVersion());
  }

  @Test
  void refreshConsumption_isOneAtomicMongoOperation() {
    MongoTemplate mongo = mock(MongoTemplate.class);
    SessionStore store = new SessionStore(mongo, mock(StringRedisTemplate.class));
    store.consume("refresh-token");
    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    verify(mongo).findAndRemove(query.capture(), eq(RefreshSession.class));
    assertEquals(new Document("token", "refresh-token"), query.getValue().getQueryObject());
    verifyNoMoreInteractions(mongo);
  }
}
