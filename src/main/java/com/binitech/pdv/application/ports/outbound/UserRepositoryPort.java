package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.enums.Role;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

  User save(User user);

  Optional<User> findById(String id);

  List<User> findAllByTenantId(String tenantId);

  Optional<User> findByUsername(String username);

  List<User> findAllByUsername(String username);

  boolean existsByUsername(String username);

  Optional<User> findByUsernameAndTenantId(String username, String tenantId);

  Optional<User> findByUsernameAndTenantIdIsNull(String username);

  boolean existsByUsernameAndTenantId(String username, String tenantId);

  long countByTenantId(String tenantId);

  long countByTenantIdAndRole(String tenantId, Role role);
}
