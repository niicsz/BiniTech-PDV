package com.binitech.pdv.config;

import com.binitech.pdv.application.ports.outbound.CheckoutSessionRepositoryPort;

public record BillingAsaasConfig(
    String frontendUrl,
    AsaasGateway asaasGateway,
    CheckoutSessionRepositoryPort checkoutSessionRepository) {}
