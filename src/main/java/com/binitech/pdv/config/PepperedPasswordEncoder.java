package com.binitech.pdv.config;

import org.springframework.security.crypto.password.PasswordEncoder;

public class PepperedPasswordEncoder implements PasswordEncoder {

  private final PasswordEncoder delegate;
  private final String pepper;

  public PepperedPasswordEncoder(PasswordEncoder delegate, String pepper) {
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalArgumentException(
          "A variável SECURITY_PEPPER é obrigatória e não pode estar vazia. "
              + "Defina-a nas variáveis de ambiente do servidor.");
    }
    this.delegate = delegate;
    this.pepper = pepper;
  }

  @Override
  public String encode(CharSequence rawPassword) {
    return delegate.encode(rawPassword.toString() + pepper);
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return delegate.matches(rawPassword.toString() + pepper, encodedPassword);
  }

  @Override
  public boolean upgradeEncoding(String encodedPassword) {
    return delegate.upgradeEncoding(encodedPassword);
  }
}
