package com.binitech.pdv.domain;

public record EmailEvent(
    EmailType type,
    String to,
    String tenantName,
    String tenantSlug,
    String username,
    String tempPassword,
    String actionLink) {

  public enum EmailType {
    TENANT_APPROVAL,
    PASSWORD_RESET
  }

  public static EmailEvent approval(
      String to, String tenantName, String tenantSlug, String username, String tempPassword) {
    return new EmailEvent(
        EmailType.TENANT_APPROVAL, to, tenantName, tenantSlug, username, tempPassword, null);
  }

  public static EmailEvent passwordReset(
      String to, String tenantName, String username, String actionLink) {
    return new EmailEvent(
        EmailType.PASSWORD_RESET, to, tenantName, null, username, null, actionLink);
  }
}
