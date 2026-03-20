package com.wd.api.service;

import com.wd.api.model.CustomerPasswordResetToken;
import com.wd.api.model.CustomerUser;
import com.wd.api.repository.CustomerPasswordResetTokenRepository;
import com.wd.api.repository.CustomerUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Portal-side service that generates a password-reset link for a CustomerUser
 * and emails it to them.
 *
 * <p>The token is stored in the shared {@code customer_password_reset_tokens}
 * table so the customer API's own {@code /auth/reset-password} endpoint can
 * validate it when the customer clicks the link in their email.
 *
 * <p>Only the SHA-256 hash of the token is persisted; the raw token travels
 * only via the emailed link — never through the API response.
 */
@Service
public class CustomerPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(CustomerPasswordResetService.class);

    /** Token validity window. */
    private static final int TOKEN_VALIDITY_MINUTES = 15;

    /**
     * Minimum seconds between successive reset emails for the same customer.
     * Prevents portal staff (or an attacker with staff access) from bombing a customer's inbox.
     */
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private CustomerPasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Base URL of the customer-facing app — controls where the reset link points.
     * Set in application-{local|staging|production}.yml.
     */
    @Value("${app.customer-portal-base-url:}")
    private String customerPortalBaseUrl;

    /** Per-customer cooldown tracking (customerId → last-send epoch-second). */
    private final Map<Long, Long> lastSentAt = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a password-reset token for the customer identified by {@code customerId}
     * and sends a branded reset email.
     *
     * @param customerId  DB id of the CustomerUser
     * @throws IllegalArgumentException  if customer not found or has no email
     * @throws IllegalStateException     if a reset email was sent too recently
     */
    @Transactional
    public void sendPasswordResetEmail(Long customerId) {

        CustomerUser customer = customerUserRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found: " + customerId));

        String email = customer.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer " + customerId + " has no email address on file");
        }

        if (customerPortalBaseUrl == null || customerPortalBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "app.customer-portal-base-url is not configured for this environment");
        }

        // ── Cooldown check ────────────────────────────────────────────────────
        long nowEpoch = System.currentTimeMillis() / 1_000;
        Long last = lastSentAt.get(customerId);
        if (last != null && (nowEpoch - last) < RESEND_COOLDOWN_SECONDS) {
            long remaining = RESEND_COOLDOWN_SECONDS - (nowEpoch - last);
            throw new IllegalStateException(
                    "Password reset email already sent. Please wait " + remaining + " seconds before resending.");
        }

        // ── Token generation ──────────────────────────────────────────────────
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        String hashedToken = sha256Hex(rawToken);

        // Invalidate any previous unused tokens for this email
        tokenRepository.deleteAllByEmail(email);

        CustomerPasswordResetToken token = new CustomerPasswordResetToken(
                email,
                hashedToken,
                LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
        tokenRepository.save(token);

        // ── Build reset link ──────────────────────────────────────────────────
        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String resetLink = customerPortalBaseUrl
                + "/#/reset_password?token=" + encodedToken
                + "&email=" + encodedEmail;

        // ── Send email (async) ────────────────────────────────────────────────
        emailService.sendCustomerPasswordResetEmail(email, customer.getFirstName(), resetLink);

        // ── Record send time ──────────────────────────────────────────────────
        lastSentAt.put(customerId, nowEpoch);

        log.info("Password reset email dispatched for customer {} ({})", customerId, email);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Maintenance
    // ─────────────────────────────────────────────────────────────────────────

    /** Nightly cleanup — remove expired tokens at 2:05 AM IST. */
    @Scheduled(cron = "0 5 2 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = tokenRepository.deleteExpiredBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired customer password reset tokens", deleted);
        }
        // Also clear the in-memory cooldown map periodically
        lastSentAt.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
