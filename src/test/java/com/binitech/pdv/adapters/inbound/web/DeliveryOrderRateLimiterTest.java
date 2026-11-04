package com.binitech.pdv.adapters.inbound.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryOrderRateLimiterTest {

  private static final class MovingClock extends Clock {
    private Instant now = Instant.parse("2026-11-03T12:00:00Z");

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

  @Test
  @DisplayName("Libera até o limite por cliente e de novo depois que a janela passa")
  void limitsPerKey() {
    MovingClock clock = new MovingClock();
    DeliveryOrderRateLimiter limiter =
        new DeliveryOrderRateLimiter(2, Duration.ofMinutes(10), clock);

    assertTrue(limiter.tryAcquire("1.1.1.1|loja"));
    assertTrue(limiter.tryAcquire("1.1.1.1|loja"));
    assertFalse(limiter.tryAcquire("1.1.1.1|loja"));
    assertTrue(limiter.tryAcquire("2.2.2.2|loja"));

    clock.now = clock.now.plus(Duration.ofMinutes(10));
    assertTrue(limiter.tryAcquire("1.1.1.1|loja"));
  }
}
