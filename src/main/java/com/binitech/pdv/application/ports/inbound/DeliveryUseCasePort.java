package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.DeliveryAddress;
import com.binitech.pdv.domain.DeliveryOrder;
import com.binitech.pdv.domain.DeliveryStore;
import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.utils.enums.DeliveryFulfillment;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.Role;
import java.util.List;

public interface DeliveryUseCasePort {

  StoreView getSettings(String tenantId);

  StoreView updateSettings(String tenantId, DeliveryStore changes, Role role);

  List<DeliveryOrder> listOrders(String tenantId, DeliveryOrderStatus status);

  DeliveryOrder updateStatus(String orderId, String tenantId, DeliveryOrderStatus target);

  DeliveryOrder complete(String orderId, PaymentMethod method, String userId, String tenantId);

  PublicStore getPublicStore(String slug);

  PlacedOrder placeOrder(String slug, OrderRequest request);

  PlacedOrder trackOrder(String slug, String orderId);

  record StoreView(Tenant tenant, DeliveryStore store) {}

  record PublicStore(
      Tenant tenant, DeliveryStore store, List<Product> products, boolean pixIntegrated) {}

  record PlacedOrder(DeliveryOrder order, DeliveryStore store, String whatsappUrl, PixCharge pix) {}

  record OrderLine(String productId, int quantity, String notes) {}

  record OrderRequest(
      String customerName,
      String customerPhone,
      DeliveryFulfillment fulfillment,
      DeliveryAddress address,
      DeliveryPaymentMethod paymentMethod,
      Double changeFor,
      String notes,
      List<OrderLine> items) {}
}
