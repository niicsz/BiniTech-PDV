package com.binitech.pdv.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.binitech.pdv.application.ports.outbound.AuthenticationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthenticationClientConfigTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
          .withUserConfiguration(AuthenticationClientConfig.class)
          .withPropertyValues("auth.service-key=test-machine-key-with-at-least-32-bytes");

  @Test
  void defaults_createHttpClientWithBoundedTimeouts() {
    runner
        .withPropertyValues("auth.service-url=http://localhost:8081")
        .run(context -> assertThat(context).hasSingleBean(AuthenticationGateway.class));
  }

  @Test
  void invalidServiceUrl_failsAtStartup() {
    runner
        .withPropertyValues("auth.service-url=file:///tmp/auth")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void unboundedTimeout_failsAtStartup() {
    runner
        .withPropertyValues("auth.service-url=http://localhost:8081", "auth.read-timeout=0s")
        .run(context -> assertThat(context).hasFailed());
  }
}
