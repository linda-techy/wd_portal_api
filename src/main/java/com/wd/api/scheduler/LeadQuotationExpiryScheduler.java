package com.wd.api.scheduler;

import com.wd.api.repository.LeadQuotationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily housekeeping job that flips {@code SENT}/{@code VIEWED} quotations
 * past their validity window to {@code EXPIRED}.
 *
 * <p>Without this, the {@code EXPIRED} status defined on
 * {@link com.wd.api.model.LeadQuotation} was a comment in the entity but
 * not a behaviour — a quote with {@code validityDays = 30} sent 90 days ago
 * would still appear as {@code SENT} forever, and customers could accept
 * stale prices.
 *
 * <p>{@code @EnableScheduling} is already configured in
 * {@link TaskAlertScheduler}, so this component picks up the schedule
 * without additional configuration.
 *
 * <p>Cron schedule: 02:15 every day (local server time). Chosen to avoid
 * other portal scheduled jobs and to run during low-traffic hours.
 */
@Component
public class LeadQuotationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(LeadQuotationExpiryScheduler.class);

    private final LeadQuotationRepository leadQuotationRepository;

    public LeadQuotationExpiryScheduler(LeadQuotationRepository leadQuotationRepository) {
        this.leadQuotationRepository = leadQuotationRepository;
    }

    /**
     * Daily sweep — UPDATE statement is atomic at the DB level so this is
     * safe even if multiple instances of the API run with overlapping schedules.
     */
    @Scheduled(cron = "0 15 2 * * *")
    @Transactional
    public void expireOverdueQuotations() {
        int updated = leadQuotationRepository.markExpiredQuotations();
        if (updated > 0) {
            log.info("Marked {} quotations as EXPIRED (validity window elapsed)", updated);
        } else {
            log.debug("No SENT/VIEWED quotations past validity window — nothing to expire");
        }
    }
}
