package com.wd.api.service;

import com.wd.api.model.Lead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.wd.api.model.Task;
import com.wd.api.model.TaskAlert;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:noreply@walldot.com}")
    private String fromEmail;

    /** Alert recipient for HOT lead and system-level notifications. Configured per-environment. */
    @Value("${app.admin.email:info@walldotbuilders.com}")
    private String adminEmail;

    /**
     * Sends a welcome email to the newly created user with their credentials.
     * If email is disabled or mocked, it logs the content instead.
     * 
     * @param to       Recipient email
     * @param name     Recipient name
     * @param password Generated password
     */
    @Async
    public void sendWelcomeEmail(String to, String name, String password) {
        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject("Welcome to Walldot Portal - Account Created");
                message.setText(buildWelcomeEmailBody(name, to, password));

                mailSender.send(message);
                logger.info("Welcome email sent successfully to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send welcome email to {}. Falling back to simulation log.", to, e);
                logEmailSimulation(to, name, password);
            }
        } else {
            logEmailSimulation(to, name, password);
        }
    }

    private String buildWelcomeEmailBody(String name, String username, String password) {
        return String.format("""
                Dear %s,

                Welcome to Walldot! Your customer account has been successfully created.

                Here are your login credentials:
                Username: %s
                Password: %s

                Please log in to the portal and change your password immediately.

                Best regards,
                The Walldot Team
                """, name, username, password);
    }

    @Async
    public void sendLeadWelcomeEmail(Lead lead) {
        // Only send if email is valid
        if (lead.getEmail() == null || lead.getEmail().isEmpty())
            return;

        String subject = "Welcome to Walldot - We Received Your Inquiry";
        String body = String.format("""
                Dear %s,

                Thank you for contacting Walldot! We have received your inquiry regarding %s.

                Our team has been assigned and will reach out to you shortly to discuss your requirements.

                Project ID: %s

                Best regards,
                The Walldot Team
                """, lead.getName(), lead.getProjectType() != null ? lead.getProjectType() : "your project",
                lead.getId());

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(lead.getEmail());
                message.setSubject(subject);
                message.setText(body);

                mailSender.send(message);
                logger.info("Lead welcome email sent successfully to {}", lead.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send lead welcome email to {}. Falling back to simulation.", lead.getEmail(),
                        e);
                logEmailSimulation(lead.getEmail(), subject, body);
            }
        } else {
            logEmailSimulation(lead.getEmail(), subject, body);
        }
    }

    @Async
    public void sendLeadStatusUpdateEmail(Lead lead, String oldStatus, String newStatus) {
        if (lead.getEmail() == null || lead.getEmail().isEmpty())
            return;

        String subject = "Update on Your Walldot Project Inquiry";
        String body = String.format("""
                Dear %s,

                The status of your inquiry has been updated.

                Previous Status: %s
                New Status: %s

                We are making progress!

                Best regards,
                The Walldot Team
                """, lead.getName(), oldStatus, newStatus);

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(lead.getEmail());
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Lead status update email sent to {}", lead.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send status update to {}. Simulation logged.", lead.getEmail(), e);
                logEmailSimulation(lead.getEmail(), subject, body);
            }
        } else {
            logEmailSimulation(lead.getEmail(), subject, body);
        }
    }

    @Async
    public void sendAdminScoreAlert(Lead lead) {
        String subject = "HOT LEAD ALERT: " + lead.getName();
        String body = String.format("""
                Admin,

                A Lead has been identified as HOT!

                Name: %s
                Score: %d
                Category: %s
                Potential Value: %s

                Please assign a senior representative immediately.

                Link: /leads/%d
                """, lead.getName(), lead.getScore(), lead.getScoreCategory(),
                lead.getBudget() != null ? lead.getBudget().toString() : "N/A", lead.getId());

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(adminEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Admin Hot Lead Alert sent for {}", lead.getId());
            } catch (Exception e) {
                logEmailSimulation(adminEmail, subject, body);
            }
        } else {
            logEmailSimulation(adminEmail, subject, body);
        }
    }

    @Async
    public void sendTaskAlert(String to, Task task, String messageContent, TaskAlert.AlertSeverity severity) {
        String subject = String.format("ACTION REQUIRED: %s Task Alert - %s", severity, task.getTitle());

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(messageContent);
                mailSender.send(message);
                logger.info("Task alert email sent to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send task alert to {}. Simulation logged.", to, e);
                logEmailSimulation(to, subject, messageContent);
            }
        } else {
            logEmailSimulation(to, subject, messageContent);
        }
    }

    /**
     * Sends a password-reset email to a CustomerUser.
     * Called by portal staff when they trigger "Send Reset Password Email" from the portal app.
     * The reset link routes to the customer-facing app (not the portal).
     *
     * @param to        Customer email address
     * @param firstName Customer first name (for personalisation)
     * @param resetLink Full URL with encoded token and email query params
     */
    @Async
    public void sendCustomerPasswordResetEmail(String to, String firstName, String resetLink) {
        String subject = "Reset Your Walldot Customer Password";
        String body = buildCustomerPasswordResetEmailBody(firstName, resetLink);

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Customer password reset email sent to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send customer password reset email to {}. Simulation logged.", to, e);
                logEmailSimulation(to, subject, body);
            }
        } else {
            logEmailSimulation(to, subject, body);
        }
    }

    private String buildCustomerPasswordResetEmailBody(String firstName, String resetLink) {
        return String.format("""
                Dear %s,

                A password reset has been requested for your Walldot customer account by the Walldot team.

                Click the link below to reset your password. This link is valid for 15 minutes.

                Reset Password: %s

                If you did not request this, please contact us immediately at %s.

                Best regards,
                The Walldot Team
                """, firstName, resetLink, adminEmail);
    }

    /**
     * Log that an email would have been sent (simulation mode).
     * NOTE: Body is intentionally NOT logged to avoid leaking passwords or PII into logs.
     * Only recipient and subject are safe to log.
     */
    private void logEmailSimulation(String to, String subject, String body) {
        logger.info("[EMAIL SIMULATION] TO: {} | SUBJECT: {}", to, subject);
        // body omitted intentionally — may contain plaintext passwords or customer PII
    }
}
