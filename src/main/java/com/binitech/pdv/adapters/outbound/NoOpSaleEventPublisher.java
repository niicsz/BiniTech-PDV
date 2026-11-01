package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.SaleEventPublisherPort;
import com.binitech.pdv.domain.SaleCompletedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "spring.rabbitmq.host",
    havingValue = "disabled",
    matchIfMissing = true)
public class NoOpSaleEventPublisher implements SaleEventPublisherPort {

  @Override
  public boolean publishCompleted(SaleCompletedEvent event) {
    return true;
  }
}
