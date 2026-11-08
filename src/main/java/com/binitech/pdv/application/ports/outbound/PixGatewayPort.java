package com.binitech.pdv.application.ports.outbound;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface PixGatewayPort {

  enum ChargeState {
    PENDING,
    APPROVED,
    CANCELLED,
    REJECTED,
    UNKNOWN
  }

  record ChargeRequest(
      String idempotencyKey,
      BigDecimal amount,
      String description,
      String externalReference,
      OffsetDateTime expiresAt,
      String notificationUrl) {}

  record CreatedCharge(String providerPaymentId, String qrCode, String qrCodeBase64) {}

  String verifyAccount(String accessToken);

  CreatedCharge createCharge(String accessToken, ChargeRequest request);

  ChargeState fetchState(String accessToken, String providerPaymentId);

  void cancel(String accessToken, String providerPaymentId);

  boolean isValidSignature(
      String webhookSecret, String dataId, String requestId, String signatureHeader);
}
