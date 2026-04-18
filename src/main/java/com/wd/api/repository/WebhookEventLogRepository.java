package com.wd.api.repository;

import com.wd.api.model.WebhookEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {

    /**
     * Find retryable events: those with given status and fewer than maxAttempts tries.
     */
    List<WebhookEventLog> findByStatusAndAttemptsLessThan(String status, int maxAttempts);

    /**
     * Count events by status — useful for monitoring dashboards.
     */
    long countByStatus(String status);
}
