package com.binitech.pdv.domain.exception;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
    super(String.format("%s não encontrado com %s: '%s'", resourceName, fieldName, fieldValue));
  }
}
