package com.wd.api.model.enums;

/**
 * S6 PR2 — kind of a payment-milestone reminder.
 *
 * Mapped 1:1 to the chk_psrs_kind CHECK constraint on
 * {@code payment_stage_reminder_sent.reminder_kind}. If a value is added or
 * renamed, V131's CHECK list must be updated in a follow-up migration.
 */
public enum ReminderKind {
    /** due_date is exactly 3 calendar days in the future (IST today + 3). */
    T_MINUS_3,
    /** due_date equals today (IST). */
    DUE_TODAY,
    /** due_date is in the past AND status has flipped to {@code OVERDUE}. */
    OVERDUE
}
