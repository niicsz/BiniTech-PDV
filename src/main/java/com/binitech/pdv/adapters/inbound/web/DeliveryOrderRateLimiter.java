package com.binitech.pdv.adapters.inbound.web;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeliveryOrderRateLimiter {

  private static final int MAX_KEYS = 50_000;

  private final int maxOrders;
  private final long windowMillis;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  @Autowired
  public DeliveryOrderRateLimiter(
      @Value("${delivery.orders-per-window:5}") int maxOrders,
      @Value("${delivery.order-window:10m}") Duration window) {
    this(maxOrders, window, Clock.systemUTC());
  }

  DeliveryOrderRateLimiter(int maxOrders, Duration window, Clock clock) {
    this.maxOrders = maxOrders;
    this.windowMillis = window.toMillis();
    this.clock = clock;
  }

  public boolean tryAcquire(String key) {
    long now = clock.millis();
    if (windows.size() > MAX_KEYS) {
      windows.values().removeIf(w -> w.idleSince(now));
    }
    return windows.computeIfAbsent(key, k -> new Window()).tryAcquire(now);
  }

  private final class Window {
    private final Deque<Long> times = new ArrayDeque<>();

    synchronized boolean tryAcquire(long now) {
      while (!times.isEmpty() && now - times.peekFirst() >= windowMillis) {
        times.pollFirst();
      }
      if (times.size() >= maxOrders) {
        return false;
      }
      times.addLast(now);
      return true;
    }

    synchronized boolean idleSince(long now) {
      return times.isEmpty() || now - times.peekLast() >= windowMillis;
    }
  }
}
