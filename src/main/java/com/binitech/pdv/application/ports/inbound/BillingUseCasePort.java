package com.binitech.pdv.application.ports.inbound;

import com.binitech.pdv.domain.Invoice;
import com.binitech.pdv.domain.Subscription;
import com.binitech.pdv.utils.enums.PaymentMethod;
import java.util.List;
import java.util.Optional;

public interface BillingUseCasePort {

  Subscription createSubscription(String tenantId, String planTier);

  Invoice generateMonthlyInvoice(
      String tenantId, int activeProducts, int monthlySales, int operators);

  Invoice markInvoicePaid(String asaasPaymentId, String asaasSubscriptionId);

  void recordPaymentFailure(String asaasSubscriptionId);

  void blockOverdueTenants(int graceDays);

  List<Invoice> getInvoicesForTenant(String tenantId);

  Optional<Subscription> getSubscriptionForTenant(String tenantId);

  String createCheckoutUrl(String tenantId);

  String createPortalUrl(String tenantId);

  Optional<Subscription> activateFromCheckout(
      String asaasSubscriptionId, String asaasCustomerId, String externalReference);

  Optional<Subscription> activateFromCheckoutSession(
      String checkoutSessionId,
      String asaasSubscriptionId,
      String asaasCustomerId,
      String externalReference);

  void confirmSubscriptionPayment(
      String asaasPaymentId,
      String asaasSubscriptionId,
      String asaasCustomerId,
      String checkoutSessionId);

  Subscription manuallyActivate(String tenantId, PaymentMethod paymentMethod);
}
