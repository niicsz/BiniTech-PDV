package com.binitech.pdv.config;

import com.binitech.pdv.application.usecases.SaleFiscalPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SaleFiscalOutboxJob {

  private static final Logger log = LoggerFactory.getLogger(SaleFiscalOutboxJob.class);
  private static final int BATCH = 200;

  private final SaleFiscalPublisher publisher;

  public SaleFiscalOutboxJob(SaleFiscalPublisher publisher) {
    this.publisher = publisher;
  }

  @Scheduled(fixedDelayString = "${fiscal.outbox-interval-ms:30000}")
  public void publishPending() {
    try {
      int published = publisher.publishPending(BATCH);
      if (published > 0 && log.isInfoEnabled()) {
        log.info("Vendas reenviadas ao serviço fiscal: {}", published);
      }
    } catch (RuntimeException e) {
      log.error("Falha ao reenviar vendas ao serviço fiscal: {}", e.getMessage(), e);
    }
  }
}
