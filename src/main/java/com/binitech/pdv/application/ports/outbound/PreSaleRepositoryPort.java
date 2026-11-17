package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.PreSale;
import com.binitech.pdv.utils.enums.PreSaleStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PreSaleRepositoryPort {

  PreSale save(PreSale preSale);

  long nextNumber(String tenantId);

  Optional<PreSale> findByIdAndTenantId(String id, String tenantId);

  Optional<PreSale> findByNumber(String tenantId, long number);

  List<PreSale> findRecent(String tenantId, PreSaleStatus status, int limit);

  boolean close(String id, String tenantId, PreSaleStatus target, Instant at);

  boolean reopen(String id, String tenantId);

  void attachSale(String id, String tenantId, String saleId);
}
