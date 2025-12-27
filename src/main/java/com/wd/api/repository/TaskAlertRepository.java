package com.wd.api.repository;

import com.wd.api.model.TaskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TaskAlert Repository - Alert History and Analytics
 * 
 * Key Queries:
 * - Duplicate alert prevention (24-hour window)
 * - Alert history by type for analytics
 * - Failed delivery tracking for retry logic
 */
@Repository
public interface TaskAlertRepository extends JpaRepository<TaskAlert, Long> {

    /**
     * Check if a similar alert was sent recently (prevents spam)
     * Used by: TaskAlertService to avoid duplicate alerts within 24 hours
     * 
     * @param taskId    The task ID
     * @param alertType The type of alert
     * @param since     Timestamp threshold (usually 24 hours ago)
     * @return true if recent alert exists
     */
    @Query("SELECT COUNT(a) > 0 FROM TaskAlert a " +
            "WHERE a.task.id = :taskId " +
            "AND a.alertType = :alertType " +
            "AND a.sentAt > :since")
    boolean existsRecentAlert(
            @Param("taskId") Long taskId,
            @Param("alertType") TaskAlert.AlertType alertType,
            @Param("since") LocalDateTime since);

    /**
     * Find alerts by type within date range (for analytics/reports)
     * 
     * @param alertType Type of alert
     * @param since     Start date for query
     * @return List of matching alerts
     */
    List<TaskAlert> findByAlertTypeAndSentAtAfterOrderBySentAtDesc(
            TaskAlert.AlertType alertType,
            LocalDateTime since);

    /**
     * Find all alerts for a specific task (task history)
     * 
     * @param taskId The task ID
     * @return List of alerts for the task
     */
    @Query("SELECT a FROM TaskAlert a WHERE a.task.id = :taskId ORDER BY a.sentAt DESC")
    List<TaskAlert> findByTaskId(@Param("taskId") Long taskId);

    /**
     * Find failed delivery alerts for retry logic
     * 
     * @param status Delivery status to filter
     * @return List of failed alerts
     */
    List<TaskAlert> findByDeliveryStatusOrderBySentAtDesc(TaskAlert.DeliveryStatus status);

    /**
     * Count alerts by severity (for dashboard metrics)
     * 
     * @param severity Alert severity
     * @param since    Start date for counting
     * @return Number of alerts
     */
    @Query("SELECT COUNT(a) FROM TaskAlert a " +
            "WHERE a.severity = :severity " +
            "AND a.sentAt > :since")
    long countBySeveritySince(
            @Param("severity") TaskAlert.AlertSeverity severity,
            @Param("since") LocalDateTime since);
}
