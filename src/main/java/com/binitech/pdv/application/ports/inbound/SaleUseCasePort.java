package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Sale;
import java.time.LocalDate;
import java.util.List;

public interface SaleUseCasePort {

  Sale createSale(Sale sale);

  Sale findById(String id);

  List<Sale> listSalesByDay(LocalDate date);

  List<Sale> listSalesByPeriod(LocalDate startDate, LocalDate endDate);

  List<Sale> listAll();
}
