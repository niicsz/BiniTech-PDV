package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.User;
import java.util.Optional;

public interface UserRepositoryPort {

  User save(User user);

  Optional<User> findById(String id);

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);
}
