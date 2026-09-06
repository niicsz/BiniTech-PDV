package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.PasswordResetTokenDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPasswordResetTokenRepository
    extends MongoRepository<PasswordResetTokenDocument, String> {

  Optional<PasswordResetTokenDocument> findByToken(String token);

  void deleteByUserId(String userId);
}
