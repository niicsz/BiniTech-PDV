package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.PaymentMethod;
import java.util.Objects;

public class Payment {

  private PaymentMethod method;
  private double amount;

  public Payment() {}

  public Payment(PaymentMethod method, double amount) {
    this.method = method;
    this.amount = amount;
  }

  public PaymentMethod getMethod() {
    return method;
  }

  public void setMethod(PaymentMethod method) {
    this.method = method;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Payment payment = (Payment) o;
    return Double.compare(payment.amount, amount) == 0 && method == payment.method;
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, amount);
  }

  @Override
  public String toString() {
    return "Payment{method=" + method + ", amount=" + amount + '}';
  }
}
