package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.utils.enums.Role;
import java.time.Instant;

public interface AuthUseCasePort {

  LoginOutcome login(String username, String password, String tenantId);

  AuthResult completeTwoFactorLogin(String challengeToken, String code);

  AuthResult register(String username, String password, Role role, String tenantId);

  AuthResult refreshToken(String refreshToken);

  void changePassword(String userId, String currentPassword, String newPassword);

  void logout(String accessToken);

  TwoFactorStatus twoFactorStatus(String accessToken);

  TwoFactorSetup setupTwoFactor(String accessToken);

  void enableTwoFactor(String accessToken, String code);

  void disableTwoFactor(String accessToken, String code);

  sealed interface LoginOutcome permits AuthResult, TwoFactorChallenge {}

  record AuthResult(
      String accessToken, String refreshToken, String username, String role, String tenantId)
      implements LoginOutcome {}

  record TwoFactorChallenge(String challengeToken, Instant expiresAt) implements LoginOutcome {}

  record TwoFactorStatus(boolean enabled, Instant lockedUntil) {}

  record TwoFactorSetup(
      String secret, String otpauthUri, String algorithm, int digits, long period) {}
}
