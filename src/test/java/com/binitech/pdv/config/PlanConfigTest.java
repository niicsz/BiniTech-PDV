package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlanConfigTest {

  @Test
  @DisplayName("getLimits resolve limites e tarifa-base de cada plano pago")
  void getLimits_paidPlans_shouldResolve() {
    assertEquals(new BigDecimal("99.00"), PlanConfig.getLimits("starter").baseMonthlyFee());
    assertEquals(new BigDecimal("199.00"), PlanConfig.getLimits("pro").baseMonthlyFee());
    assertEquals(new BigDecimal("349.00"), PlanConfig.getLimits("enterprise").baseMonthlyFee());
    assertEquals(1, PlanConfig.getLimits("starter").maxOperators());
    assertEquals(3, PlanConfig.getLimits("pro").maxOperators());
    assertEquals(10, PlanConfig.getLimits("enterprise").maxOperators());
  }

  @Test
  @DisplayName("getLimits é case-insensitive e rejeita plano desconhecido")
  void getLimits_caseInsensitiveAndUnknown() {
    assertEquals(3, PlanConfig.getLimits("PRO").maxOperators());
    assertThrows(IllegalArgumentException.class, () -> PlanConfig.getLimits("ouro"));
    assertThrows(IllegalArgumentException.class, () -> PlanConfig.getLimits(null));
  }

  @Test
  @DisplayName("Plano cortesia (free) é ilimitado e grátis")
  void freePlan_shouldBeUnlimitedAndFree() {
    PlanConfig.PlanLimits limits = PlanConfig.getLimits(PlanConfig.PLAN_FREE);
    assertEquals(Integer.MAX_VALUE, limits.maxProducts());
    assertEquals(Integer.MAX_VALUE, limits.maxSalesPerMonth());
    assertEquals(Integer.MAX_VALUE, limits.maxOperators());
    assertEquals(0, BigDecimal.ZERO.compareTo(limits.baseMonthlyFee()));
  }

  @Test
  @DisplayName("isFree identifica apenas o plano cortesia")
  void isFree_shouldDetectOnlyFree() {
    assertTrue(PlanConfig.isFree("free"));
    assertTrue(PlanConfig.isFree("FREE"));
    assertFalse(PlanConfig.isFree("pro"));
    assertFalse(PlanConfig.isFree(null));
  }
}
