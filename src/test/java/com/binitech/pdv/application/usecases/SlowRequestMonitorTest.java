package com.binitech.pdv.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.binitech.pdv.domain.SlowRequestEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlowRequestMonitorTest {

  private final List<SlowRequestEvent> published = new ArrayList<>();
  private final MutableClock clock = new MutableClock();
  private final SlowRequestMonitor monitor =
      new SlowRequestMonitor(published::add, clock, 1000, Duration.ofSeconds(10));

  @Test
  @DisplayName("Abaixo de 1 s não publica nada")
  void ignoresFastRequests() {
    assertFalse(monitor.record("GET", "/api/products", 200, 999));
    assertTrue(published.isEmpty());
  }

  @Test
  @DisplayName("Publica a primeira lenta e soma as repetidas da mesma rota até a janela passar")
  void throttlesPerRoute() {
    assertTrue(monitor.record("POST", "/api/sales", 201, 1500));
    assertFalse(monitor.record("POST", "/api/sales", 201, 2000));
    assertFalse(monitor.record("POST", "/api/sales", 201, 1800));
    assertTrue(monitor.record("GET", "/api/products", 200, 1200));

    clock.advance(Duration.ofSeconds(11));
    assertTrue(monitor.record("POST", "/api/sales", 503, 4000));

    assertEquals(3, published.size());
    SlowRequestEvent first = published.get(0);
    assertEquals("pdv", first.service());
    assertEquals("/api/sales", first.path());
    assertEquals(1500, first.durationMs());
    assertEquals(0, first.suppressed());
    SlowRequestEvent later = published.get(2);
    assertEquals(2, later.suppressed());
    assertEquals(503, later.status());
  }

  private static final class MutableClock extends Clock {
    private Instant now = Instant.parse("2026-11-01T12:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
