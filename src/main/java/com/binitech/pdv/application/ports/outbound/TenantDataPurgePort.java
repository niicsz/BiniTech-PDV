package com.binitech.pdv.application.ports.outbound;

public interface TenantDataPurgePort {
  void purgeTenantData(String tenantId);
}
