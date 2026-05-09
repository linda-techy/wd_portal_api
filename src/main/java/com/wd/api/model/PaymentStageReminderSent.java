package com.wd.api.model;

import com.wd.api.model.enums.ReminderKind;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * S6 PR2 — record of a single (stage, reminder_kind) reminder having been
 * sent. Inserted by {@link com.wd.api.scheduler.PaymentMilestoneReminderJob}
 * BEFORE the webhook publish, with a UNIQUE constraint on (stage_id,
 * reminder_kind) acting as the idempotency key. Re-running the job for the
 * same (stage, kind) is a no-op.
 *
 * <p>The FK is modelled as a raw {@code stage_id} column rather than a
 * {@code @ManyToOne PaymentStage} association — this row is a write-only
 * dedup ledger; loading the parent stage would be wasted I/O.
 */
@Entity
@Table(
    name = "payment_stage_reminder_sent",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_psrs_stage_kind",
        columnNames = {"stage_id", "reminder_kind"}
    )
)
public class PaymentStageReminderSent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_kind", nullable = false, length = 16)
    private ReminderKind reminderKind;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStageId() { return stageId; }
    public void setStageId(Long stageId) { this.stageId = stageId; }

    public ReminderKind getReminderKind() { return reminderKind; }
    public void setReminderKind(ReminderKind reminderKind) { this.reminderKind = reminderKind; }

    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
}
