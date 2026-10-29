package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.inbound.DataRetentionUseCasePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyDataRetentionJob {

  private static final Logger log = LoggerFactory.getLogger(DailyDataRetentionJob.class);

  private final DataRetentionUseCasePort dataRetentionUseCase;
  private final boolean enabled;
  private final int retentionDays;

  public DailyDataRetentionJob(
      DataRetentionUseCasePort dataRetentionUseCase,
      @Value("${app.data-retention.enabled:true}") boolean enabled,
      @Value("${app.data-retention.days:90}") int retentionDays) {
    this.dataRetentionUseCase = dataRetentionUseCase;
    this.enabled = enabled;
    this.retentionDays = retentionDays;
  }

  @Scheduled(cron = "0 30 3 * * *")
  public void runDataRetention() {
    if (!enabled) {
      return;
    }
    try {
      int purged = dataRetentionUseCase.purgeClosedTenants(retentionDays);
      log.info("DailyDataRetentionJob concluído: tenantsEliminados={}", purged);
    } catch (Exception e) {
      log.error("Erro no DailyDataRetentionJob: {}", e.getMessage(), e);
    }
  }
}
