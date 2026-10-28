package com.binitech.pdv.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

public final class Money {

  private Money() {}

  public static BigDecimal of(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  public static double round(double value) {
    return of(value).doubleValue();
  }

  public static double multiply(double value, int quantity) {
    return of(value)
        .multiply(BigDecimal.valueOf(quantity))
        .setScale(2, RoundingMode.HALF_UP)
        .doubleValue();
  }

  public static double sum(Stream<Double> values) {
    return values.map(Money::of).reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue();
  }

  public static int compare(double a, double b) {
    return of(a).compareTo(of(b));
  }
}
