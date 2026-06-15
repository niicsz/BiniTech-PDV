package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "spring.rabbitmq.host",
    havingValue = "disabled",
    matchIfMissing = true)
public class NoOpEmailServiceAdapter implements EmailServicePort {

  private static final Logger log = LoggerFactory.getLogger(NoOpEmailServiceAdapter.class);

  @Override
  public void sendTenantApprovalEmail(
      String to, String tenantName, String tenantSlug, String username, String tempPassword) {
    log.warn(
        "[EMAIL-NOOP] spring.mail.host não configurado. "
            + "Credenciais do tenant '{}' que deveriam ser enviadas para {}: username={} password={}",
        tenantName,
        to,
        username,
        tempPassword);
  }

  @Override
  public void sendPasswordResetEmail(
      String to, String tenantName, String username, String resetLink) {
    log.warn(
        "[EMAIL-NOOP] spring.mail.host não configurado. "
            + "Link de redefinição de senha para o usuário '{}' do tenant '{}' (enviar para {}): {}",
        username,
        tenantName,
        to,
        resetLink);
  }
}
