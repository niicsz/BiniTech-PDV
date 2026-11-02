package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.RabbitMQRequestMetricsPublisher;
import com.binitech.pdv.application.ports.outbound.RequestMetricsPublisherPort;
import com.binitech.pdv.application.usecases.SlowRequestMonitor;
import java.time.Clock;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RequestMonitoringConfig {

  private static final int PUBLISH_QUEUE = 1_000;

  @Bean
  @ConditionalOnProperty(name = "spring.rabbitmq.host")
  RequestMetricsPublisherPort requestMetricsPublisher(RabbitTemplate rabbitTemplate) {
    return new RabbitMQRequestMetricsPublisher(
        rabbitTemplate,
        new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(PUBLISH_QUEUE),
            Thread.ofVirtual().name("request-metrics").factory(),
            new ThreadPoolExecutor.AbortPolicy()));
  }

  @Bean
  SlowRequestMonitor slowRequestMonitor(
      ObjectProvider<RequestMetricsPublisherPort> publisher,
      @Value("${monitoring.slow-request-ms:1000}") long thresholdMs,
      @Value("${monitoring.slow-request-window:10s}") String window) {
    return new SlowRequestMonitor(
        publisher.getIfAvailable(() -> event -> {}),
        Clock.systemUTC(),
        thresholdMs,
        DurationStyle.detectAndParse(window));
  }

  @Bean
  FilterRegistrationBean<RequestTimingFilter> requestTimingFilter(SlowRequestMonitor monitor) {
    FilterRegistrationBean<RequestTimingFilter> registration =
        new FilterRegistrationBean<>(new RequestTimingFilter(monitor));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
