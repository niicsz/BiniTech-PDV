package com.binitech.pdv.adapters.inbound.web.mapper;

import com.binitech.pdv.adapters.inbound.web.generated.model.*;
import com.binitech.pdv.domain.*;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WebMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  Product toDomain(CreateProductDTO dto);

  ProductDTO toDto(Product product);

  List<ProductDTO> toProductDtoList(List<Product> products);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "totalAmount", ignore = true)
  @Mapping(target = "totalPaid", ignore = true)
  @Mapping(target = "change", ignore = true)
  @Mapping(target = "timestamp", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "tenantId", ignore = true)
  @Mapping(target = "totalCost", ignore = true)
  @Mapping(target = "paid", ignore = true)
  @Mapping(target = "returns", ignore = true)
  @Mapping(target = "returnedAmount", ignore = true)
  @Mapping(target = "returnedCost", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "returnsVersion", ignore = true)
  Sale toDomain(CreateSaleDTO dto);

  @Mapping(target = "productDescription", ignore = true)
  @Mapping(target = "unitPrice", ignore = true)
  @Mapping(target = "subtotal", ignore = true)
  @Mapping(target = "costPrice", ignore = true)
  SaleItem toDomain(CreateSaleItemDTO dto);

  SaleDTO toDto(Sale sale);

  SaleItemDTO toDto(SaleItem item);

  List<SaleDTO> toSaleDtoList(List<Sale> sales);

  SaleReturnDTO toDto(SaleReturn saleReturn);

  default SaleReturnRequest toDomain(CreateSaleReturnDTO dto) {
    Map<String, Integer> quantities = new LinkedHashMap<>();
    for (CreateSaleItemDTO item : dto.getItems()) {
      quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
    }
    return new SaleReturnRequest(
        quantities,
        RefundMethod.valueOf(dto.getRefundMethod().name()),
        dto.getReason(),
        !Boolean.FALSE.equals(dto.getRestock()));
  }

  default Payment toDomain(PaymentDTO dto) {
    if (dto == null) return null;
    Payment payment = new Payment();
    payment.setAmount(dto.getAmount());
    if (dto.getMethod() != null) {
      payment.setMethod(PaymentMethod.valueOf(dto.getMethod().name()));
    }
    return payment;
  }

  default PaymentDTO toDto(Payment payment) {
    if (payment == null) return null;
    PaymentDTO dto = new PaymentDTO();
    dto.setAmount(payment.getAmount());
    if (payment.getMethod() != null) {
      dto.setMethod(PaymentMethodEnum.fromValue(payment.getMethod().name()));
    }
    return dto;
  }

  default OffsetDateTime map(LocalDateTime localDateTime) {
    if (localDateTime == null) return null;
    return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
  }

  default LocalDateTime map(OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) return null;
    return offsetDateTime.toLocalDateTime();
  }
}
