package com.binitech.pdv.application.ports.inbound;

public interface DataRetentionUseCasePort {
  int purgeClosedTenants(int retentionDays);
}
