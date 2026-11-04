package com.binitech.pdv.adapters.inbound.web;

import com.binitech.pdv.adapters.inbound.web.generated.api.PublicDeliveryApi;
import com.binitech.pdv.adapters.inbound.web.generated.model.PlaceDeliveryOrderDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PlacedDeliveryOrderDTO;
import com.binitech.pdv.adapters.inbound.web.generated.model.PublicDeliveryStoreDTO;
import com.binitech.pdv.application.ports.inbound.DeliveryUseCasePort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicDeliveryController implements PublicDeliveryApi {

  private final DeliveryUseCasePort delivery;
  private final DeliveryWebMapper mapper;
  private final DeliveryOrderRateLimiter rateLimiter;
  private final HttpServletRequest request;

  public PublicDeliveryController(
      DeliveryUseCasePort delivery,
      DeliveryWebMapper mapper,
      DeliveryOrderRateLimiter rateLimiter,
      HttpServletRequest request) {
    this.delivery = delivery;
    this.mapper = mapper;
    this.rateLimiter = rateLimiter;
    this.request = request;
  }

  @Override
  public ResponseEntity<PublicDeliveryStoreDTO> getPublicDeliveryStore(String slug) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(mapper.toDto(delivery.getPublicStore(slug)));
  }

  @Override
  public ResponseEntity<PlacedDeliveryOrderDTO> placeDeliveryOrder(
      String slug, PlaceDeliveryOrderDTO dto) {
    if (!rateLimiter.tryAcquire(ClientIpResolver.resolve(request) + "|" + slug)) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(mapper.toDto(delivery.placeOrder(slug, mapper.toRequest(dto))));
  }

  @Override
  public ResponseEntity<PlacedDeliveryOrderDTO> trackDeliveryOrder(String slug, String orderId) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(mapper.toDto(delivery.trackOrder(slug, orderId)));
  }
}
