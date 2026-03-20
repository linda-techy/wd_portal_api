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
 * <p>The reset link points to the customer app ({@code https://app.walldotbuilders.com}).
 * The token is stored in the shared {@code customer_password_reset_tokens} table so
 * the customer app's existing reset-password flow (via the customer API) can validate it.
 *
 * <p>Only the SHA-256 hash of the raw token is persisted — the raw token travels
 * only inside the emailed link, never through any API response.
 */
@Service
public class CustomerPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(CustomerPasswordResetService.class);

    private static final int TOKEN_VALIDITY_MINUTES = 15;

    /**
     * Minimum seconds between successive reset emails for the same customer.
     * Prevents portal staff (or a compromised staff account) from spamming a customer's inbox.
     */
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private CustomerPasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Base URL of the customer-facing app.
     * Reset links in emails route here so the customer resets via the customer app.
     * Defaults to the production URL; override in local profile for dev testing.
     */
    @Value("${app.customer-app-base-url:https://app.walldotbuilders.com}")
    private String customerAppBaseUrl;

    /** Per-customer cooldown: customerId → last-send epoch-second. */
    private final Map<Long, Long> lastSentAt = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Send reset email (triggered by portal staff)
    // ─────────────────────────────────────────────────────────────────────────

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

        // Cooldown — prevent inbox flooding
        long nowEpoch = System.currentTimeMillis() / 1_000;
        Long last = lastSentAt.get(customerId);
        if (last != null && (nowEpoch - last) < RESEND_COOLDOWN_SECONDS) {
            long remaining = RESEND_COOLDOWN_SECONDS - (nowEpoch - last);
            throw new IllegalStateException(
                    "Password reset email already sent. Please wait " + remaining + " seconds before resending.");
        }

        // Generate a high-entropy raw token, store only its hash
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        String hashedToken = sha256Hex(rawToken);

        tokenRepository.deleteAllByEmail(email);
        tokenRepository.save(new CustomerPasswordResetToken(
                email, hashedToken, LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES)));

        // Reset link → customer app reset-password page (matches customer app deep-link route)
        String resetLink = customerAppBaseUrl
                + "/#/reset_password"
                + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);

        emailService.sendCustomerPasswordResetEmail(email, customer.getFirstName(), resetLink);

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
        lastSentAt.clear();
    }

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
