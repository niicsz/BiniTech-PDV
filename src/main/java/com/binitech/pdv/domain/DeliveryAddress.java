package com.binitech.pdv.domain;

public record DeliveryAddress(
    String street, String number, String complement, String district, String reference) {

  public String singleLine() {
    StringBuilder line = new StringBuilder(street).append(", ").append(number);
    if (complement != null && !complement.isBlank()) {
      line.append(" - ").append(complement);
    }
    return line.append(" - ").append(district).toString();
  }
}
