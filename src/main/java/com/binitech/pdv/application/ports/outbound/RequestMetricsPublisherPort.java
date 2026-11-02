package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.SlowRequestEvent;

public interface RequestMetricsPublisherPort {
  void publishSlow(SlowRequestEvent event);
}
