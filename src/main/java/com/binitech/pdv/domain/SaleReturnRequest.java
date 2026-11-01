package com.binitech.pdv.domain;

import com.binitech.pdv.utils.enums.RefundMethod;
import java.util.Map;

public record SaleReturnRequest(
    Map<String, Integer> quantities, RefundMethod refundMethod, String reason, boolean restock) {}
