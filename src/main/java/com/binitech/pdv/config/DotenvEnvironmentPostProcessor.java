package com.binitech.pdv.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {

    Path envFile = Path.of(".env");
    if (!Files.exists(envFile)) {
      log.info("Arquivo .env não encontrado, pulando carregamento de variáveis");
      return;
    }

    try {
      Map<String, Object> envVars = new HashMap<>();
      Files.readAllLines(envFile).stream()
          .filter(line -> !line.isBlank() && !line.startsWith("#"))
          .filter(line -> line.contains("="))
          .forEach(
              line -> {
                int idx = line.indexOf('=');
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (System.getenv(key) == null) {
                  envVars.put(key, value);
                }
              });

      if (!envVars.isEmpty()) {
        environment.getPropertySources().addLast(new MapPropertySource("dotenv", envVars));
        log.info("Arquivo .env carregado com {} variável(is)", envVars.size());
      }
    } catch (IOException e) {
      log.error("Erro ao ler arquivo .env: {}", e.getMessage(), e);
    }
  }
}
