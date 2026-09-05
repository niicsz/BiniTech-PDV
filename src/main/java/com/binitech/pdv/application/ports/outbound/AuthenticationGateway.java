package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort.AuthResult;

/** Authentication is provided by the shared identity service. */
public interface AuthenticationGateway {
  AuthResult login(String username, String password, String tenantId);

  AuthResult refresh(String refreshToken);

  void logout(String accessToken);
}
