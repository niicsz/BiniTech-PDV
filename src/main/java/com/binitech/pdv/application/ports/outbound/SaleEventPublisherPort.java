package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.SaleCompletedEvent;
import com.binitech.pdv.domain.SaleReturnedEvent;

public interface SaleEventPublisherPort {
  boolean publishCompleted(SaleCompletedEvent event);

  boolean publishReturned(SaleReturnedEvent event);
}
