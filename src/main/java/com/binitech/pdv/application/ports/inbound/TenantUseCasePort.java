package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Tenant;
import com.binitech.pdv.domain.User;
import com.binitech.pdv.utils.Enum.TenantStatus;
import java.util.List;

public interface TenantUseCasePort {

  Tenant createTenant(String name, String slug, String planId, String billingEmail);

  List<User> getUsersByTenant(String tenantId);

  Tenant getTenantById(String id);

  Tenant getTenantBySlug(String slug);

  List<Tenant> getAllTenants();

  List<Tenant> getTenantsByStatus(TenantStatus status);

  Tenant approveTenant(String tenantId);

  Tenant blockTenant(String tenantId, String reason);

  Tenant updateTenantStatus(String tenantId, TenantStatus newStatus);
}
