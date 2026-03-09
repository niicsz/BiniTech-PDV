package com.binitech.pdv.adapters.outbound.persistence.mapper;

import com.binitech.pdv.adapters.outbound.persistence.document.PaymentDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.ProductDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.SaleDocument;
import com.binitech.pdv.adapters.outbound.persistence.document.SaleItemDocument;
import com.binitech.pdv.domain.Payment;
import com.binitech.pdv.domain.Product;
import com.binitech.pdv.domain.Sale;
import com.binitech.pdv.domain.SaleItem;
import com.binitech.pdv.utils.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PersistenceMapper {

  ProductDocument toDocument(Product product);

  Product toDomain(ProductDocument document);

  SaleDocument toDocument(Sale sale);

  Sale toDomain(SaleDocument document);

  SaleItemDocument toDocument(SaleItem item);

  SaleItem toDomain(SaleItemDocument document);

  @Mapping(target = "method", source = "method", qualifiedByName = "paymentMethodToString")
  PaymentDocument toDocument(Payment payment);

  @Mapping(target = "method", source = "method", qualifiedByName = "stringToPaymentMethod")
  Payment toDomain(PaymentDocument document);

  @Named("paymentMethodToString")
  default String paymentMethodToString(PaymentMethod method) {
    return method != null ? method.name() : null;
  }

  @Named("stringToPaymentMethod")
  default PaymentMethod stringToPaymentMethod(String method) {
    return method != null ? PaymentMethod.valueOf(method) : null;
  }
}
