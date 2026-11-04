package com.binitech.pdv.domain.exception;

public class SessionRejectedException extends RuntimeException {
  public SessionRejectedException() {
    super("Sua sessão foi encerrada. Entre novamente.");
  }
}
