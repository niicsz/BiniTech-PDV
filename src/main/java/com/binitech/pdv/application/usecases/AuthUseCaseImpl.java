package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.domain.RefreshToken;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.Enum.Role;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthUseCaseImpl implements AuthUseCasePort {

  private final UserRepositoryPort userRepository;
  private final RefreshTokenRepositoryPort refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final long refreshExpiration;

  public AuthUseCaseImpl(
      UserRepositoryPort userRepository,
      RefreshTokenRepositoryPort refreshTokenRepository,
      JwtTokenProvider jwtTokenProvider,
      PasswordEncoder passwordEncoder,
      long refreshExpiration) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.passwordEncoder = passwordEncoder;
    this.refreshExpiration = refreshExpiration;
  }

  @Override
  public AuthResult login(String username, String password) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new BusinessException("Credenciais inválidas."));

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new BusinessException("Credenciais inválidas.");
    }

    String accessToken =
        jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name());

    refreshTokenRepository.deleteByUserId(user.getId());
    RefreshToken refreshToken = createRefreshToken(user.getId());

    return new AuthResult(
        accessToken, refreshToken.getToken(), user.getUsername(), user.getRole().name());
  }

  @Override
  public AuthResult register(String username, String password, Role role) {
    if (userRepository.existsByUsername(username)) {
      throw new BusinessException("Usuário já existe com o username: " + username);
    }

    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(role);
    User saved = userRepository.save(user);

    String accessToken =
        jwtTokenProvider.generateAccessToken(
            saved.getId(), saved.getUsername(), saved.getRole().name());

    RefreshToken refreshToken = createRefreshToken(saved.getId());

    return new AuthResult(
        accessToken, refreshToken.getToken(), saved.getUsername(), saved.getRole().name());
  }

  @Override
  public AuthResult refreshToken(String refreshTokenStr) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenStr)
            .orElseThrow(() -> new BusinessException("Refresh token inválido."));

    if (refreshToken.isExpired()) {
      refreshTokenRepository.deleteByUserId(refreshToken.getUserId());
      throw new BusinessException("Refresh token expirado. Faça login novamente.");
    }

    User user =
        userRepository
            .findById(refreshToken.getUserId())
            .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

    String accessToken =
        jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name());

    refreshTokenRepository.deleteByUserId(user.getId());
    RefreshToken newRefreshToken = createRefreshToken(user.getId());

    return new AuthResult(
        accessToken, newRefreshToken.getToken(), user.getUsername(), user.getRole().name());
  }

  private RefreshToken createRefreshToken(String userId) {
    RefreshToken rt = new RefreshToken();
    rt.setToken(UUID.randomUUID().toString());
    rt.setUserId(userId);
    rt.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
    return refreshTokenRepository.save(rt);
  }
}
