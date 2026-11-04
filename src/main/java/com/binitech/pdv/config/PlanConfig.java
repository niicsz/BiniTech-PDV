package com.binitech.pdv.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlanConfig {

  public static final String PLAN_FREE = "free";
  public static final int UNLIMITED = Integer.MAX_VALUE;

  public static final BigDecimal EXCESS_PER_PRODUCT = new BigDecimal("0.10");
  public static final BigDecimal EXCESS_PER_SALE = new BigDecimal("0.05");
  public static final BigDecimal EXCESS_PER_OPERATOR = new BigDecimal("30.00");
  public static final List<String> PAID_PLANS = List.of("starter", "pro", "enterprise");
  public static final int MAX_EXTRA_USERS = 50;

  public record PlanLimits(
      int maxProducts, int maxSalesPerMonth, int maxOperators, BigDecimal baseMonthlyFee) {}

  public static PlanLimits getLimits(String planTier) {
    return switch (normalize(planTier)) {
      case "starter" -> new PlanLimits(3000, UNLIMITED, 1, new BigDecimal("99.00"));
      case "pro" -> new PlanLimits(10000, UNLIMITED, 3, new BigDecimal("149.00"));
      case "enterprise" -> new PlanLimits(UNLIMITED, UNLIMITED, 10, new BigDecimal("349.00"));
      case PLAN_FREE ->
          new PlanLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, BigDecimal.ZERO);
      default -> throw new IllegalArgumentException("Plano desconhecido: " + planTier);
    };
  }

  public static BigDecimal monthlyAmount(String planTier, int extraUsers) {
    return getLimits(planTier)
        .baseMonthlyFee()
        .add(EXCESS_PER_OPERATOR.multiply(BigDecimal.valueOf(Math.max(0, extraUsers))));
  }

  public static int operatorLimit(String planTier, int extraUsers) {
    int base = getLimits(planTier).maxOperators();
    return base == Integer.MAX_VALUE ? base : base + Math.max(0, extraUsers);
  }

  public static boolean isFree(String planTier) {
    return planTier != null && PLAN_FREE.equalsIgnoreCase(planTier.trim());
  }

  private static String normalize(String planTier) {
    if (planTier == null || planTier.isBlank()) {
      throw new IllegalArgumentException("Plano desconhecido: " + planTier);
    }
    return planTier.toLowerCase(Locale.ROOT);
  }
}
