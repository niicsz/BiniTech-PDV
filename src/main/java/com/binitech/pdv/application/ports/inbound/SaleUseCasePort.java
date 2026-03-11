package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.utils.Enum.Role;
import java.time.LocalDate;
import java.util.List;

public interface SaleUseCasePort {

  Sale createSale(Sale sale, String userId);

  Sale findById(String id, String userId, Role role);

  List<Sale> listSalesByDay(LocalDate date, String userId, Role role);

  List<Sale> listSalesByPeriod(LocalDate startDate, LocalDate endDate, String userId, Role role);

  List<Sale> listAll(String userId, Role role);
}
