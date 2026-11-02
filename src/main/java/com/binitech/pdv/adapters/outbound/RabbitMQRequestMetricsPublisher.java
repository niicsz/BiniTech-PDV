package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.RequestMetricsPublisherPort;
import com.binitech.pdv.config.RabbitMQConfig;
import com.binitech.pdv.domain.SlowRequestEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitMQRequestMetricsPublisher implements RequestMetricsPublisherPort, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQRequestMetricsPublisher.class);

  private final RabbitTemplate rabbitTemplate;
  private final ExecutorService executor;

  public RabbitMQRequestMetricsPublisher(RabbitTemplate rabbitTemplate, ExecutorService executor) {
    this.rabbitTemplate = rabbitTemplate;
    this.executor = executor;
  }

  @Override
  public void publishSlow(SlowRequestEvent event) {
    try {
      executor.execute(() -> send(event));
    } catch (RejectedExecutionException e) {
      log.warn("Evento de lentidão descartado: {} {}", event.method(), event.path());
    }
  }

  @Override
  public void close() {
    executor.shutdown();
  }

  private void send(SlowRequestEvent event) {
    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.OBSERVABILITY_EXCHANGE, RabbitMQConfig.SLOW_REQUEST_KEY, event);
    } catch (AmqpException e) {
      log.warn(
          "Evento de lentidão não publicado: {} {} erro={}",
          event.method(),
          event.path(),
          e.getMessage());
    }
  }
}
