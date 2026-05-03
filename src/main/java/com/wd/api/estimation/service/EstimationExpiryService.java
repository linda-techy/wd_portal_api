package com.wd.api.estimation.service;

import com.wd.api.estimation.repository.EstimationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * L — auto-expires SENT estimations whose validUntil has passed.
 * ACCEPTED rows are intentionally untouched (acceptance freezes the deal even
 * if validUntil lapses afterwards).
 */
@Service
public class EstimationExpiryService {

    private static final Logger log = LoggerFactory.getLogger(EstimationExpiryService.class);

    private final EstimationRepository repo;

    public EstimationExpiryService(EstimationRepository repo) {
        this.repo = repo;
    }

    /** Runs nightly at 02:10 IST (offset from auth-cleanup at 02:00 to avoid lock contention). */
    @Scheduled(cron = "0 10 2 * * *", zone = "Asia/Kolkata")
    @Transactional
    public int expireOverdue() {
        int n = repo.markExpiredWhereSentAndOverdue(LocalDate.now());
        if (n > 0) log.info("Auto-expired {} estimations past validUntil", n);
        return n;
    }
}
