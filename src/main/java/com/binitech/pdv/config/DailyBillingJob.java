package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.inbound.BillingUseCasePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyBillingJob {

  private static final Logger log = LoggerFactory.getLogger(DailyBillingJob.class);
  private static final int GRACE_DAYS = 3;

  private final BillingUseCasePort billingUseCase;

  public DailyBillingJob(BillingUseCasePort billingUseCase) {
    this.billingUseCase = billingUseCase;
  }

  @Scheduled(cron = "0 0 0 * * *")
  public void runDailyBilling() {
    log.info("DailyBillingJob iniciado");
    try {
      billingUseCase.blockOverdueTenants(GRACE_DAYS);
      log.info("DailyBillingJob concluído com sucesso");
    } catch (Exception e) {
      log.error("Erro no DailyBillingJob: {}", e.getMessage(), e);
    }
  }
}
