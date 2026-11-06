package com.binitech.pdv.domain;

import com.binitech.pdv.domain.exception.BusinessException;
import com.binitech.pdv.utils.enums.PaymentMethod;
import com.binitech.pdv.utils.enums.RefundMethod;
import com.binitech.pdv.utils.enums.SaleStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

public class Sale {

  private String id;
  private List<SaleItem> items;
  private List<Payment> payments;
  private double totalAmount;
  private double totalCost;
  private double totalPaid;
  private double change;
  private LocalDateTime timestamp;
  private String userId;
  private String tenantId;
  private String customerName;
  private String customerPhone;
  private String customerCpf;
  private boolean fiscalPending;
  private boolean skipStockValidation;
  private boolean paid;
  private List<SaleReturn> returns;
  private double returnedAmount;
  private double returnedCost;
  private SaleStatus status;
  private int returnsVersion;
  private double itemsTotal;
  private double discount;
  private double surcharge;
  private String cashSessionId;

  public Sale() {
    this.items = new ArrayList<>();
    this.payments = new ArrayList<>();
    this.returns = new ArrayList<>();
  }

  public int soldQuantity(String productId) {
    return items.stream()
        .filter(i -> Objects.equals(i.getProductId(), productId))
        .mapToInt(SaleItem::getQuantity)
        .sum();
  }

  public int returnedQuantity(String productId) {
    return returns.stream()
        .flatMap(r -> r.getItems().stream())
        .filter(i -> Objects.equals(i.getProductId(), productId))
        .mapToInt(SaleItem::getQuantity)
        .sum();
  }

  public boolean hasOpenCrediario() {
    return !paid
        && payments != null
        && payments.stream().anyMatch(p -> p.getMethod() == PaymentMethod.CREDIARIO);
  }

  public SaleReturn registerReturn(
      SaleReturnRequest request, String returnId, String userId, LocalDateTime now) {
    if (getStatus() == SaleStatus.RETURNED) {
      throw new BusinessException("Esta venda já foi devolvida por completo.");
    }
    if (request.refundMethod() == null) {
      throw new BusinessException("Informe a forma de reembolso.");
    }
    validateRefundMethod(request.refundMethod());
    Map<String, Integer> quantities = normalizeQuantities(request.quantities());

    SaleReturn saleReturn = new SaleReturn();
    saleReturn.setId(returnId);
    saleReturn.setTimestamp(now);
    saleReturn.setUserId(userId);
    saleReturn.setRefundMethod(request.refundMethod());
    saleReturn.setReason(blankToNull(request.reason()));
    saleReturn.setRestocked(request.restock());
    for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
      saleReturn.getItems().add(returnItem(entry.getKey(), entry.getValue()));
    }
    saleReturn.setRefundAmount(
        Money.sum(saleReturn.getItems().stream().map(SaleItem::netSubtotal)));
    saleReturn.setRefundCost(
        Money.sum(
            saleReturn.getItems().stream()
                .map(i -> Money.multiply(i.getCostPrice(), i.getQuantity()))));

