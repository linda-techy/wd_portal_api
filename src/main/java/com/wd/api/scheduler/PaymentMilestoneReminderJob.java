package com.wd.api.scheduler;

import com.wd.api.model.PaymentStage;
import com.wd.api.model.enums.ReminderKind;
import com.wd.api.repository.PaymentStageReminderSentRepository;
import com.wd.api.repository.PaymentStageRepository;
import com.wd.api.service.WebhookPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * S6 PR2 — daily 09:00 IST scan of {@code payment_stages} that fires up to one
 * webhook per (stage, kind) reminder per day.
 *
 * <p><b>Selection rules</b> (applied to each candidate returned by
 * {@link PaymentStageRepository#findCandidatesForReminder()}):
 * <ul>
 *   <li>{@code due_date == today + 3 days} → {@link ReminderKind#T_MINUS_3}</li>
 *   <li>{@code due_date == today}           → {@link ReminderKind#DUE_TODAY}</li>
 *   <li>{@code due_date < today AND status == OVERDUE} → {@link ReminderKind#OVERDUE}</li>
 *   <li>otherwise → skip (e.g. INVOICED-but-past-due waits for the OVERDUE auto-flip job)</li>
 * </ul>
 *
 * <p><b>Idempotency:</b> for each (stage, kind), the job first attempts an
 * INSERT-ON-CONFLICT-DO-NOTHING into {@code payment_stage_reminder_sent}. If
 * the insert affects 1 row → publish. If 0 rows → already sent today (or
 * earlier), skip silently. Re-running the job on the same day is a no-op.
 *
 * <p><b>Order:</b> insert first, publish second. If the publish fails after
 * the insert commits, the customer doesn't get that day's reminder — accepted
 * trade-off (under-notify > over-notify; same as S3 PR3 HANDOVER_SHIFT).
 *
 * <p><b>Profile gating:</b> the bean is excluded from the {@code test} profile
 * so the cron does not fire inside the JUnit suite (where
 * {@code TestcontainersPostgresBase} sets {@code spring.profiles.active=test}).
 * Tests instantiate the job directly with a {@link Clock#fixed(java.time.Instant, java.time.ZoneId)}.
 */
@Component
@Profile("!test")
public class PaymentMilestoneReminderJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentMilestoneReminderJob.class);

    private final PaymentStageRepository stageRepo;
    private final PaymentStageReminderSentRepository reminderRepo;
    private final WebhookPublisherService webhookPublisher;
    private final Clock clock;

    public PaymentMilestoneReminderJob(PaymentStageRepository stageRepo,
                                       PaymentStageReminderSentRepository reminderRepo,
                                       WebhookPublisherService webhookPublisher,
                                       Clock clock) {
        this.stageRepo = stageRepo;
        this.reminderRepo = reminderRepo;
        this.webhookPublisher = webhookPublisher;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void run() {
        LocalDate today = LocalDate.now(clock);
        List<PaymentStage> candidates = stageRepo.findCandidatesForReminder();
        log.info("PaymentMilestoneReminderJob: scanning {} candidate stage(s) for today={}",
                candidates.size(), today);

        int sent = 0;
        for (PaymentStage stage : candidates) {
            ReminderKind kind = classify(stage, today);
            if (kind == null) continue;

            boolean inserted = reminderRepo.insertIfAbsent(stage.getId(), kind);
            if (!inserted) {
                log.debug("Reminder already sent for stage={} kind={} — skipping", stage.getId(), kind);
                continue;
            }

            ReminderContext ctx = ReminderContext.from(stage);
            webhookPublisher.publishPaymentMilestoneDue(kind, ctx);
            sent++;
        }
        log.info("PaymentMilestoneReminderJob: dispatched {} reminder(s)", sent);
    }

    /** null = no reminder fires for this stage today. */
    static ReminderKind classify(PaymentStage stage, LocalDate today) {
        LocalDate due = stage.getDueDate();
        if (due == null) return null;

        // Defence-in-depth: the SQL filter already excludes PAID + ON_HOLD,
        // but the spec is explicit ("PAID NEVER fires, ON_HOLD NEVER fires"),
        // so re-check in-memory in case a future schema/query change leaks one
        // through.
        com.wd.api.model.enums.PaymentStageStatus status = stage.getStatus();
        if (status == com.wd.api.model.enums.PaymentStageStatus.PAID
                || status == com.wd.api.model.enums.PaymentStageStatus.ON_HOLD) {
            return null;
        }

        if (due.equals(today.plusDays(3))) return ReminderKind.T_MINUS_3;
        if (due.equals(today))             return ReminderKind.DUE_TODAY;
        if (due.isBefore(today) && status == com.wd.api.model.enums.PaymentStageStatus.OVERDUE) {
            return ReminderKind.OVERDUE;
        }
        return null;
    }

    /**
     * Pure-data carrier passed from the job to the publisher. Avoids leaking
     * the JPA entity through service boundaries — the publisher only needs
     * the fields it'll embed in the webhook payload.
     */
    public record ReminderContext(
            Long projectId,
            Long stageId,
            Integer stageNumber,
            String stageName,
            LocalDate dueDate,
            BigDecimal netPayableAmount
    ) {
        static ReminderContext from(PaymentStage s) {
            Long projectId = s.getProject() != null ? s.getProject().getId() : null;
            return new ReminderContext(
                    projectId,
                    s.getId(),
                    s.getStageNumber(),
                    s.getStageName(),
                    s.getDueDate(),
                    s.getNetPayableAmount()
            );
        }
    }
}
