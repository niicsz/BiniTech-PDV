package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.binitech.pdv.adapters.outbound.persistence.document.PasswordResetTokenDocument;
import com.binitech.pdv.adapters.outbound.persistence.repository.SpringDataPasswordResetTokenRepository;
import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.TenantRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.PasswordResetConfig;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.enums.Role;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetUseCaseImplTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private TenantRepositoryPort tenantRepository;
  @Mock private SpringDataPasswordResetTokenRepository resetTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private EmailServicePort emailService;
  @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
  @Mock private TokenBlacklistService tokenBlacklistService;

  private PasswordResetUseCaseImpl passwordResetUseCase;

  @BeforeEach
  void setUp() {
    passwordResetUseCase =
        new PasswordResetUseCaseImpl(
            userRepository,
            tenantRepository,
            resetTokenRepository,
            emailService,
            new PasswordResetConfig(
                "http://localhost:4200",
                passwordEncoder,
                refreshTokenRepository,
                tokenBlacklistService));
  }

  @Test
  @DisplayName("Redefinição de senha deve revogar todas as sessões do usuário")
  void resetPassword_shouldRevokeAllSessions() {
    PasswordResetTokenDocument resetToken =
        PasswordResetTokenDocument.builder()
            .token("reset-token")
            .userId("user1")
            .expiryDate(Instant.now().plusSeconds(300))
            .build();
    User user = new User("user1", "operator", "old-hash", Role.OPERATOR, "tenant1");
    when(resetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(resetToken));
    when(userRepository.findById("user1")).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

    passwordResetUseCase.resetPassword("reset-token", "new-password");

    assertEquals("new-hash", user.getPassword());
    verify(userRepository).save(user);
    verify(resetTokenRepository).deleteByUserId("user1");
    verify(refreshTokenRepository).deleteByUserId("user1");
    verify(tokenBlacklistService).revokeAllForUser("user1");
  }
}
