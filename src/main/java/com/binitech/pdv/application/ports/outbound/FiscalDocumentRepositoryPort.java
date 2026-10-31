package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.FiscalDocument;
import java.util.Optional;

public interface FiscalDocumentRepositoryPort {

  FiscalDocument save(FiscalDocument document);

  Optional<FiscalDocument> findBySaleIdAndTenantId(String saleId, String tenantId);
}
