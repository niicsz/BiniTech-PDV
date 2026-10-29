package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.inbound.AccessLogUseCasePort;
import com.binitech.pdv.application.ports.outbound.AccessLogRepositoryPort;
import com.binitech.pdv.domain.AccessLog;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLogUseCaseImpl implements AccessLogUseCasePort {

  private static final Logger log = LoggerFactory.getLogger(AccessLogUseCaseImpl.class);

  private final AccessLogRepositoryPort accessLogRepository;
  private final Clock clock;

  public AccessLogUseCaseImpl(AccessLogRepositoryPort accessLogRepository, Clock clock) {
    this.accessLogRepository = accessLogRepository;
    this.clock = clock;
  }

  @Override
  public void record(String event, String username, String tenantId, String ip) {
    try {
      accessLogRepository.save(new AccessLog(event, username, tenantId, ip, clock.instant()));
    } catch (RuntimeException e) {
      log.error("Falha ao gravar registro de acesso: event={}", event, e);
    }
  }
}
