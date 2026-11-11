package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.DeliveryFulfillment;
import com.binitech.pdv.utils.enums.DeliveryPaymentMethod;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

public final class DeliveryWhatsappMessage {

  private static final Locale PT_BR = Locale.of("pt", "BR");
  private static final char NO_BREAK_SPACE = (char) 0xA0;

  private DeliveryWhatsappMessage() {}

  public static String url(String whatsapp, String text) {
    return "https://wa.me/"
        + whatsapp
        + "?text="
        + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
  }

  public static String text(
      DeliveryOrder order, DeliveryStore store, String storeName, String trackUrl) {
    StringBuilder text = new StringBuilder();
    text.append("*Novo pedido #").append(order.getNumber()).append("* - ").append(storeName);
    text.append("\n\n*Itens*");
    for (DeliveryOrderItem item : order.getItems()) {
      text.append('\n')
          .append(item.quantity())
          .append("x ")
          .append(item.description())
          .append(" - ")
          .append(money(item.subtotal()));
      if (hasText(item.notes())) {
        text.append("\n   _Obs.: ").append(item.notes().strip()).append('_');
      }
    }
    text.append("\n\nSubtotal: ").append(money(order.getSubtotal()));
    if (order.getFulfillment() == DeliveryFulfillment.DELIVERY) {
      text.append("\nTaxa de entrega: ").append(money(order.getDeliveryFee()));
    }
    text.append("\n*Total: ").append(money(order.getTotal())).append('*');
    text.append("\n\n*Pagamento:* ").append(payment(order.getPaymentMethod()));
    if (order.isPaid()) {
      text.append(" (já pago)");
    }
    if (order.getChangeFor() != null) {
      text.append(" (troco para ").append(money(order.getChangeFor())).append(')');
    }
    if (order.getPaymentMethod() == DeliveryPaymentMethod.PIX
        && !order.isPaid()
        && hasText(store.getPixKey())) {
      text.append("\nChave Pix: ").append(store.getPixKey());
    }
    if (order.getFulfillment() == DeliveryFulfillment.DELIVERY) {
      DeliveryAddress address = order.getAddress();
      text.append("\n\n*Entrega:* ").append(address.singleLine());
      if (hasText(address.reference())) {
        text.append("\nReferência: ").append(address.reference());
      }
    } else {
      text.append("\n\n*Retirada na loja*");
    }
    text.append("\n\n*Cliente:* ")
        .append(order.getCustomerName())
        .append(" - ")
        .append(phone(order.getCustomerPhone()));
    if (hasText(order.getNotes())) {
      text.append("\nObs.: ").append(order.getNotes().strip());
    }
    text.append("\n\nAcompanhar: ").append(trackUrl);
    return text.toString();
  }

  public static String money(double value) {
    return NumberFormat.getCurrencyInstance(PT_BR).format(value).replace(NO_BREAK_SPACE, ' ');
  }

  private static String payment(DeliveryPaymentMethod method) {
    return switch (method) {
      case PIX -> "Pix";
      case CASH -> "Dinheiro";
      case CARD -> "Cartão na entrega";
    };
  }

  private static String phone(String digits) {
    String local = digits.startsWith("55") && digits.length() > 11 ? digits.substring(2) : digits;
    if (local.length() == 11) {
      return "(" + local.substring(0, 2) + ") " + local.substring(2, 7) + "-" + local.substring(7);
    }
    if (local.length() == 10) {
      return "(" + local.substring(0, 2) + ") " + local.substring(2, 6) + "-" + local.substring(6);
    }
    return digits;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
