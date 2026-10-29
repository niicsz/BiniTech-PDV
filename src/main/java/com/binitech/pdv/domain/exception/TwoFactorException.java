package com.binitech.pdv.domain.exception;

import java.time.Instant;

public class TwoFactorException extends RuntimeException {
  private final int status;
  private final String code;
  private final Integer remainingAttempts;
  private final Instant lockedUntil;

  public TwoFactorException(
      int status, String code, String message, Integer remainingAttempts, Instant lockedUntil) {
    super(message);
    this.status = status;
    this.code = code;
    this.remainingAttempts = remainingAttempts;
    this.lockedUntil = lockedUntil;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public Integer remainingAttempts() {
    return remainingAttempts;
  }

  public Instant lockedUntil() {
    return lockedUntil;
  }
}
