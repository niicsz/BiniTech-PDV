package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.binitech.pdv.application.ports.outbound.*;
import com.binitech.pdv.domain.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PasswordResetUseCaseImplTest {
  AuthenticationGateway auth = mock(AuthenticationGateway.class);
  EmailServicePort email = mock(EmailServicePort.class);
  PasswordResetUseCaseImpl useCase =
      new PasswordResetUseCaseImpl(auth, email, "https://pdv.example");

  @Test
  void deliversOnlyToAuthSelectedRecoveryContact() {
    when(auth.requestRecovery("user"))
        .thenReturn(
            Optional.of(
                new AuthenticationGateway.RecoveryDelivery(
                    "user", "recovery@example.com", "token")));
    useCase.requestReset(" user ");
    verify(email)
        .sendPasswordResetEmail(
            "recovery@example.com",
            "BiniTech",
            "user",
            "https://pdv.example/reset-password?token=token");
  }

  @Test
  void unknownAccountDoesNotSendMail() {
    when(auth.requestRecovery("unknown")).thenReturn(Optional.empty());
    useCase.requestReset("unknown");
    verifyNoInteractions(email);
  }

  @Test
  void blankUsernameDoesNothing() {
    useCase.requestReset(" ");
    verifyNoInteractions(auth, email);
  }

  @Test
  void delegatesResetWithoutLocalCredentialPersistence() {
    useCase.resetPassword("token", "new-password");
    verify(auth).resetPassword("token", "new-password");
  }

  @Test
  void invalidTokenIsRejected() {
    doThrow(new BusinessException("Invalid token")).when(auth).resetPassword("bad", "new-password");
    assertThrows(BusinessException.class, () -> useCase.resetPassword("bad", "new-password"));
  }
}
