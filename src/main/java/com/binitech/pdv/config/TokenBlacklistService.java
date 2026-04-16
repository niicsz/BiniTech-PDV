package com.binitech.pdv.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TokenBlacklistService {

  private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

  public void blacklist(String token) {
    blacklistedTokens.add(token);
  }

  public boolean isBlacklisted(String token) {
    return blacklistedTokens.contains(token);
  }
}
