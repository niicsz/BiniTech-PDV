package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.inbound.TrialNurtureUseCasePort;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyTrialEmailJob {

  private static final Logger log = LoggerFactory.getLogger(DailyTrialEmailJob.class);
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

  private final TrialNurtureUseCasePort trialNurture;

  public DailyTrialEmailJob(TrialNurtureUseCasePort trialNurture) {
    this.trialNurture = trialNurture;
  }

  @Scheduled(cron = "0 0 10 * * *", zone = "America/Sao_Paulo")
  public void run() {
    try {
      int sent = trialNurture.sendDueEmails(LocalDate.now(BUSINESS_ZONE));
      log.info("DailyTrialEmailJob concluído: enviados={}", sent);
    } catch (Exception e) {
      log.error("Erro no DailyTrialEmailJob: {}", e.getMessage(), e);
    }
  }
}
