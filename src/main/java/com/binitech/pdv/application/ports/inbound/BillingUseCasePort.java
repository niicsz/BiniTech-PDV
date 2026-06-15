package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Invoice;
import com.binitech.pdv.domain.Subscription;
import java.util.List;
import java.util.Optional;

public interface BillingUseCasePort {

  Subscription createSubscription(String tenantId, String planTier);

  Invoice generateMonthlyInvoice(
      String tenantId, int activeProducts, int monthlySales, int operators);

  Invoice markInvoicePaid(String stripeInvoiceId, String stripeSubscriptionId);

  void recordPaymentFailure(String stripeSubscriptionId);

  void blockOverdueTenants(int graceDays);

  List<Invoice> getInvoicesForTenant(String tenantId);

  Optional<Subscription> getSubscriptionForTenant(String tenantId);

  String createCheckoutUrl(String tenantId);

  String createPortalUrl(String tenantId);

  Optional<Subscription> activateFromCheckout(
      String stripeSubscriptionId,
      String stripeCustomerId,
      String externalReference,
      String stripePriceId);

  Subscription manuallyActivate(String tenantId);
}
