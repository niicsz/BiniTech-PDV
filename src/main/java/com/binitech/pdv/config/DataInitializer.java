package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.Enum.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${admin.username}")
  private String adminUsername;

  @Value("${admin.password}")
  private String adminPassword;

  public DataInitializer(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) {
    var existing = userRepository.findByUsername(adminUsername);
    if (existing.isEmpty()) {
      User admin = new User();
      admin.setUsername(adminUsername);
      admin.setPassword(passwordEncoder.encode(adminPassword));
      admin.setRole(Role.ADMIN);
      userRepository.save(admin);
      log.info("Usuário admin padrão criado com sucesso.");
    } else {
      User admin = existing.get();
      if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
        admin.setPassword(passwordEncoder.encode(adminPassword));
        userRepository.save(admin);
        log.info("Senha do admin atualizada conforme variável de ambiente.");
      } else {
        log.info("Usuário admin já existe e senha está atualizada.");
      }
    }
  }
}
