package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.CashMovementType;
import java.time.LocalDateTime;

public record CashMovement(
    String id,
    CashMovementType type,
    double amount,
    String reason,
    String userId,
    LocalDateTime timestamp) {}
