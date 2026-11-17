package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.PreSale;
import com.binitech.pdv.utils.enums.PreSaleStatus;
import com.binitech.pdv.utils.enums.Role;
import java.util.List;

public interface PreSaleUseCasePort {

  record Attendant(String userId, String username, String tenantId, Role role) {}

  record ItemRequest(String productId, Integer quantity) {}

  PreSale create(Attendant attendant, List<ItemRequest> items, String notes);

  List<PreSale> list(Attendant attendant, PreSaleStatus status);

  PreSale findByNumber(Attendant attendant, long number);

  PreSale cancel(Attendant attendant, String id);
}
