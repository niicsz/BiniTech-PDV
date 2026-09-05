package com.binitech.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/** Reads identities without importing application-specific roles or business rules. */
@Repository
public class IdentityStore {
  private final MongoTemplate mongo;

  public IdentityStore(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  public List<Identity> findCandidates(String username, String tenantId) {
    Criteria criteria = Criteria.where("username").is(username);
    if (tenantId != null && !tenantId.isBlank()) {
      criteria = criteria.and("tenantId").is(tenantId);
    }
    return mongo.find(Query.query(criteria), Identity.class);
  }

  public Optional<Identity> findById(String id) {
    return Optional.ofNullable(mongo.findById(id, Identity.class));
  }

  @Document("users")
  public record Identity(
      @Id String id,
      String username,
      String password,
      String role,
      String tenantId,
      Boolean active) {
    public boolean isActive() {
      return !Boolean.FALSE.equals(active);
    }
  }
}
