package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.SaleEventPublisherPort;
import com.binitech.pdv.config.RabbitMQConfig;
import com.binitech.pdv.domain.SaleCompletedEvent;
import com.binitech.pdv.utils.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMQSaleEventPublisher implements SaleEventPublisherPort {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQSaleEventPublisher.class);
  private static final long CONFIRM_TIMEOUT_MS = 5_000;

  private final RabbitTemplate rabbitTemplate;

  public RabbitMQSaleEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public boolean publishCompleted(SaleCompletedEvent event) {
    try {
      return Boolean.TRUE.equals(
          rabbitTemplate.invoke(
              operations -> {
                operations.convertAndSend(
                    RabbitMQConfig.SALES_EXCHANGE, RabbitMQConfig.SALE_COMPLETED_KEY, event);
                operations.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
                return true;
              }));
    } catch (AmqpException | IllegalStateException e) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Venda não publicada para o serviço fiscal (vai tentar de novo): saleId={} erro={}",
            LogSanitizer.maskId(event.saleId()),
            e.getMessage());
      }
      return false;
    }
  }
}
