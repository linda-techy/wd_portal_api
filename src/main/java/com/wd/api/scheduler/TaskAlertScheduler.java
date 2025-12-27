package com.wd.api.scheduler;

import com.wd.api.service.TaskAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task Alert Scheduler - Automated Deadline Monitoring
 * 
 * Production-Grade Scheduling (Construction Business Hours):
 * - Runs daily at 9:00 AM IST (when construction teams start work)
 * - Uses Spring's @Scheduled with cron expression
 * - Can be disabled via configuration for testing
 * - Fault-tolerant: errors don't stop future executions
 * 
 * Configuration:
 * - Enable/disable: task.alerts.enabled=true/false in application.properties
 * - Schedule: task.alerts.cron=0 0 9 * * * (default: daily 9 AM)
 * - Timezone: task.alerts.timezone=Asia/Kolkata
 * 
 * Monitoring:
 * - Comprehensive logging at INFO level
 * - Error handling prevents scheduler from dying
 * - Manual trigger endpoint available for testing
 * 
 * @author Senior Engineer (15+ years construction domain)
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "task.alerts.enabled", havingValue = "true", matchIfMissing = true // Enabled by default
)
public class TaskAlertScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TaskAlertScheduler.class);

    @Autowired
    private TaskAlertService taskAlertService;

    /**
     * Scheduled task deadline check
     * 
     * Cron Expression: "0 0 9 * * *"
     * - Seconds: 0
     * - Minutes: 0
     * - Hours: 9
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: * (all days)
     * 
     * Timezone: Asia/Kolkata (IST UTC+5:30)
     * 
     * Business Rationale:
     * 9 AM is when construction teams start work, making it the ideal time
     * for managers/admins to review overdue and upcoming tasks
     */
    @Scheduled(cron = "${task.alerts.cron:0 0 9 * * *}", zone = "${task.alerts.timezone:Asia/Kolkata}")
    public void scheduledTaskDeadlineCheck() {
        logger.info("==========================================");
        logger.info("Starting scheduled task deadline check...");
        logger.info("==========================================");

        try {
            long startTime = System.currentTimeMillis();

            // Execute alert logic
            taskAlertService.sendTaskDeadlineAlerts();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Scheduled task deadline check completed successfully in {}ms", duration);

        } catch (Exception e) {
            // Log error but don't re-throw - scheduler should continue
            logger.error("ERROR during scheduled task deadline check", e);
            logger.error("Alert check failed, but scheduler will continue. "
                    + "Next run: as per cron schedule");
        }

        logger.info("==========================================");
    }

    /**
     * Manual trigger for testing/admin use
     * Can be called from REST endpoint or admin panel
     * 
     * Usage:
     * - Testing during development
     * - Force alert check outside schedule
     * - Troubleshooting alert system
     */
    public void triggerManualCheck() {
        logger.warn("========================================");
        logger.warn("MANUAL task deadline check triggered");
        logger.warn("========================================");

        try {
            taskAlertService.sendTaskDeadlineAlerts();
            logger.info("Manual check completed successfully");
        } catch (Exception e) {
            logger.error("ERROR during manual task check", e);
            throw e; // Re-throw for manual calls so caller knows it failed
        }
    }

    /**
     * Get scheduler status info (for health checks)
     */
    public SchedulerStatus getStatus() {
        return new SchedulerStatus(
                true,
                "Daily at 9:00 AM IST",
                "Monitoring task deadlines for OVERDUE, DUE_TODAY, and DUE_SOON alerts");
    }

    // Simple DTO for status info
    public static class SchedulerStatus {
        public final boolean enabled;
        public final String schedule;
        public final String description;

        public SchedulerStatus(boolean enabled, String schedule, String description) {
            this.enabled = enabled;
            this.schedule = schedule;
            this.description = description;
        }
    }
}
