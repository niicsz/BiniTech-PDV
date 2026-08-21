package com.binitech.pdv.application.ports.inbound;

public interface PasswordResetUseCasePort {

  void requestReset(String username);

  void resetPassword(String token, String newPassword);
}
