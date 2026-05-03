package com.binitech.pdv.utils;

public final class LogSanitizer {

  private LogSanitizer() {}

  public static String maskUsername(String username) {
    if (username == null || username.isEmpty()) {
      return "***";
    }
    int len = username.length();
    if (len <= 2) {
      return "*".repeat(len);
    }
    if (len <= 4) {
      return username.charAt(0) + "*".repeat(len - 1);
    }
    return username.charAt(0) + "*".repeat(len - 2) + username.charAt(len - 1);
  }

  public static String maskId(String id) {
    if (id == null || id.isEmpty()) {
      return "***";
    }
    int len = id.length();
    if (len <= 8) {
      return "*".repeat(len);
    }
    return id.substring(0, 4) + "***" + id.substring(len - 4);
  }
}
