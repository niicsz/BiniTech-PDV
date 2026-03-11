package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.UserDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataUserRepository extends MongoRepository<UserDocument, String> {

  Optional<UserDocument> findByUsername(String username);

  boolean existsByUsername(String username);
}