    returns.add(saleReturn);
    returnedAmount = Money.sum(returns.stream().map(SaleReturn::getRefundAmount));
    returnedCost = Money.sum(returns.stream().map(SaleReturn::getRefundCost));
    status = fullyReturned() ? SaleStatus.RETURNED : SaleStatus.PARTIALLY_RETURNED;
    return saleReturn;
  }

  private void validateRefundMethod(RefundMethod method) {
    boolean deduction = method == RefundMethod.DEBT_DEDUCTION;
    if (hasOpenCrediario() && !deduction) {
      throw new BusinessException(
          "Esta venda está no fiado e ainda não foi paga: a devolução deve abater o débito.");
    }
    if (!hasOpenCrediario() && deduction) {
      throw new BusinessException("Só dá para abater do fiado em vendas no crediário em aberto.");
    }
  }

  private Map<String, Integer> normalizeQuantities(Map<String, Integer> requested) {
    if (requested == null || requested.isEmpty()) {
      throw new BusinessException("Escolha ao menos um item para devolver.");
    }
    Map<String, Integer> quantities = new LinkedHashMap<>();
    for (Map.Entry<String, Integer> entry : requested.entrySet()) {
      int quantity = entry.getValue() == null ? 0 : entry.getValue();
      if (quantity <= 0) {
        continue;
      }
      String productId = entry.getKey();
      int sold = soldQuantity(productId);
      if (sold == 0) {
        throw new BusinessException("O produto informado não faz parte desta venda.");
      }
      int available = sold - returnedQuantity(productId);
      if (quantity > available) {
        throw new BusinessException(
            String.format(
                "Quantidade maior que a disponível para devolução de '%s'. Disponível: %d",
                descriptionOf(productId), available));
      }
      quantities.put(productId, quantity);
    }
    if (quantities.isEmpty()) {
      throw new BusinessException("Escolha ao menos um item para devolver.");
    }
    return quantities;
  }

  private SaleItem returnItem(String productId, int quantity) {
    SaleItem sold =
        items.stream()
            .filter(i -> Objects.equals(i.getProductId(), productId))
            .findFirst()
            .orElseThrow();
    SaleItem returned =
        new SaleItem(
            productId,
            sold.getProductDescription(),
            quantity,
            sold.getUnitPrice(),
            sold.getCostPrice());
    returned.setDiscount(returnShare(productId, quantity, SaleItem::getDiscount));
    returned.setSurcharge(returnShare(productId, quantity, SaleItem::getSurcharge));
    return returned;
  }

  private double returnShare(String productId, int quantity, ToDoubleFunction<SaleItem> value) {
    BigDecimal sold = productValue(items.stream(), productId, value);
    BigDecimal alreadyReturned =
        productValue(returns.stream().flatMap(r -> r.getItems().stream()), productId, value);
    int available = soldQuantity(productId) - returnedQuantity(productId);
    if (quantity >= available) {
      return sold.subtract(alreadyReturned).max(BigDecimal.ZERO).doubleValue();
    }
    return sold.multiply(BigDecimal.valueOf(quantity))
        .divide(BigDecimal.valueOf(soldQuantity(productId)), 2, RoundingMode.HALF_UP)
        .doubleValue();
  }

  private static BigDecimal productValue(
      Stream<SaleItem> source, String productId, ToDoubleFunction<SaleItem> value) {
    return source
        .filter(i -> Objects.equals(i.getProductId(), productId))
        .map(i -> Money.of(value.applyAsDouble(i)))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private String descriptionOf(String productId) {
    return items.stream()
        .filter(i -> Objects.equals(i.getProductId(), productId))
        .map(SaleItem::getProductDescription)
        .findFirst()
        .orElse(productId);
  }

  private boolean fullyReturned() {
    return items.stream()
        .map(SaleItem::getProductId)
        .distinct()
        .allMatch(productId -> returnedQuantity(productId) >= soldQuantity(productId));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public void addItem(SaleItem item) {
    item.recalculateSubtotal();
    this.items.add(item);
    recalculateTotal();
  }

  public void removeItem(String productId) {
    this.items.removeIf(i -> i.getProductId().equals(productId));
    recalculateTotal();
  }

  public void recalculateTotal() {
    this.itemsTotal = Money.sum(items.stream().map(SaleItem::getSubtotal));
    allocateAdjustments();
    this.totalAmount =
        Money.of(itemsTotal).subtract(Money.of(discount)).add(Money.of(surcharge)).doubleValue();
    this.totalCost =
        Money.sum(items.stream().map(i -> Money.multiply(i.getCostPrice(), i.getQuantity())));
  }

  private void allocateAdjustments() {
    List<BigDecimal> discounts = allocate(Money.of(discount));
    List<BigDecimal> surcharges = allocate(Money.of(surcharge));
    for (int i = 0; i < items.size(); i++) {
      items.get(i).setDiscount(discounts.get(i).doubleValue());
      items.get(i).setSurcharge(surcharges.get(i).doubleValue());
    }
  }

  private List<BigDecimal> allocate(BigDecimal amount) {
    BigDecimal base = Money.of(itemsTotal);
    List<BigDecimal> parts = new ArrayList<>();
    if (amount.signum() == 0 || base.signum() == 0) {
      items.forEach(i -> parts.add(BigDecimal.ZERO));
      return parts;
    }
    BigDecimal allocated = BigDecimal.ZERO;
    int largest = 0;
    for (int i = 0; i < items.size(); i++) {
      BigDecimal subtotal = Money.of(items.get(i).getSubtotal());
      BigDecimal part = amount.multiply(subtotal).divide(base, 2, RoundingMode.DOWN);
      parts.add(part);
      allocated = allocated.add(part);
      if (subtotal.compareTo(Money.of(items.get(largest).getSubtotal())) > 0) {
        largest = i;
      }
    }
    parts.set(largest, parts.get(largest).add(amount.subtract(allocated)));
    return parts;
  }

  public void calculatePayment() {
    this.totalPaid = Money.sum(payments.stream().map(Payment::getAmount));
    this.change = Math.max(0, Money.round(this.totalPaid - this.totalAmount));
  }

  public void recalculate() {
    recalculateTotal();
    calculatePayment();
  }

  public void validate() {
    if (items == null || items.isEmpty()) {
      throw new BusinessException("A venda deve ter ao menos um item.");
    }
    if (payments == null || payments.isEmpty()) {
      throw new BusinessException("A venda deve ter ao menos uma forma de pagamento.");
    }
    if (discount < 0 || surcharge < 0) {
      throw new BusinessException("Desconto e acréscimo não podem ser negativos.");
    }
    if (Money.compare(discount, itemsTotal) >= 0) {
      throw new BusinessException("O desconto precisa ser menor que o total dos produtos.");
    }
    if (Money.compare(this.totalPaid, this.totalAmount) < 0) {
      throw new BusinessException(
          String.format("Pagamento insuficiente. Total: %.2f, Pago: %.2f", totalAmount, totalPaid));
    }
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<SaleItem> getItems() {
    return items;
  }

  public void setItems(List<SaleItem> items) {
    this.items = items;
  }

  public List<Payment> getPayments() {
    return payments;
  }

  public void setPayments(List<Payment> payments) {
    this.payments = payments;
  }

  public double getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(double totalAmount) {
    this.totalAmount = totalAmount;
  }

  public double getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(double totalCost) {
    this.totalCost = totalCost;
  }

  public double getTotalPaid() {
    return totalPaid;
  }

  public void setTotalPaid(double totalPaid) {
    this.totalPaid = totalPaid;
  }

  public double getChange() {
    return change;
  }

  public void setChange(double change) {
    this.change = change;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getCustomerPhone() {
    return customerPhone;
  }

  public void setCustomerPhone(String customerPhone) {
    this.customerPhone = customerPhone;
  }

  public String getCustomerCpf() {
    return customerCpf;
  }

  public void setCustomerCpf(String customerCpf) {
    this.customerCpf = customerCpf;
  }

  public boolean isFiscalPending() {
    return fiscalPending;
  }

  public void setFiscalPending(boolean fiscalPending) {
    this.fiscalPending = fiscalPending;
  }

  public boolean isSkipStockValidation() {
    return skipStockValidation;
  }

  public void setSkipStockValidation(boolean skipStockValidation) {
    this.skipStockValidation = skipStockValidation;
  }

  public boolean isPaid() {
    return paid;
  }

  public void setPaid(boolean paid) {
    this.paid = paid;
  }

  public List<SaleReturn> getReturns() {
    return returns;
  }

  public void setReturns(List<SaleReturn> returns) {
    this.returns = returns == null ? new ArrayList<>() : new ArrayList<>(returns);
  }

  public double getReturnedAmount() {
    return returnedAmount;
  }

  public void setReturnedAmount(double returnedAmount) {
    this.returnedAmount = returnedAmount;
  }

  public double getReturnedCost() {
    return returnedCost;
  }

  public void setReturnedCost(double returnedCost) {
    this.returnedCost = returnedCost;
  }

  public SaleStatus getStatus() {
    return status == null ? SaleStatus.COMPLETED : status;
  }

  public void setStatus(SaleStatus status) {
    this.status = status;
  }

  public double getItemsTotal() {
    return itemsTotal;
  }

  public void setItemsTotal(double itemsTotal) {
    this.itemsTotal = itemsTotal;
  }

  public double getDiscount() {
    return discount;
  }

  public void setDiscount(double discount) {
    this.discount = discount;
  }

  public double getSurcharge() {
    return surcharge;
  }

  public void setSurcharge(double surcharge) {
    this.surcharge = surcharge;
  }

  public String getCashSessionId() {
    return cashSessionId;
  }

  public void setCashSessionId(String cashSessionId) {
    this.cashSessionId = cashSessionId;
  }

  public int getReturnsVersion() {
    return returnsVersion;
  }

  public void setReturnsVersion(int returnsVersion) {
    this.returnsVersion = returnsVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Sale sale = (Sale) o;
    return Objects.equals(id, sale.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Sale{"
        + "id='"
        + id
        + '\''
        + ", totalAmount="
        + totalAmount
        + ", totalCost="
        + totalCost
        + ", totalPaid="
        + totalPaid
        + ", change="
        + change
        + ", timestamp="
        + timestamp
        + ", items="
        + items.size()
        + '}';
  }
}
