package com.binitech.pdv.domain;

import java.time.Instant;

public record AccessLog(
    String event, String username, String tenantId, String ip, Instant occurredAt) {}
