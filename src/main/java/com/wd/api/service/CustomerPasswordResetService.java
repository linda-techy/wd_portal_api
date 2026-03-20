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
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * <p>The reset link points to the portal API itself
 * ({@code GET /api/customers/reset-password-page}). The portal API serves a
 * self-contained HTML form; on submission the portal API validates the token
 * and updates the customer's password in the shared database.
 *
 * <p>Only the SHA-256 hash of the token is persisted. The raw token travels
 * only via the emailed link, never through any API response.
 */
@Service
public class CustomerPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(CustomerPasswordResetService.class);

    private static final int TOKEN_VALIDITY_MINUTES = 15;
    private static final long RESEND_COOLDOWN_SECONDS = 60;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private CustomerPasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Portal API's own public base URL. Set per environment in application-{profile}.yml. */
    @Value("${app.base-url:}")
    private String portalBaseUrl;

    /** Per-customer cooldown: customerId → last-send epoch-second. */
    private final Map<Long, Long> lastSentAt = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Send reset email (triggered by portal staff)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void sendPasswordResetEmail(Long customerId) {
        if (portalBaseUrl == null || portalBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "app.base-url is not configured for this environment");
        }

        CustomerUser customer = customerUserRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found: " + customerId));

        String email = customer.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer " + customerId + " has no email address on file");
        }

        // Cooldown check
        long nowEpoch = System.currentTimeMillis() / 1_000;
        Long last = lastSentAt.get(customerId);
        if (last != null && (nowEpoch - last) < RESEND_COOLDOWN_SECONDS) {
            long remaining = RESEND_COOLDOWN_SECONDS - (nowEpoch - last);
            throw new IllegalStateException(
                    "Password reset email already sent. Please wait " + remaining + " seconds before resending.");
        }

        // Generate token
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        String hashedToken = sha256Hex(rawToken);

        tokenRepository.deleteAllByEmail(email);
        tokenRepository.save(new CustomerPasswordResetToken(
                email, hashedToken, LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES)));

        // Build reset link — points to portal API's own reset page endpoint
        String resetLink = portalBaseUrl
                + "/api/customers/reset-password-page"
                + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);

        emailService.sendCustomerPasswordResetEmail(email, customer.getFirstName(), resetLink);

        lastSentAt.put(customerId, nowEpoch);
        log.info("Password reset email dispatched for customer {} ({})", customerId, email);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validate token and update password (submitted from portal API's HTML form)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(String email, String rawToken, String newPassword) {
        String hashedToken = sha256Hex(rawToken);

        CustomerPasswordResetToken token = tokenRepository
                .findByEmailAndResetCodeAndUsedFalse(email, hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or already used reset link."));

        if (token.isExpired()) {
            throw new IllegalArgumentException("Reset link has expired. Please request a new one.");
        }

        // Atomically mark as used — prevents replay attacks
        int updated = tokenRepository.markUsedById(token.getId());
        if (updated == 0) {
            throw new IllegalArgumentException("Reset link was already used.");
        }

        CustomerUser customer = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Customer account not found."));

        customer.setPassword(passwordEncoder.encode(newPassword));
        customerUserRepository.save(customer);

        log.info("Customer password reset completed for {}", email);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Maintenance
    // ─────────────────────────────────────────────────────────────────────────

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
