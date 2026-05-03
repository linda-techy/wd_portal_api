package com.wd.api.estimation.domain.enums;

/**
 * Estimation lifecycle states.
 *
 * <p>Valid transitions for the Sub-project F workflow:
 * <ul>
 *   <li>DRAFT → SENT (markSent)</li>
 *   <li>SENT → ACCEPTED (markAccepted) — also flips lead status to project_won</li>
 *   <li>SENT → REJECTED (markRejected)</li>
 *   <li>SENT | REJECTED → DRAFT (revertToDraft) — ACCEPTED cannot be reverted</li>
 * </ul>
 *
 * <p>DRAFT must remain first for Flutter byName() round-trip compatibility.
 */
public enum EstimationStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    SENT,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
