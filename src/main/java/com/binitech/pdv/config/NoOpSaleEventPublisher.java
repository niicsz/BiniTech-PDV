package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.outbound.SaleEventPublisherPort;
import com.binitech.pdv.domain.SaleCompletedEvent;
import com.binitech.pdv.domain.SaleReturnedEvent;

class NoOpSaleEventPublisher implements SaleEventPublisherPort {

  @Override
  public boolean publishCompleted(SaleCompletedEvent event) {
    return true;
  }

  @Override
  public boolean publishReturned(SaleReturnedEvent event) {
    return true;
  }
}
