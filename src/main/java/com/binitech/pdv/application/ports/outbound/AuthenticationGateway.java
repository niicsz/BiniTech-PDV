package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.AuthResult;

/** Authentication is provided by the shared identity service. */
public interface AuthenticationGateway {
  AuthResult login(String username, String password, String tenantId);

  AuthResult refresh(String refreshToken);

  void logout(String accessToken);

  SessionIdentity session(String accessToken);

  void provision(
      String identityId, String username, String password, String tenantId, String recoveryEmail);

  void changePassword(String identityId, String currentPassword, String newPassword);

  void revokeSessions(String identityId);

  java.util.Optional<RecoveryDelivery> requestRecovery(String username);

  void resetPassword(String token, String newPassword);

  record SessionIdentity(String userId, String username, String role, String tenantId) {}

  record RecoveryDelivery(String username, String email, String token) {}
}
