package com.binitech.pdv.domain.exception;

public class EmailProcessingException extends RuntimeException {

  public EmailProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
