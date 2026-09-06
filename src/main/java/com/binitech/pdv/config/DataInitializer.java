package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.outbound.UserRepositoryPort;
import com.binitech.pdv.utils.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Never creates or resets credentials. Bootstrap identities are provisioned in Auth. */
@Component
public class DataInitializer implements CommandLineRunner {
  private final UserRepositoryPort users;
  private final String adminUsername;

  public DataInitializer(
      UserRepositoryPort users, @Value("${admin.username}") String adminUsername) {
    this.users = users;
    this.adminUsername = adminUsername;
  }

  public void run(String... args) {
    var admin = users.findByUsernameAndTenantIdIsNull(adminUsername);
    if (admin.isEmpty() || admin.get().getRole() != Role.SUPER_ADMIN) {
      throw new IllegalStateException(
          "Provision the platform administrator identity and PDV membership before startup.");
    }
  }
}
