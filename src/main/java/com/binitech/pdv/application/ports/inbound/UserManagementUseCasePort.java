package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.enums.Role;
import java.util.List;

public interface UserManagementUseCasePort {

  List<User> listUsers(String tenantId, Role actorRole);

  User getUser(String userId, String tenantId, Role actorRole);

  User createUser(
      String name, String email, String password, Role role, String tenantId, Role actorRole);

  User updateStatus(
      String userId, boolean active, String actorUserId, String tenantId, Role actorRole);

  User updateRole(String userId, Role role, String actorUserId, String tenantId, Role actorRole);
}
