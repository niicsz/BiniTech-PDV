package com.binitech.pdv.adapters.outbound;

import com.binitech.pdv.application.ports.outbound.EmailServicePort;
import com.binitech.pdv.config.RabbitMQConfig;
import com.binitech.pdv.domain.EmailEvent;
import com.binitech.pdv.domain.TrialEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMQEmailPublisher implements EmailServicePort {

  private static final Logger log = LoggerFactory.getLogger(RabbitMQEmailPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public RabbitMQEmailPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void sendTenantApprovalEmail(
      String to, String tenantName, String tenantSlug, String username, String tempPassword) {
    EmailEvent event = EmailEvent.approval(to, tenantName, tenantSlug, username, tempPassword);
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, event);
    log.info("Evento de aprovação de e-mail enfileirado para: {}", to);
  }

  @Override
  public void sendWelcomeEmail(String to, String tenantName, String username) {
    EmailEvent event = EmailEvent.welcome(to, tenantName, username);
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, event);
    log.info("Evento de redefinição de senha enfileirado para: {}", to);
  }

  @Override
  public void sendPasswordResetEmail(
      String to, String tenantName, String username, String resetLink) {
    EmailEvent event = EmailEvent.passwordReset(to, tenantName, username, resetLink);
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, event);
    log.info("Evento de redefinição de senha enfileirado para: {}", to);
  }

  @Override
  public void sendTrialEmail(String to, String tenantName, TrialEmail step) {
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EMAIL_EXCHANGE,
        RabbitMQConfig.EMAIL_ROUTING_KEY,
        EmailEvent.trial(to, tenantName, step));
    log.info("E-mail do teste grátis enfileirado: etapa={}", step);
  }
}
