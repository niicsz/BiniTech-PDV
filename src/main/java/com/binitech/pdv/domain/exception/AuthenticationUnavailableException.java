package com.binitech.pdv.domain.exception;

public class AuthenticationUnavailableException extends RuntimeException {
  public AuthenticationUnavailableException() {
    super("Autenticação temporariamente indisponível. Tente novamente mais tarde.");
  }
}
