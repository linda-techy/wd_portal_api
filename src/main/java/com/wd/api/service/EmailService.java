package com.wd.api.service;

import com.wd.api.model.Task;
import com.wd.api.model.TaskAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Email Service - Production-Ready Stub
 * 
 * Purpose: Handle task alert email delivery
 * 
 * PRODUCTION TODO:
 * - Integrate with email provider (SendGrid, AWS SES, Mailgun, etc.)
 * - Use JavaMailSender for SMTP
 * - Implement @Async for non-blocking sends
 * - Add retry logic for failed deliveries
 * - Use HTML templates for professional formatting
 * 
 * Current Implementation:
 * - Logs alerts (development/testing)
 * - Provides interface for future integration
 * - Allows system to run without email dependencies
 * 
 * @author Senior Engineer
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /**
     * Send task alert email to user
     * 
     * @param toEmail  Recipient email address
     * @param task     Task triggering the alert
     * @param message  Alert message content
     * @param severity Alert severity level
     */
    public void sendTaskAlert(String toEmail, Task task,
            String message, TaskAlert.AlertSeverity severity) {

        // Production implementation would use actual email sending:
        /*
         * try {
         * MimeMessage email = mailSender.createMimeMessage();
         * MimeMessageHelper helper = new MimeMessageHelper(email, true);
         * 
         * helper.setTo(toEmail);
         * helper.setSubject(getSubject(severity, task));
         * helper.setText(formatEmailHtml(message, task), true);
         * helper.setFrom("alerts@walldotbuilders.com");
         * 
         * mailSender.send(email);
         * logger.info("Task alert email sent to {}", toEmail);
         * } catch (Exception e) {
         * logger.error("Failed to send email to {}", toEmail, e);
         * throw new RuntimeException("Email delivery failed", e);
         * }
         */

        // Development stub: Log the alert
        logger.info("===============================================");
        logger.info("TASK ALERT EMAIL [{}]", severity);
        logger.info("To: {}", toEmail);
        logger.info("Task ID: {}", task.getId());
        logger.info("-----------------------------------------------");
        logger.info("{}", message);
        logger.info("===============================================");
    }

    /**
     * Generate email subject based on severity
     */
    private String getSubject(TaskAlert.AlertSeverity severity, Task task) {
        return switch (severity) {
            case CRITICAL -> "🚨 CRITICAL: Task Overdue - " + task.getTitle();
            case HIGH -> "⚠️ HIGH: Task Due Today - " + task.getTitle();
            case MEDIUM -> "📅 Upcoming Deadline - " + task.getTitle();
            case LOW -> "Task Notification - " + task.getTitle();
        };
    }

    /**
     * Format message as HTML email (production)
     */
    private String formatEmailHtml(String message, Task task) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif;">
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 5px;">
                        <h2 style="color: #d32f2f;">Task Alert</h2>
                        <pre style="white-space: pre-wrap;">%s</pre>
                        <hr>
                        <p style="color: #666; font-size: 12px;">
                            This is an automated alert from WallDot Builders Portal.
                            <br>Task ID: %s
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(message, task.getId());
    }
}
