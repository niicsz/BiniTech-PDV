package com.binitech.pdv.application.usecases;

import com.binitech.pdv.application.ports.outbound.RequestMetricsPublisherPort;
import com.binitech.pdv.domain.SlowRequestEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlowRequestMonitor {

  public static final String SERVICE = "pdv";
  private static final int MAX_TRACKED_ROUTES = 1_000;

  private final RequestMetricsPublisherPort publisher;
  private final Clock clock;
  private final long thresholdMs;
  private final Duration window;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  public SlowRequestMonitor(
      RequestMetricsPublisherPort publisher, Clock clock, long thresholdMs, Duration window) {
    this.publisher = publisher;
    this.clock = clock;
    this.thresholdMs = thresholdMs;
    this.window = window;
  }

  public boolean register(String method, String path, int status, long durationMs) {
    if (durationMs < thresholdMs) {
      return false;
    }
    if (windows.size() >= MAX_TRACKED_ROUTES) {
      windows.clear();
    }
    String key = method + " " + path;
    Instant now = clock.instant();
    Window current = windows.computeIfAbsent(key, k -> new Window());
    int suppressed;
    synchronized (current) {
      if (current.lastPublished != null && current.lastPublished.plus(window).isAfter(now)) {
        current.suppressed++;
        return false;
      }
      suppressed = current.suppressed;
      current.suppressed = 0;
      current.lastPublished = now;
    }
    publisher.publishSlow(
        new SlowRequestEvent(SERVICE, method, path, status, durationMs, now, suppressed));
    return true;
  }

  public long thresholdMs() {
    return thresholdMs;
  }

  private static final class Window {
    private Instant lastPublished;
    private int suppressed;
  }
}
