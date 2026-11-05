package com.binitech.pdv.application.usecases;

import java.util.Locale;
import java.util.Set;

final class DisposableEmail {

  private static final Set<String> DOMAINS =
      Set.of(
          "mailinator.com",
          "guerrillamail.com",
          "guerrillamail.net",
          "sharklasers.com",
          "10minutemail.com",
          "10minutemail.net",
          "temp-mail.org",
          "tempmail.com",
          "tempmail.net",
          "yopmail.com",
          "trashmail.com",
          "getnada.com",
          "dispostable.com",
          "maildrop.cc",
          "throwawaymail.com",
          "emailondeck.com",
          "mohmal.com",
          "minuteinbox.com");

  private DisposableEmail() {}

  static boolean isDisposable(String email) {
    int at = email == null ? -1 : email.lastIndexOf('@');
    return at >= 0 && DOMAINS.contains(email.substring(at + 1).toLowerCase(Locale.ROOT));
  }
}
