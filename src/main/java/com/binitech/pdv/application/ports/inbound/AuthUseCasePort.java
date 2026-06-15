package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.utils.Enum.Role;

public interface AuthUseCasePort {

  AuthResult login(String username, String password, String tenantId);

  AuthResult register(String username, String password, Role role, String tenantId);

  AuthResult refreshToken(String refreshToken);

  void changePassword(String userId, String currentPassword, String newPassword);

  void logout(String accessToken);

  record AuthResult(
      String accessToken, String refreshToken, String username, String role, String tenantId) {}
}
