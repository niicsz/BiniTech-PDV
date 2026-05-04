package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.AuthUseCasePort;
import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.config.JwtTokenProvider;
import com.binitech.pdv.config.TokenBlacklistService;
import com.binitech.pdv.domain.RefreshToken;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.Enum.Role;
import com.binitech.pdv.utils.LogSanitizer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthUseCaseImpl implements AuthUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(AuthUseCaseImpl.class);

  private final UserRepositoryPort userRepository;
  private final RefreshTokenRepositoryPort refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final TokenBlacklistService tokenBlacklistService;
  private final PasswordEncoder passwordEncoder;
  private final long refreshExpiration;
  private final String dummyPasswordHash;

  public AuthUseCaseImpl(
      UserRepositoryPort userRepository,
      RefreshTokenRepositoryPort refreshTokenRepository,
      JwtTokenProvider jwtTokenProvider,
      TokenBlacklistService tokenBlacklistService,
      PasswordEncoder passwordEncoder,
      long refreshExpiration,
      String dummyPasswordHash) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.tokenBlacklistService = tokenBlacklistService;
    this.passwordEncoder = passwordEncoder;
    this.refreshExpiration = refreshExpiration;
    this.dummyPasswordHash = dummyPasswordHash;
  }

  @Override
  public AuthResult login(String username, String password) {
    if (log.isInfoEnabled()) {
      log.info("Tentativa de login para usuário: {}", LogSanitizer.maskUsername(username));
    }
    Optional<User> userOpt = userRepository.findByUsername(username);

    boolean passwordMatches;
    if (userOpt.isEmpty()) {
      passwordEncoder.matches(password, dummyPasswordHash);
      passwordMatches = false;
    } else {
      passwordMatches = passwordEncoder.matches(password, userOpt.get().getPassword());
    }

    if (userOpt.isEmpty() || !passwordMatches) {
      if (log.isWarnEnabled()) {
        log.warn("Login falhou para usuário: {}", LogSanitizer.maskUsername(username));
      }
      throw new BusinessException("Credenciais inválidas.");
    }

    User user = userOpt.get();

    String accessToken =
        jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name());

    refreshTokenRepository.deleteByUserId(user.getId());
    RefreshToken refreshToken = createRefreshToken(user.getId());

    if (log.isInfoEnabled()) {
      log.info(
          "Login realizado com sucesso: userId={} role={}",
          LogSanitizer.maskId(user.getId()),
          user.getRole());
    }
    return new AuthResult(
        accessToken, refreshToken.getToken(), user.getUsername(), user.getRole().name());
  }

  @Override
  public AuthResult register(String username, String password, Role role) {
    if (log.isInfoEnabled()) {
      log.info(
          "Tentativa de registro de novo usuário: {} role={}",
          LogSanitizer.maskUsername(username),
          role);
    }
    if (userRepository.existsByUsername(username)) {
      if (log.isWarnEnabled()) {
        log.warn("Registro falhou - usuário já existe: {}", LogSanitizer.maskUsername(username));
      }
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

    if (log.isInfoEnabled()) {
      log.info(
          "Usuário registrado com sucesso: userId={} role={}",
          LogSanitizer.maskId(saved.getId()),
          saved.getRole());
    }
    return new AuthResult(
        accessToken, refreshToken.getToken(), saved.getUsername(), saved.getRole().name());
  }

  @Override
  public AuthResult refreshToken(String refreshTokenStr) {
    log.info("Tentativa de refresh de token");
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenStr)
            .orElseThrow(
                () -> {
                  log.warn("Refresh token inválido fornecido");
                  return new BusinessException("Refresh token inválido.");
                });

    if (refreshToken.isExpired()) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Refresh token expirado para userId={}", LogSanitizer.maskId(refreshToken.getUserId()));
      }
      refreshTokenRepository.deleteByUserId(refreshToken.getUserId());
      throw new BusinessException("Refresh token expirado. Faça login novamente.");
    }

    User user =
        userRepository
            .findById(refreshToken.getUserId())
            .orElseThrow(
                () -> {
                  if (log.isErrorEnabled()) {
                    log.error(
                        "Usuário não encontrado durante refresh de token: userId={}",
                        LogSanitizer.maskId(refreshToken.getUserId()));
                  }
                  return new BusinessException("Usuário não encontrado.");
                });

    String accessToken =
        jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name());

    refreshTokenRepository.deleteByUserId(user.getId());
    RefreshToken newRefreshToken = createRefreshToken(user.getId());

    if (log.isInfoEnabled()) {
      log.info("Token renovado com sucesso para userId={}", LogSanitizer.maskId(user.getId()));
    }
    return new AuthResult(
        accessToken, newRefreshToken.getToken(), user.getUsername(), user.getRole().name());
  }

  @Override
  public void logout(String accessToken) {
    log.info("Realizando logout e invalidando token");
    tokenBlacklistService.blacklist(accessToken);
    if (jwtTokenProvider.validateToken(accessToken)) {
      String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
      refreshTokenRepository.deleteByUserId(userId);
      if (log.isInfoEnabled()) {
        log.info("Logout realizado com sucesso para userId={}", LogSanitizer.maskId(userId));
      }
    }
  }

  private RefreshToken createRefreshToken(String userId) {
    RefreshToken rt = new RefreshToken();
    rt.setToken(UUID.randomUUID().toString());
    rt.setUserId(userId);
    rt.setExpiryDate(Instant.now().plusMillis(refreshExpiration));
    if (log.isDebugEnabled()) {
      log.debug("Refresh token criado para userId={}", LogSanitizer.maskId(userId));
    }
    return refreshTokenRepository.save(rt);
  }
}
