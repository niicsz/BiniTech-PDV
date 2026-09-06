package com.binitech.pdv.config;

import com.binitech.pdv.adapters.outbound.HttpAuthenticationGateway;
import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AuthenticationClientConfig {
  @Bean
  AuthenticationGateway authenticationGateway(
      RestClient.Builder builder,
      @Value("${auth.service-url}") String serviceUrl,
      @Value("${auth.service-key}") String serviceKey,
      @Value("${auth.connect-timeout:2s}") String connectTimeout,
      @Value("${auth.read-timeout:5s}") String readTimeout) {
    URI uri = URI.create(serviceUrl);
    if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null) {
      throw new IllegalArgumentException("AUTH_SERVICE_URL deve ser uma URL HTTP(S) válida.");
    }
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(positiveTimeout(connectTimeout));
    factory.setReadTimeout(positiveTimeout(readTimeout));
    return new HttpAuthenticationGateway(
        builder
            .baseUrl(serviceUrl)
            .defaultHeader("X-Auth-Service-Key", serviceKey)
            .requestFactory(factory)
            .build());
  }

  private Duration positiveTimeout(String value) {
    Duration duration = DurationStyle.detectAndParse(value);
    if (duration.toMillis() < 1 || duration.toMillis() > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Timeout de autenticação deve ser positivo e limitado.");
    }
    return duration;
  }
}
