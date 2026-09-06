package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.domain.User;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Auth's insert-only account is the durable provisioning record. Retrying cannot replace a
 * password. The membership is activated only after the identity exists. A failed local write can be
 * retried with the same tenant/username/password without creating another identity.
 */
public class IdentityProvisioningUseCase {
  private final UserRepositoryPort users;
  private final AuthenticationGateway authentication;

  public IdentityProvisioningUseCase(
      UserRepositoryPort users, AuthenticationGateway authentication) {
    this.users = users;
    this.authentication = authentication;
  }

  public User provision(User membership, String password) {
    String key = "pdv:" + membership.getTenantId() + ":" + membership.getUsername();
    membership.setId(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString());
    authentication.provision(
        membership.getId(),
        membership.getUsername(),
        password,
        membership.getTenantId(),
        membership.getEmail());
    return users.save(membership);
  }
}
