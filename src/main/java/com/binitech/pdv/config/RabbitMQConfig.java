package com.binitech.pdv.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String EMAIL_EXCHANGE = "email.exchange";
  public static final String EMAIL_QUEUE = "email.queue";
  public static final String EMAIL_ROUTING_KEY = "email.approval";

  @Bean
  public TopicExchange emailExchange() {
    return new TopicExchange(EMAIL_EXCHANGE, true, false);
  }

  @Bean
  public Queue emailQueue() {
    return QueueBuilder.durable(EMAIL_QUEUE).build();
  }

  @Bean
  public Binding emailBinding(Queue emailQueue, TopicExchange emailExchange) {
    return BindingBuilder.bind(emailQueue).to(emailExchange).with(EMAIL_ROUTING_KEY);
  }

  @Bean
  public Jackson2JsonMessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    return template;
  }
}
