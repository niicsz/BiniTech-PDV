package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.utils.LogSanitizer;
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
        "[EMAIL-NOOP] RESEND_API_KEY não configurada. "
            + "E-mail de aprovação (com credenciais) do tenant '{}' não foi enviado para o "
            + "destinatário configurado. Configure o servidor de e-mail para reenviar.",
        LogSanitizer.maskUsername(tenantName));
  }

  @Override
  public void sendPasswordResetEmail(
      String to, String tenantName, String username, String resetLink) {
    log.warn(
        "[EMAIL-NOOP] RESEND_API_KEY não configurada. "
            + "E-mail de redefinição de senha do usuário '{}' (tenant '{}') não foi enviado. "
            + "Configure o servidor de e-mail para reenviar.",
        LogSanitizer.maskUsername(username),
        LogSanitizer.maskUsername(tenantName));
  }
}
