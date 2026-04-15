package com.wd.api.model.enums;

/** Action recorded in an immutable co_approval_history row. */
public enum ApprovalAction {
    APPROVED,
    REJECTED,
    /** Sent to a higher approval level. */
    ESCALATED,
    /** Returned to submitter for clarification/amendment. */
    RETURNED
}
