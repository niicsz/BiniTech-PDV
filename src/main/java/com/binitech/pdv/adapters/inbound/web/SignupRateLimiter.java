package com.binitech.pdv.adapters.inbound.web;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SignupRateLimiter {

  private final DeliveryOrderRateLimiter window;

  public SignupRateLimiter(
      @Value("${signup.per-ip:3}") int maxSignups, @Value("${signup.window:24h}") Duration window) {
    this.window = new DeliveryOrderRateLimiter(maxSignups, window, Clock.systemUTC());
  }

  public boolean tryAcquire(String ip) {
    return window.tryAcquire("signup|" + ip);
  }
}
