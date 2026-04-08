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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthUseCaseImpl implements AuthUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(AuthUseCaseImpl.class);

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
    log.info("Tentativa de login para o usuário: {}", username);
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> {
              log.warn("Login falhou - usuário não encontrado: {}", username);
              return new BusinessException("Credenciais inválidas.");
            });

    if (!passwordEncoder.matches(password, user.getPassword())) {
      log.warn("Login falhou - senha inválida para o usuário: {}", username);
      throw new BusinessException("Credenciais inválidas.");
    }

    String accessToken =
        jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name());

    refreshTokenRepository.deleteByUserId(user.getId());
    RefreshToken refreshToken = createRefreshToken(user.getId());

    log.info("Login realizado com sucesso: userId={} username={} role={}", user.getId(), user.getUsername(), user.getRole());
    return new AuthResult(
        accessToken, refreshToken.getToken(), user.getUsername(), user.getRole().name());
  }

  @Override
  public AuthResult register(String username, String password, Role role) {
    log.info("Tentativa de registro de novo usuário: {} com role: {}", username, role);
    if (userRepository.existsByUsername(username)) {
      log.warn("Registro falhou - usuário já existe: {}", username);
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

    log.info("Usuário registrado com sucesso: userId={} username={} role={}", saved.getId(), saved.getUsername(), saved.getRole());
    return new AuthResult(
        accessToken, refreshToken.getToken(), saved.getUsername(), saved.getRole().name());
  }

  @Override
  public AuthResult refreshToken(String refreshTokenStr) {
    log.info("Tentativa de refresh de token");
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenStr)
            .orElseThrow(() -> {
              log.warn("Refresh token inválido fornecido");
              return new BusinessException("Refresh token inválido.");
            });

    if (refreshToken.isExpired()) {
      log.warn("Refresh token expirado para userId={}", refreshToken.getUserId());
      refreshTokenRepository.deleteByUserId(refreshToken.getUserId());
      throw new BusinessException("Refresh token expirado. Faça login novamente.");
    }

    User user =
        userRepository
            .findById(refreshToken.getUserId())
            .orElseThrow(() -> {
              log.error("Usuário não encontrado durante refresh de token: userId={}", refreshToken.getUserId());
              return new BusinessException("Usuário não encontrado.");
            });

    String accessToken =
        jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name());

    refreshTokenRepository.deleteByUserId(user.getId());
    RefreshToken newRefreshToken = createRefreshToken(user.getId());

    log.info("Token renovado com sucesso para userId={} username={}", user.getId(), user.getUsername());
    return new AuthResult(
        accessToken, newRefreshToken.getToken(), user.getUsername(), user.getRole().name());
  }

  private RefreshToken createRefreshToken(String userId) {
    RefreshToken rt = new RefreshToken();
    rt.setToken(UUID.randomUUID().toString());
    rt.setUserId(userId);
    rt.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
    log.debug("Refresh token criado para userId={}", userId);
    return refreshTokenRepository.save(rt);
  }
}
