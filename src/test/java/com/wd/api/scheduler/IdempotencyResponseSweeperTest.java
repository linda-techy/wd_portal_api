package com.wd.api.scheduler;

import com.wd.api.model.IdempotencyResponse;
import com.wd.api.repository.IdempotencyResponseRepository;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyResponseSweeperTest extends TestcontainersPostgresBase {

    @Autowired IdempotencyResponseRepository repo;
    @Autowired IdempotencyResponseSweeper sweeper;

    @BeforeEach
    void clean() { repo.deleteAll(); }

    @Test
    void sweepDeletesExpiredRowsOnly() {
        LocalDateTime now = LocalDateTime.now();
        repo.save(IdempotencyResponse.builder().idempotencyKey("expired-1")
                .requestMethod("POST").requestPath("/api/site-reports")
                .responseStatus(201).responseBody("{}").responseContentType("application/json")
                .cachedAt(now.minusDays(2)).expiresAt(now.minusDays(1)).build());
        repo.save(IdempotencyResponse.builder().idempotencyKey("live-1")
                .requestMethod("POST").requestPath("/api/site-reports")
                .responseStatus(201).responseBody("{}").responseContentType("application/json")
                .cachedAt(now).expiresAt(now.plusHours(12)).build());

        sweeper.sweep();

        assertThat(repo.findById("expired-1")).isEmpty();
        assertThat(repo.findById("live-1")).isPresent();
    }
}
