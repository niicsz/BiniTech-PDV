package com.binitech.pdv.config;

import com.binitech.pdv.application.usecases.SlowRequestMonitor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

public class RequestTimingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
  private static final String UNMATCHED = "unmatched";

  private final SlowRequestMonitor monitor;
  private final LongSupplier nanoTime;

  public RequestTimingFilter(SlowRequestMonitor monitor) {
    this(monitor, System::nanoTime);
  }

  RequestTimingFilter(SlowRequestMonitor monitor, LongSupplier nanoTime) {
    this.monitor = monitor;
    this.nanoTime = nanoTime;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long started = nanoTime.getAsLong();
    try {
      chain.doFilter(request, response);
    } finally {
      long durationMs = TimeUnit.NANOSECONDS.toMillis(nanoTime.getAsLong() - started);
      if (durationMs >= monitor.thresholdMs()) {
        report(request, response, durationMs);
      }
    }
  }

  private void report(HttpServletRequest request, HttpServletResponse response, long durationMs) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    String path = pattern == null ? UNMATCHED : pattern.toString();
    try {
      monitor.register(request.getMethod(), path, response.getStatus(), durationMs);
    } catch (RuntimeException e) {
      log.warn(
          "Falha ao registrar requisição lenta {} {}: {}",
          request.getMethod(),
          path,
          e.getMessage());
    }
  }
}
