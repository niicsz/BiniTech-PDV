package com.binitech.pdv.domain;

import java.time.Instant;

public record SlowRequestEvent(
    String service,
    String method,
    String path,
    int status,
    long durationMs,
    Instant occurredAt,
    int suppressed) {}
