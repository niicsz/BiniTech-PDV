package com.binitech.pdv.application.ports.inbound;

public interface AccessLogUseCasePort {
  void record(String event, String username, String tenantId, String ip);
}
