package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryAddressDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryFulfillmentDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryOrderDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryOrderItemDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryOrderStatusDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliveryPaymentMethodDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.DeliverySettingsDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PlaceDeliveryOrderDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PlacedDeliveryOrderDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PublicDeliveryPixDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PublicDeliveryProductDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PublicDeliveryStoreDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.UpdateDeliverySettingsDTO;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.OrderLine;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.OrderRequest;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.PlacedOrder;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.PublicStore;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort.StoreView;
import com.binitech.pdv.domain.DeliveryAddress;
import com.binitech.pdv.domain.DeliveryOrder;
import com.binitech.pdv.domain.DeliveryOrderItem;
import com.binitech.pdv.domain.DeliveryStore;
import com.binitech.pdv.domain.PixCharge;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.utils.enums.DeliveryFulfillment;
import com.binitech.pdv.utils.enums.DeliveryOrderStatus;
import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeliveryWebMapper {

  private static final int MAX_QUANTITY = 99;

  public DeliverySettingsDTO toDto(StoreView view) {
    DeliveryStore store = view.store();
    return new DeliverySettingsDTO()
        .enabled(store.isEnabled())
        .open(store.isOpen())
        .slug(view.tenant().getSlug())
        .storeName(view.tenant().getName())
        .whatsapp(store.getWhatsapp())
        .message(store.getMessage())
        .deliveryFee(store.getDeliveryFee())
        .minimumOrder(store.getMinimumOrder())
        .estimatedMinutes(store.getEstimatedMinutes())
        .deliveryAvailable(store.isDeliveryAvailable())
        .pickupAvailable(store.isPickupAvailable())
        .pickupAddress(store.getPickupAddress())
        .acceptedPayments(payments(store.getAcceptedPayments()))
        .pixKey(store.getPixKey());
  }

  public DeliveryStore toDomain(UpdateDeliverySettingsDTO dto) {
    DeliveryStore store = new DeliveryStore();
    store.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
    store.setOpen(Boolean.TRUE.equals(dto.getOpen()));
    store.setWhatsapp(dto.getWhatsapp());
    store.setMessage(dto.getMessage());
    store.setDeliveryFee(dto.getDeliveryFee() == null ? 0 : dto.getDeliveryFee());
    store.setMinimumOrder(dto.getMinimumOrder() == null ? 0 : dto.getMinimumOrder());
    store.setEstimatedMinutes(dto.getEstimatedMinutes() == null ? 40 : dto.getEstimatedMinutes());
    store.setDeliveryAvailable(Boolean.TRUE.equals(dto.getDeliveryAvailable()));
    store.setPickupAvailable(Boolean.TRUE.equals(dto.getPickupAvailable()));
    store.setPickupAddress(dto.getPickupAddress());
    EnumSet<DeliveryPaymentMethod> methods = EnumSet.noneOf(DeliveryPaymentMethod.class);
    if (dto.getAcceptedPayments() != null) {
      dto.getAcceptedPayments()
          .forEach(m -> methods.add(DeliveryPaymentMethod.valueOf(m.getValue())));
    }
    store.setAcceptedPayments(methods);
    store.setPixKey(dto.getPixKey());
    return store;
  }

  public DeliveryOrderDTO toDto(DeliveryOrder order) {
    return new DeliveryOrderDTO()
        .id(order.getId())
        .number(order.getNumber())
        .status(DeliveryOrderStatusDTO.fromValue(order.getStatus().name()))
        .fulfillment(DeliveryFulfillmentDTO.fromValue(order.getFulfillment().name()))
        .customerName(order.getCustomerName())
        .customerPhone(order.getCustomerPhone())
        .address(toDto(order.getAddress()))
        .items(items(order.getItems()))
        .subtotal(order.getSubtotal())
        .deliveryFee(order.getDeliveryFee())
        .total(order.getTotal())
        .paymentMethod(DeliveryPaymentMethodDTO.fromValue(order.getPaymentMethod().name()))
        .changeFor(order.getChangeFor())
        .notes(order.getNotes())
        .saleId(order.getSaleId())
        .paidAt(time(order.getPaidAt()))
        .createdAt(time(order.getCreatedAt()))
        .updatedAt(time(order.getUpdatedAt()));
  }

  public List<DeliveryOrderDTO> toOrderDtos(List<DeliveryOrder> orders) {
    return orders.stream().map(this::toDto).toList();
  }

  public PublicDeliveryStoreDTO toDto(PublicStore view) {
    DeliveryStore store = view.store();
    return new PublicDeliveryStoreDTO()
        .slug(view.tenant().getSlug())
        .name(view.tenant().getName())
        .open(store.isOpen())
        .message(store.getMessage())
        .deliveryFee(store.getDeliveryFee())
        .minimumOrder(store.getMinimumOrder())
        .estimatedMinutes(store.getEstimatedMinutes())
        .deliveryAvailable(store.isDeliveryAvailable())
        .pickupAvailable(store.isPickupAvailable())
        .pickupAddress(store.getPickupAddress())
        .acceptedPayments(payments(store.getAcceptedPayments()))
        .pixIntegrated(view.pixIntegrated())
        .products(view.products().stream().map(DeliveryWebMapper::toPublicProduct).toList());
  }

  public PlacedDeliveryOrderDTO toDto(PlacedOrder placed) {
    DeliveryOrder order = placed.order();
    return new PlacedDeliveryOrderDTO()
        .id(order.getId())
        .number(order.getNumber())
        .status(DeliveryOrderStatusDTO.fromValue(order.getStatus().name()))
        .fulfillment(DeliveryFulfillmentDTO.fromValue(order.getFulfillment().name()))
        .items(items(order.getItems()))
        .subtotal(order.getSubtotal())
        .deliveryFee(order.getDeliveryFee())
        .total(order.getTotal())
        .paymentMethod(DeliveryPaymentMethodDTO.fromValue(order.getPaymentMethod().name()))
        .pixKey(
            order.getPaymentMethod() == DeliveryPaymentMethod.PIX && !order.isPaid()
                ? placed.store().getPixKey()
                : null)
        .paid(order.isPaid())
        .pix(toDto(placed.pix()))
        .whatsappUrl(placed.whatsappUrl())
        .createdAt(time(order.getCreatedAt()));
  }

  private static PublicDeliveryPixDTO toDto(PixCharge charge) {
    if (charge == null) {
      return null;
    }
    return new PublicDeliveryPixDTO()
        .qrCode(charge.getQrCode())
        .qrCodeBase64(charge.getQrCodeBase64())
        .expiresAt(
            charge.getExpiresAt() == null
                ? null
                : charge.getExpiresAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
  }

  public OrderRequest toRequest(PlaceDeliveryOrderDTO dto) {
    DeliveryAddressDTO address = dto.getAddress();
    return new OrderRequest(
        dto.getCustomerName(),
        dto.getCustomerPhone(),
        dto.getFulfillment() == null
            ? null
            : DeliveryFulfillment.valueOf(dto.getFulfillment().getValue()),
        address == null
            ? null
            : new DeliveryAddress(
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getDistrict(),
                address.getReference()),
        dto.getPaymentMethod() == null
            ? null
            : DeliveryPaymentMethod.valueOf(dto.getPaymentMethod().getValue()),
        dto.getChangeFor(),
        dto.getNotes(),
        lines(dto));
  }

  private static List<OrderLine> lines(PlaceDeliveryOrderDTO dto) {
    if (dto.getItems() == null) {
      return List.of();
    }
    return dto.getItems().stream()
        .map(i -> new OrderLine(i.getProductId(), quantityOf(i.getQuantity()), i.getNotes()))
        .toList();
  }

  private static int quantityOf(Integer quantity) {
    return quantity == null ? 0 : quantity;
  }

  public DeliveryOrderStatus toStatus(DeliveryOrderStatusDTO dto) {
    return dto == null ? null : DeliveryOrderStatus.valueOf(dto.getValue());
  }

  private static PublicDeliveryProductDTO toPublicProduct(Product product) {
    int stock = Math.max(0, product.getStockQuantity());
    return new PublicDeliveryProductDTO()
        .id(product.getId())
        .description(product.getDescription())
        .category(
            product.getCategory() == null || product.getCategory().isBlank()
                ? "Outros"
                : product.getCategory().strip())
        .price(product.getPrice())
        .available(stock > 0)
        .maxQuantity(Math.min(stock, MAX_QUANTITY));
  }

  private static DeliveryAddressDTO toDto(DeliveryAddress address) {
    if (address == null) {
      return null;
    }
    return new DeliveryAddressDTO()
        .street(address.street())
        .number(address.number())
        .complement(address.complement())
        .district(address.district())
        .reference(address.reference());
  }

  private static List<DeliveryOrderItemDTO> items(List<DeliveryOrderItem> items) {
    return items.stream()
        .map(
            item ->
                new DeliveryOrderItemDTO()
                    .productId(item.productId())
                    .description(item.description())
                    .unitPrice(item.unitPrice())
                    .quantity(item.quantity())
                    .notes(item.notes())
                    .subtotal(item.subtotal()))
        .toList();
  }

  private static List<DeliveryPaymentMethodDTO> payments(
      Collection<DeliveryPaymentMethod> methods) {
    if (methods == null) {
      return List.of();
    }
    return methods.stream().map(m -> DeliveryPaymentMethodDTO.fromValue(m.name())).toList();
  }

  private static OffsetDateTime time(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }
}
