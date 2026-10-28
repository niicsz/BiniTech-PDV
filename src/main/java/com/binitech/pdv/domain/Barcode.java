package com.binitech.pdv.domain;

import java.util.Set;

public final class Barcode {

  private static final Set<Integer> GTIN_LENGTHS = Set.of(8, 12, 13, 14);

  private Barcode() {}

  public static boolean isValid(String value) {
    if (value == null || value.isBlank() || value.length() > 64) return false;
    if (value.chars().anyMatch(Character::isISOControl)) return false;
    if (!value.matches("\\d+") || !GTIN_LENGTHS.contains(value.length())) return true;
    int sum = 0;
    for (int i = value.length() - 2, weight = 3; i >= 0; i--, weight = weight == 3 ? 1 : 3) {
      sum += (value.charAt(i) - '0') * weight;
    }
    int check = (10 - sum % 10) % 10;
    return check == value.charAt(value.length() - 1) - '0';
  }
}
