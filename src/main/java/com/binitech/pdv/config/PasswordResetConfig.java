package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.outbound.RefreshTokenRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;

public record PasswordResetConfig(
    String frontendUrl,
    PasswordEncoder passwordEncoder,
    RefreshTokenRepositoryPort refreshTokenRepository,
    TokenBlacklistService tokenBlacklistService) {}
