package com.binitech.pdv.domain;

import java.time.LocalDateTime;

public record PixSettings(
    String tenantId,
    boolean enabled,
    String accessToken,
    String webhookSecret,
    String accountName,
    LocalDateTime updatedAt) {

  public static PixSettings empty(String tenantId) {
    return new PixSettings(tenantId, false, null, null, null, null);
  }

  public boolean configured() {
    return accessToken != null && !accessToken.isBlank();
  }

  public boolean active() {
    return enabled && configured();
  }

  public String accessTokenHint() {
    if (!configured()) {
      return null;
    }
    String token = accessToken.trim();
    return token.length() <= 4 ? "••••" : "••••" + token.substring(token.length() - 4);
  }
}
