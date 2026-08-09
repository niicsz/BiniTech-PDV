package com.binitech.pdv.adapters.inbound;

import com.binitech.pdv.adapters.outbound.ResendEmailServiceAdapter;
import com.binitech.pdv.config.RabbitMQConfig;
import com.binitech.pdv.domain.EmailEvent;
import com.binitech.pdv.domain.EmailEvent.EmailType;
import com.binitech.pdv.domain.exception.EmailProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class EmailEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(EmailEventConsumer.class);

  private final ResendEmailServiceAdapter resendAdapter;

  public EmailEventConsumer(@Autowired(required = false) ResendEmailServiceAdapter resendAdapter) {
    this.resendAdapter = resendAdapter;
  }

  @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
  public void consume(EmailEvent event) {
    if (resendAdapter == null) {
      if (event.type() == EmailType.PASSWORD_RESET) {
        log.warn(
            "[EMAIL-NOOP] RESEND_API_KEY não configurada. Link de redefinição de senha"
                + " para '{}' (enviar para {}): {}",
            event.username(),
            event.to(),
            event.actionLink());
      } else {
        log.warn(
            "[EMAIL-NOOP] Evento recebido mas RESEND_API_KEY não configurada. tenant={} to={}",
            event.tenantName(),
            event.to());
      }
      return;
    }
    try {
      resendAdapter.send(event);
    } catch (Exception e) {
      log.error("Falha ao processar e-mail para {}: {}", event.to(), e.getMessage());
      throw new EmailProcessingException("Falha ao processar envio de e-mail.", e);
    }
  }
}
