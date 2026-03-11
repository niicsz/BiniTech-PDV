package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.utils.Enum.Role;

public interface AuthUseCasePort {

  AuthResult login(String username, String password);

  AuthResult register(String username, String password, Role role);

  AuthResult refreshToken(String refreshToken);

  record AuthResult(String accessToken, String refreshToken, String username, String role) {}
}
