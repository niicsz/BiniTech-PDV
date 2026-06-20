package com.binitech.pdv.utils;

public final class LogSanitizer {

  private LogSanitizer() {}

  public static String neutralize(String value) {
    if (value == null) {
      return null;
    }
    // Strip CR/LF/TAB explicitly (prevents log forging/injection) and then any
    // remaining control characters. The explicit \r\n\t class is what static
    // analyzers (CodeQL java/log-injection) recognize as a log-injection barrier.
    return value.replaceAll("[\\r\\n\\t]", "_").replaceAll("\\p{Cntrl}", "_");
  }

  public static String maskUsername(String username) {
    if (username == null || username.isEmpty()) {
      return "***";
    }
    String safe = neutralize(username);
    int len = safe.length();
    if (len <= 2) {
      return "*".repeat(len);
    }
    if (len <= 4) {
      return safe.charAt(0) + "*".repeat(len - 1);
    }
    return safe.charAt(0) + "*".repeat(len - 2) + safe.charAt(len - 1);
  }

  public static String maskId(String id) {
    if (id == null || id.isEmpty()) {
      return "***";
    }
    String safe = neutralize(id);
    int len = safe.length();
    if (len <= 8) {
      return "*".repeat(len);
    }
    return safe.substring(0, 4) + "***" + safe.substring(len - 4);
  }
}
