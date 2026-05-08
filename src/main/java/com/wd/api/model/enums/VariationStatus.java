package com.wd.api.model.enums;

/**
 * Status for project variations (CRs) under the v2 8-state workflow + REJECTED.
 *
 * <p>State diagram:
 * <pre>
 *   DRAFT
 *     |
 *     v (submit, CR_SUBMIT)
 *   SUBMITTED
 *     |
 *     v (cost, CR_COST)
 *   COSTED
 *     |
 *     v (sendToCustomer, CR_SEND_TO_CUSTOMER)
 *   CUSTOMER_APPROVAL_PENDING
 *     |
 *     v (approveByCustomer, OTP — PR3)
 *   APPROVED
 *     |
 *     v (schedule, CR_SCHEDULE — PR2 wires WBS merge)
 *   SCHEDULED
 *     |
 *     v (start, CR_START)
 *   IN_PROGRESS
 *     |
 *     v (complete, CR_COMPLETE)
 *   COMPLETE
 *
 *   any -> REJECTED via reject(reason, CR_REJECT)
 * </pre>
 *
 * <p>NOTE: legacy {@code PENDING_APPROVAL} value (pre-V127) is renamed
 * to {@link #CUSTOMER_APPROVAL_PENDING}. V127 includes a data fixup.
 */
public enum VariationStatus {
    DRAFT,
    SUBMITTED,
    COSTED,
    CUSTOMER_APPROVAL_PENDING,
    APPROVED,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETE,
    REJECTED
}
