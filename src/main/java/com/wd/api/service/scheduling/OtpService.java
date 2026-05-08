package com.wd.api.service.scheduling;

import com.wd.api.model.OtpToken;
import com.wd.api.model.ProjectVariation;
import com.wd.api.repository.OtpTokenRepository;
import com.wd.api.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final String TARGET_TYPE_CR_APPROVAL = "CR_APPROVAL";
    private static final int EXPIRY_MINUTES = 15;
    private static final int RATE_LIMIT_PER_DAY = 5;

    private final OtpTokenRepository repo;
    private final EmailService emailService;
    private final CustomerUserLookup customerUserLookup;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpTokenRepository repo,
                      EmailService emailService,
                      CustomerUserLookup customerUserLookup) {
        this.repo = repo;
        this.emailService = emailService;
        this.customerUserLookup = customerUserLookup;
    }

    @Transactional
    public OtpToken generateForCrApproval(Long crId, Long customerUserId, ProjectVariation cr) {
        // Rate limit: 5 OTPs / 24h / (target,customer)
        long recent = repo.countCreatedSince(
            TARGET_TYPE_CR_APPROVAL, crId, customerUserId,
            LocalDateTime.now().minusHours(24));
        if (recent >= RATE_LIMIT_PER_DAY) {
            throw new OtpRateLimitException(
                "OTP requests rate-limited: max " + RATE_LIMIT_PER_DAY + " per 24h",
                3600L /* one hour retry hint */);
        }

        String plaintext = sixDigitCode();
        OtpToken token = new OtpToken();
        token.setCodeHash(sha256(plaintext));
        token.setTargetType(TARGET_TYPE_CR_APPROVAL);
        token.setTargetId(crId);
        token.setCustomerUserId(customerUserId);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        token = repo.save(token);

        CustomerUserContact contact = customerUserLookup.contactFor(customerUserId);
        emailService.sendCrApprovalOtp(contact.email(), contact.name(),
                                        plaintext, cr, EXPIRY_MINUTES);
        log.info("Generated CR approval OTP token id={} crId={} customerUserId={}",
                 token.getId(), crId, customerUserId);
        return token;
    }

    @Transactional
    public OtpVerifyOutcome verify(String targetType, Long targetId,
                                   Long customerUserId, String plaintextCode) {
        Optional<OtpToken> opt = repo.findActive(targetType, targetId, customerUserId);
        if (opt.isEmpty()) return new OtpVerifyOutcome(OtpVerifyResult.NO_ACTIVE_TOKEN, null);

        OtpToken t = opt.get();
        // Capture the hash BEFORE any state change. After we set used_at the
        // row drops out of findActive, so a later hashFor() lookup would race
        // with concurrent submits. Returning the hash here is atomic w.r.t.
        // the row we actually checked.
        String hashAtCheckTime = t.getCodeHash();

        if (t.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new OtpVerifyOutcome(OtpVerifyResult.EXPIRED, hashAtCheckTime);
        }
        if (t.getAttempts() >= t.getMaxAttempts()) {
            return new OtpVerifyOutcome(OtpVerifyResult.MAX_ATTEMPTS, hashAtCheckTime);
        }

        if (!sha256(plaintextCode).equals(hashAtCheckTime)) {
            t.setAttempts(t.getAttempts() + 1);
            repo.save(t);
            OtpVerifyResult result = t.getAttempts() >= t.getMaxAttempts()
                ? OtpVerifyResult.MAX_ATTEMPTS
                : OtpVerifyResult.WRONG_CODE;
            return new OtpVerifyOutcome(result, hashAtCheckTime);
        }

        t.setUsedAt(LocalDateTime.now());
        repo.save(t);
        return new OtpVerifyOutcome(OtpVerifyResult.VERIFIED, hashAtCheckTime);
    }

    // ── internals ─────────────────────────────────────────────────

    String sixDigitCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
