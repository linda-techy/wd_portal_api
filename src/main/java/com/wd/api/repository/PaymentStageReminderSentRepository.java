package com.wd.api.repository;

import com.wd.api.model.PaymentStageReminderSent;
import com.wd.api.model.enums.ReminderKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * S6 PR2 — repository for the daily reminder dedup ledger.
 *
 * The custom {@link #insertIfAbsent(Long, ReminderKind)} method uses Postgres
 * {@code INSERT ... ON CONFLICT DO NOTHING} so the (stage_id, reminder_kind)
 * unique constraint is the single source of truth for "have we sent this
 * already" — racing instances of the scheduled job get exactly one winner per
 * (stage, kind), and re-runs on the same day are a no-op.
 */
@Repository
public interface PaymentStageReminderSentRepository
        extends JpaRepository<PaymentStageReminderSent, Long> {

    /**
     * Atomic "insert if not present" using Postgres ON CONFLICT DO NOTHING.
     *
     * @return {@code true} if a new row was inserted, {@code false} if the
     *         (stage_id, reminder_kind) pair already existed (insert was a no-op).
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO payment_stage_reminder_sent (stage_id, reminder_kind, sent_at)
        VALUES (:stageId, CAST(:kind AS VARCHAR), NOW())
        ON CONFLICT (stage_id, reminder_kind) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsentRaw(@Param("stageId") Long stageId, @Param("kind") String kind);

    /**
     * Convenience wrapper around {@link #insertIfAbsentRaw} that converts the
     * row-count to a boolean. Default method so it doesn't need its own query.
     */
    default boolean insertIfAbsent(Long stageId, ReminderKind kind) {
        return insertIfAbsentRaw(stageId, kind.name()) == 1;
    }
}
