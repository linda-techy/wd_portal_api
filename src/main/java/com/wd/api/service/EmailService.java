package com.wd.api.service;

import com.wd.api.model.Lead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
     * Sends a password reset email to a partnership/referral partner.
     * Reset link points to the website's partnerships login page with reset mode.
     */
    @Async
    public void sendPartnerPasswordResetEmail(String to, String name, String resetLink) {
        String subject = "Reset Your Walldot Partner Account Password";
        String body = String.format("""
                Dear %s,

                A password reset was requested for your Walldot Partner / Referral account.

                Click the link below to reset your password. This link is valid for 15 minutes.

                Reset Password: %s

                If you did not request this, you can safely ignore this email. Your password will not change.

                Need help? Contact us at %s

                Best regards,
                The Walldot Team
                """, name, resetLink, adminEmail);

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Partner password reset email sent to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send partner password reset email to {}. Simulation logged.", to, e);
                logEmailSimulation(to, subject, body);
            }
        } else {
            logEmailSimulation(to, subject, body);
        }
    }

    /**
     * Sends a partner application approval email.
     * Called when a portal admin approves a partnership application.
     */
    @Async
    public void sendPartnerApprovalEmail(String to, String name, String partnershipType) {
        String subject = "Your Walldot Partner Application Has Been Approved!";
        String body = String.format("""
                Dear %s,

                Congratulations! Your application to join the Walldot Builders Partner / Referral Program has been approved.

                Partnership Type: %s

                You can now log in to your partner dashboard to track your referrals and commissions.

                Partner Dashboard: https://walldotbuilders.com/partnerships/dashboard

                Welcome to the Walldot Partner Network! If you have any questions, reach out to us at %s.

                Best regards,
                The Walldot Team
                """, name, partnershipType, adminEmail);

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Partner approval email sent to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send partner approval email to {}. Simulation logged.", to, e);
                logEmailSimulation(to, subject, body);
            }
        } else {
            logEmailSimulation(to, subject, body);
        }
    }

    /**
     * Sends a notification email to a referred friend when someone submits a referral on their behalf.
     * Does NOT say "thank you for contacting us" — they didn't initiate contact.
     * Informs them that Walldot's team will reach out and they should expect a call.
     */
    @Async
    public void sendReferralLeadNotificationEmail(Lead lead) {
        if (lead.getEmail() == null || lead.getEmail().isEmpty())
            return;

        String subject = "Walldot Builders Will Be in Touch With You";
        String body = String.format("""
                Dear %s,

                Someone who cares about your future home has referred you to Walldot Builders!

                We have received your details for a %s project and our team will reach out to you shortly to understand your requirements and answer any questions you may have.

                There is no obligation — we are happy to have a friendly conversation about your plans.

                If you have any questions in the meantime, feel free to contact us:
                  Phone: +91 9074 9548 74
                  Email: hello@walldotbuilders.com
                  Website: https://walldotbuilders.com

                Looking forward to speaking with you!

                Best regards,
                The Walldot Builders Team
                Thrissur, Kerala
                """,
                lead.getName(),
                lead.getProjectType() != null && !lead.getProjectType().isEmpty()
                        ? lead.getProjectType() : "construction");

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(lead.getEmail());
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Referral lead notification email sent to {}", lead.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send referral lead notification email to {}. Simulation logged.", lead.getEmail(), e);
                logEmailSimulation(lead.getEmail(), subject, body);
            }
        } else {
            logEmailSimulation(lead.getEmail(), subject, body);
        }
    }

    /**
     * Sends an invite email to a person who was referred by someone.
     * They click the setup link to set a password and login to track their inquiry status.
     */
    @Async
    public void sendReferredClientInviteEmail(String to, String name, String referrerName, String setupLink) {
        String subject = "You've been referred to Walldot Builders — Track Your Inquiry";
        String body = String.format("""
                Dear %s,

                Great news! %s has referred you to Walldot Builders for your construction project.

                Your inquiry has been received and our team will reach out to you shortly.

                You can also track the progress of your inquiry online by setting up your account:

                %s

                This link is valid for 24 hours. Once you set your password, you can log in anytime at:
                https://walldotbuilders.com/partnerships/login

                If you have any questions, feel free to contact us at info@walldotbuilders.com or call +91 9207 700 700.

                Best regards,
                The Walldot Builders Team
                """, name, referrerName, setupLink);

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Referred client invite email sent to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send referred client invite email to {}. Simulation logged.", to, e);
                logEmailSimulation(to, subject, body);
            }
        } else {
            logEmailSimulation(to, subject, body);
        }
    }

    /**
     * Sends a partner application rejection email.
     * Called when a portal admin rejects a partnership application.
     */
    @Async
    public void sendPartnerRejectionEmail(String to, String name) {
        String subject = "Update on Your Walldot Partner Application";
        String body = String.format("""
                Dear %s,

                Thank you for your interest in joining the Walldot Builders Partner / Referral Program.

                After careful review, we are unable to approve your application at this time.

                If you believe this decision was made in error or would like to discuss further, please contact us at %s.

                We appreciate your interest in Walldot Builders and wish you all the best.

                Best regards,
                The Walldot Team
                """, name, adminEmail);

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Partner rejection email sent to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send partner rejection email to {}. Simulation logged.", to, e);
                logEmailSimulation(to, subject, body);
            }
        } else {
            logEmailSimulation(to, subject, body);
        }
    }

    // ── Portal Password Reset ─────────────────────────────────────────────────

    /**
     * Sends an HTML password-reset email to a portal user.
     *
     * @param to        Recipient email address
     * @param name      Recipient first name (for personalisation)
     * @param resetLink Full URL including token, e.g. https://portal.../reset-password?token=XXX
     */
    @Async
    public void sendPortalPasswordResetEmail(String to, String name, String resetLink) {
        String subject = "Reset Your Walldot Portal Password";
        String html = buildPortalPasswordResetHtml(name, resetLink);

        if (emailEnabled && mailSender != null) {
            try {
                jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(fromEmail, "Walldot Builders Portal");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true); // true = HTML

                mailSender.send(mimeMessage);
                logger.info("Portal password reset email sent to {}", to);
            } catch (Exception e) {
                logger.error("Failed to send portal password reset email to {}", to, e);
                logEmailSimulation(to, subject, "[HTML email — body omitted]");
            }
        } else {
            logger.info("[EMAIL SIMULATION] Portal password reset link for {}: {}", to, resetLink);
            logEmailSimulation(to, subject, "[HTML email — body omitted]");
        }
    }

    private String buildPortalPasswordResetHtml(String name, String resetLink) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Reset Your Password</title>
            </head>
            <body style="margin:0;padding:0;background:#F4F5F7;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F4F5F7;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="580" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:12px;overflow:hidden;
                                  box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#E84545,#2A2A3A);
                                   padding:36px 40px;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:24px;
                                     font-weight:700;letter-spacing:-0.5px;">
                            Walldot Builders
                          </h1>
                          <p style="margin:6px 0 0;color:rgba(255,255,255,0.75);font-size:13px;">
                            Portal Administration
                          </p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:40px 48px;">
                          <h2 style="margin:0 0 16px;color:#2A2A3A;font-size:22px;font-weight:700;">
                            Reset Your Password
                          </h2>
                          <p style="margin:0 0 12px;color:#555;font-size:15px;line-height:1.6;">
                            Hi <strong>%s</strong>,
                          </p>
                          <p style="margin:0 0 24px;color:#555;font-size:15px;line-height:1.6;">
                            We received a request to reset the password for your Walldot Builders
                            portal account. Click the button below to choose a new password.
                          </p>

                          <!-- CTA Button -->
                          <table cellpadding="0" cellspacing="0" style="margin:0 auto 28px;">
                            <tr>
                              <td align="center"
                                  style="background:#E84545;border-radius:8px;">
                                <a href="%s"
                                   style="display:inline-block;padding:14px 36px;
                                          color:#ffffff;font-size:15px;font-weight:600;
                                          text-decoration:none;letter-spacing:0.3px;">
                                  Reset Password
                                </a>
                              </td>
                            </tr>
                          </table>

                          <!-- Expiry notice -->
                          <div style="background:#FFF8F0;border:1px solid #FFD9A0;
                                      border-radius:8px;padding:14px 18px;margin-bottom:24px;">
                            <p style="margin:0;color:#7A4A00;font-size:13px;line-height:1.5;">
                              ⏱ <strong>This link expires in 30 minutes.</strong>
                              If it expires, you can request a new one from the login screen.
                            </p>
                          </div>

                          <!-- Fallback link -->
                          <p style="margin:0 0 8px;color:#888;font-size:13px;">
                            If the button doesn't work, copy and paste this URL into your browser:
                          </p>
                          <p style="margin:0 0 24px;font-size:12px;
                                    word-break:break-all;color:#E84545;">
                            %s
                          </p>

                          <!-- Security notice -->
                          <div style="border-top:1px solid #EEEEEE;padding-top:20px;">
                            <p style="margin:0;color:#AAA;font-size:12px;line-height:1.6;">
                              🔒 If you did not request this password reset, please ignore this
                              email. Your password will remain unchanged. For security concerns,
                              contact your system administrator.
                            </p>
                          </div>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#F8F9FA;padding:20px 48px;
                                   border-top:1px solid #EEEEEE;text-align:center;">
                          <p style="margin:0;color:#AAA;font-size:12px;">
                            © 2025 Walldot Builders LLP · Kerala, India
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(name, resetLink, resetLink);
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
