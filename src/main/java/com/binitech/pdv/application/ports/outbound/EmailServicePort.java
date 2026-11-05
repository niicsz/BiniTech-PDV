package com.binitech.pdv.application.ports.outbound;

import com.binitech.pdv.domain.TrialEmail;

public interface EmailServicePort {

  void sendTenantApprovalEmail(
      String to, String tenantName, String tenantSlug, String username, String tempPassword);

  void sendWelcomeEmail(String to, String tenantName, String username);

  void sendPasswordResetEmail(String to, String tenantName, String username, String resetLink);

  void sendTrialEmail(String to, String tenantName, TrialEmail step);
}
