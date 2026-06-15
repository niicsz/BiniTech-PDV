package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.utils.Enum.Role;
import java.time.LocalDate;
import java.util.List;

public interface SaleUseCasePort {

  Sale createSale(Sale sale, String userId, String tenantId);

  Sale findById(String id, String tenantId);

  List<Sale> listSalesByDay(LocalDate date, String tenantId);

  List<Sale> listSalesByPeriod(LocalDate startDate, LocalDate endDate, String tenantId);

  List<Sale> listAll(String tenantId);

  List<Sale> listAll(String tenantId, int page, int size);

  List<Sale> listDebtors(String tenantId);

  Sale markAsPaid(String id, String userId, String tenantId, Role role);
}
