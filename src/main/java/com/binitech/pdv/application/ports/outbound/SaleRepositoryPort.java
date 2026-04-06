package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.Sale;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepositoryPort {

  Sale save(Sale sale);

  Optional<Sale> findById(String id);

  List<Sale> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

  List<Sale> findAll();

  List<Sale> findAllByUserId(String userId);

  List<Sale> findByTimestampBetweenAndUserId(LocalDateTime start, LocalDateTime end, String userId);

  List<Sale> findDebtors();

  List<Sale> findDebtorsByUserId(String userId);
}
