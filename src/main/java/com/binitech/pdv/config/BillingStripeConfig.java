package com.binitech.pdv.config;

public record BillingStripeConfig(
    String frontendUrl, StripeGateway stripeGateway, StripeProperties stripeProperties) {}
