package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.AuthResult;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.LoginOutcome;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.TwoFactorSetup;
import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.TwoFactorStatus;
import java.util.List;

public interface AuthenticationGateway {
  LoginOutcome login(String username, String password, String tenantId);

  AuthResult completeTwoFactorLogin(String challengeToken, String code);

  TwoFactorStatus twoFactorStatus(String accessToken);

  TwoFactorSetup setupTwoFactor(String accessToken);

  List<String> enableTwoFactor(String accessToken, String code);

  List<String> regenerateRecoveryCodes(String accessToken, String code);

  void disableTwoFactor(String accessToken, String code);

  AuthResult refresh(String refreshToken);

  void logout(String accessToken);

  SessionIdentity session(String accessToken);

  void provision(
      String identityId, String username, String password, String tenantId, String recoveryEmail);

  void changePassword(String identityId, String currentPassword, String newPassword);

  void revokeSessions(String identityId);

  boolean resetTwoFactor(String identityId);

  void purgeTenant(String tenantId);

  java.util.Optional<RecoveryDelivery> requestRecovery(String username);

  void resetPassword(String token, String newPassword);

  record SessionIdentity(String userId, String username, String role, String tenantId) {}

  record RecoveryDelivery(String username, String email, String token) {}
}
