package com.binitech.pdv.application.ports.outbound;

public interface EmailServicePort {

  void sendTenantApprovalEmail(
      String to, String tenantName, String tenantSlug, String username, String tempPassword);

  void sendPasswordResetEmail(String to, String tenantName, String username, String resetLink);
}
