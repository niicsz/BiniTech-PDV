package com.binitech.pdv.domain.exception;

public class FiscalProviderException extends BusinessException {

  public FiscalProviderException(String message) {
    super(message);
  }

  public FiscalProviderException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
