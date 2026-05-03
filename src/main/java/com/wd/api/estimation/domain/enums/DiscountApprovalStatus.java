package com.wd.api.estimation.domain.enums;

/**
 * O — Discount approval lifecycle for estimations whose discount % exceeds the
 * configured threshold (default 5%).
 *
 * Null means no approval needed (discount at or below threshold).
 */
public enum DiscountApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
