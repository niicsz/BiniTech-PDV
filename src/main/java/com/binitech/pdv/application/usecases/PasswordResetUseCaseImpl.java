package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.PasswordResetUseCasePort;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;

/** PDV only delivers mail. Recovery credentials and tokens belong to Auth. */
public class PasswordResetUseCaseImpl implements PasswordResetUseCasePort {
  private final AuthenticationGateway authentication;
  private final EmailServicePort email;
  private final String frontendUrl;

  public PasswordResetUseCaseImpl(
      AuthenticationGateway authentication, EmailServicePort email, String frontendUrl) {
    this.authentication = authentication;
    this.email = email;
    this.frontendUrl = frontendUrl;
  }

  public void requestReset(String username) {
    if (username == null || username.isBlank()) return;
    authentication
        .requestRecovery(username.trim())
        .ifPresent(
            delivery ->
                email.sendPasswordResetEmail(
                    delivery.email(),
                    "BiniTech",
                    delivery.username(),
                    frontendUrl + "/reset-password?token=" + delivery.token()));
  }

  public void resetPassword(String token, String newPassword) {
    authentication.resetPassword(token, newPassword);
  }
}
