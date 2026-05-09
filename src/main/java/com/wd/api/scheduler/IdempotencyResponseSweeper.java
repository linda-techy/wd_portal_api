package com.wd.api.scheduler;

import com.wd.api.repository.IdempotencyResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * S5 PR1: weekly cleanup of expired idempotency cache rows. Runs every
 * Sunday 03:00 Asia/Kolkata. Manually-invokable via {@link #sweep()} for
 * tests.
 */
@Component
public class IdempotencyResponseSweeper {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyResponseSweeper.class);

    private final IdempotencyResponseRepository repo;

    public IdempotencyResponseSweeper(IdempotencyResponseRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "0 0 3 * * SUN", zone = "Asia/Kolkata")
    public void sweep() {
        int deleted = repo.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("IdempotencyResponseSweeper deleted {} expired rows", deleted);
    }
}
