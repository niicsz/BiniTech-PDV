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
  private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

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
    if (hits.size() > MAX_KEYS) {
      hits.entrySet().removeIf(e -> expired(e.getValue(), now));
    }
    Deque<Long> times = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
    synchronized (times) {
      while (!times.isEmpty() && now - times.peekFirst() >= windowMillis) {
        times.pollFirst();
      }
      if (times.size() >= maxOrders) {
        return false;
      }
      times.addLast(now);
      return true;
    }
  }

  private boolean expired(Deque<Long> times, long now) {
    synchronized (times) {
      return times.isEmpty() || now - times.peekLast() >= windowMillis;
    }
  }
}
