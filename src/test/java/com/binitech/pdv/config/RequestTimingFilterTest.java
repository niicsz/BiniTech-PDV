package com.binitech.pdv.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.binitech.pdv.application.usecases.SlowRequestMonitor;
import com.binitech.pdv.domain.SlowRequestEvent;
import jakarta.servlet.ServletException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class RequestTimingFilterTest {

  private final List<SlowRequestEvent> published = new ArrayList<>();
  private final AtomicLong nanos = new AtomicLong();
  private final RequestTimingFilter filter =
      new RequestTimingFilter(
          new SlowRequestMonitor(published::add, Clock.systemUTC(), 1000, Duration.ofSeconds(10)),
          nanos::get);

  private MockHttpServletRequest request(String method, String uri, String pattern) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    if (pattern != null) {
      request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
    }
    return request;
  }

  @Test
  @DisplayName("Requisição de 1,5 s publica a rota sem IDs, com método e status")
  void publishesSlowRequestWithPattern() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        request("POST", "/api/sales/abc123/returns", "/api/sales/{id}/returns"),
        response,
        (req, res) -> {
          ((MockHttpServletResponse) res).setStatus(201);
          nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(1500));
        });

    assertEquals(1, published.size());
    SlowRequestEvent event = published.get(0);
    assertEquals("POST", event.method());
    assertEquals("/api/sales/{id}/returns", event.path());
    assertEquals(201, event.status());
    assertEquals(1500, event.durationMs());
  }

  @Test
  @DisplayName("Rápida, actuator e falha no meio da requisição seguem as regras")
  void ignoresFastAndActuatorAndReportsFailures() throws Exception {
    filter.doFilter(
        request("GET", "/api/products", "/api/products"),
        new MockHttpServletResponse(),
        (req, res) -> nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(80)));
    filter.doFilter(
        request("GET", "/actuator/health", null),
        new MockHttpServletResponse(),
        (req, res) -> nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(5000)));
    assertTrue(published.isEmpty());

    assertThrows(
        ServletException.class,
        () ->
            filter.doFilter(
                request("GET", "/api/x", null),
                new MockHttpServletResponse(),
                (req, res) -> {
                  nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(2000));
                  throw new ServletException("boom");
                }));
    assertEquals("unmatched", published.get(0).path());
  }
}
