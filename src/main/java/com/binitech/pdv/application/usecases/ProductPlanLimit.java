package com.binitech.pdv.application.usecases;

import com.binitech.pdv.config.PlanConfig;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.exception.BusinessException;
import java.text.NumberFormat;
import java.util.Locale;

final class ProductPlanLimit {

  private ProductPlanLimit() {}

  static long remaining(Tenant tenant, long activeProducts) {
    int limit = limitOf(tenant);
    return limit == PlanConfig.UNLIMITED ? Long.MAX_VALUE : Math.max(0, limit - activeProducts);
  }

  static BusinessException reached(Tenant tenant) {
    return new BusinessException(
        "Você chegou ao limite de "
            + NumberFormat.getIntegerInstance(Locale.of("pt", "BR")).format(limitOf(tenant))
            + " produtos ativos do seu plano. Inative produtos que não vende mais ou mude de plano"
            + " em Assinatura. Não cobramos por produto a mais.");
  }

  private static int limitOf(Tenant tenant) {
    try {
      return PlanConfig.getLimits(tenant.getPlanId()).maxProducts();
    } catch (IllegalArgumentException e) {
      return PlanConfig.UNLIMITED;
    }
  }
}
