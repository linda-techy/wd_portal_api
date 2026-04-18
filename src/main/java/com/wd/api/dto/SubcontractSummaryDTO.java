package com.wd.api.dto;

import java.math.BigDecimal;

public record SubcontractSummaryDTO(
        BigDecimal contractedAmount,
        BigDecimal totalMeasuredAmount,
        BigDecimal totalApprovedAmount,
        BigDecimal totalPaidAmount,
        BigDecimal totalRetentionHeld,
        BigDecimal totalRetentionReleased,
        BigDecimal balancePayable,
        int totalMeasurements,
        int pendingMeasurements,
        int approvedMeasurements,
        int totalPayments
) {}
