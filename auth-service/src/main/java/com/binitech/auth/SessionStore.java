package com.binitech.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionStore {
  private final MongoTemplate mongo;
  private final StringRedisTemplate redis;

  public SessionStore(MongoTemplate mongo, StringRedisTemplate redis) {
    this.mongo = mongo;
    this.redis = redis;
  }

  public void save(RefreshSession session) {
    mongo.insert(session);
  }

  /** Atomic consumption prevents concurrent refresh requests from reusing the same token. */
  public Optional<RefreshSession> consume(String token) {
    return Optional.ofNullable(
        mongo.findAndRemove(Query.query(Criteria.where("token").is(token)), RefreshSession.class));
  }

  public void deleteForUser(String userId, String tenantId) {
    mongo.remove(
        Query.query(Criteria.where("userId").is(userId).and("tenantId").is(tenantId)),
        RefreshSession.class);
  }

  public long sessionVersion(String userId) {
    String value = redis.opsForValue().get("user:session-version:" + userId);
    return value == null ? 0L : Long.parseLong(value);
  }

  public boolean isBlacklisted(String token) {
    return Boolean.TRUE.equals(redis.hasKey("token:blacklist:" + token));
  }

  public void blacklist(String token, long ttlMillis) {
    redis.opsForValue().set("token:blacklist:" + token, "1", ttlMillis, TimeUnit.MILLISECONDS);
  }

  @Document("refresh_tokens")
  public record RefreshSession(
      @Id String id,
      @Indexed(unique = true) String token,
      String userId,
      String tenantId,
      @Indexed(expireAfter = "0s") Instant expiryDate,
      Long sessionVersion) {}
}
