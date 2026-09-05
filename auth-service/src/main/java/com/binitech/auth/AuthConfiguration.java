package com.binitech.auth;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class AuthConfiguration {
  @Bean
  PasswordEncoder passwordEncoder(@Value("${security.pepper}") String pepper) {
    if (pepper == null || pepper.isBlank()) {
      throw new IllegalArgumentException("SECURITY_PEPPER é obrigatório.");
    }
    PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    return new PasswordEncoder() {
      @Override
      public String encode(CharSequence rawPassword) {
        return argon2.encode(rawPassword.toString() + pepper);
      }

      @Override
      public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return argon2.matches(rawPassword.toString() + pepper, encodedPassword);
      }
    };
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.cors(Customizer.withDefaults())
        // Only JSON requests and explicit bearer tokens are used; no cookie authentication.
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/**"))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/auth/session",
                        "/actuator/health",
                        "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${cors.allowed-origins}") String origins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(
        Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
    config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/auth/**", config);
    return source;
  }
}
