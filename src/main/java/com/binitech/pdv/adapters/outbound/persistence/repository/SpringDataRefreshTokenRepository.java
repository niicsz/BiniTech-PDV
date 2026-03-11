package com.binitech.pdv.adapters.outbound.persistence.repository;

import com.binitech.pdv.adapters.outbound.persistence.document.RefreshTokenDocument;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataRefreshTokenRepository
    extends MongoRepository<RefreshTokenDocument, String> {

  Optional<RefreshTokenDocument> findByToken(String token);

  void deleteByUserId(String userId);
}
