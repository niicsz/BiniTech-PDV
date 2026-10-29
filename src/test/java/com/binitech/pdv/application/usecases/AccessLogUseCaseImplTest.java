package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.binitech.pdv.application.ports.outbound.AccessLogRepositoryPort;
import com.binitech.pdv.domain.AccessLog;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessLogUseCaseImplTest {

  private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

  @Mock private AccessLogRepositoryPort repository;

  @Test
  void savesAccessWithTimestamp() {
    var useCase = new AccessLogUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    useCase.record("LOGIN", "joao", "tenant-1", "200.1.1.1");

    verify(repository).save(new AccessLog("LOGIN", "joao", "tenant-1", "200.1.1.1", NOW));
  }

  @Test
  void storageFailureDoesNotBreakLogin() {
    var useCase = new AccessLogUseCaseImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    doThrow(new IllegalStateException("mongo fora")).when(repository).save(any());

    assertDoesNotThrow(() -> useCase.record("LOGIN", "joao", "tenant-1", "200.1.1.1"));
  }
}
