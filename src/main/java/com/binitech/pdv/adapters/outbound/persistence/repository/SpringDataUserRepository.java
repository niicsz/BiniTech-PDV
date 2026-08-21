package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.UserDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataUserRepository extends MongoRepository<UserDocument, String> {

  Optional<UserDocument> findByUsername(String username);

  List<UserDocument> findAllByUsername(String username);

  boolean existsByUsername(String username);

  Optional<UserDocument> findByUsernameAndTenantId(String username, String tenantId);

  Optional<UserDocument> findByUsernameAndTenantIdIsNull(String username);

  boolean existsByUsernameAndTenantId(String username, String tenantId);

  long countByTenantId(String tenantId);

  List<UserDocument> findAllByTenantId(String tenantId);
}
