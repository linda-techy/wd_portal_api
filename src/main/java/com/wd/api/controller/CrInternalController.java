package com.wd.api.controller;

import com.wd.api.model.ProjectVariation;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.security.InternalHmacVerifier;
import com.wd.api.service.ProjectVariationService;
import com.wd.api.service.scheduling.OtpRateLimitException;
import com.wd.api.service.scheduling.OtpService;
import com.wd.api.service.scheduling.OtpVerifyOutcome;
import com.wd.api.service.scheduling.OtpVerifyResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Internal endpoints called by customer-API after the customer submits an
 * OTP request or an OTP verification. HMAC-signed (header X-Portal-Signature).
 */
@RestController
@RequestMapping("/internal")
public class CrInternalController {

    private static final Logger log = LoggerFactory.getLogger(CrInternalController.class);
    private static final String SIG_HEADER = "X-Portal-Signature";
    private static final String TARGET_TYPE = "CR_APPROVAL";

    private final InternalHmacVerifier hmac;
    private final OtpService otpService;
    private final ProjectVariationService projectVariationService;
    private final ProjectVariationRepository projectVariationRepository;

    public CrInternalController(InternalHmacVerifier hmac,
                                 OtpService otpService,
                                 ProjectVariationService projectVariationService,
                                 ProjectVariationRepository projectVariationRepository) {
        this.hmac = hmac;
        this.otpService = otpService;
        this.projectVariationService = projectVariationService;
        this.projectVariationRepository = projectVariationRepository;
    }

    public record RequestOtpBody(Long crId, Long customerUserId) {}
    public record ApproveBody(Long crId, Long customerUserId, String otpCode, String customerIp) {}
    public record CrLookupBody(Long crId) {}

    @PostMapping("/cr-request-otp")
    public ResponseEntity<Map<String, Object>> requestOtp(@RequestBody String rawBody,
                                                           HttpServletRequest request) {
        if (!hmac.verify(rawBody, request.getHeader(SIG_HEADER))) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_SIGNATURE"));
        }
        RequestOtpBody body = parse(rawBody, RequestOtpBody.class);
        ProjectVariation cr = projectVariationRepository.findById(body.crId())
            .orElseThrow(() -> new NoSuchElementException("CR not found: " + body.crId()));
        try {
            otpService.generateForCrApproval(body.crId(), body.customerUserId(), cr);
            return ResponseEntity.ok(Map.of("status", "SENT"));
        } catch (OtpRateLimitException e) {
            log.warn("CR {} customer {} rate-limited: {}", body.crId(), body.customerUserId(), e.getMessage());
            return ResponseEntity.status(429).body(Map.of(
                "error", "RATE_LIMITED",
                "retryAfterSeconds", e.getRetryAfterSeconds()));
        }
    }

    @PostMapping("/cr-approve")
    public ResponseEntity<Map<String, Object>> approve(@RequestBody String rawBody,
                                                        HttpServletRequest request) {
        if (!hmac.verify(rawBody, request.getHeader(SIG_HEADER))) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_SIGNATURE"));
        }
        ApproveBody body = parse(rawBody, ApproveBody.class);

        // verify() returns both the result enum AND the matched hash atomically,
        // so there's no race window between "look up active token" and "consume
        // active token" — the hash is captured before any state mutation.
        OtpVerifyOutcome outcome = otpService.verify(
            TARGET_TYPE, body.crId(), body.customerUserId(), body.otpCode());

        if (outcome.result() == OtpVerifyResult.VERIFIED) {
            projectVariationService.approveByCustomer(
                body.crId(), body.customerUserId(), outcome.hash(), body.customerIp());
        }
        return ResponseEntity.ok(Map.of("status", outcome.result().name()));
    }

    /**
     * Resolve a CR id to its owning project id. Used by customer-API to enforce
     * that the authenticated customer owns the project of the CR they're trying
     * to act on, before forwarding /cr-request-otp or /cr-approve calls. Without
     * this lookup, customer-API would have to either reach into portal's
     * project_variations table directly (cross-DB read) or trust the client.
     */
    @PostMapping("/cr-project-id")
    public ResponseEntity<Map<String, Object>> getCrProjectId(@RequestBody String rawBody,
                                                               HttpServletRequest request) {
        if (!hmac.verify(rawBody, request.getHeader(SIG_HEADER))) {
            return ResponseEntity.status(401).body(Map.of("error", "INVALID_SIGNATURE"));
        }
        CrLookupBody body = parse(rawBody, CrLookupBody.class);
        ProjectVariation cr = projectVariationRepository.findById(body.crId())
            .orElseThrow(() -> new NoSuchElementException("CR not found: " + body.crId()));
        if (cr.getProject() == null) {
            // Should never happen given project_id is NOT NULL on project_variations,
            // but guard anyway so we don't NPE in service code on a corrupt row.
            return ResponseEntity.status(404).body(Map.of("error", "CR_HAS_NO_PROJECT"));
        }
        return ResponseEntity.ok(Map.of(
            "crId", cr.getId(),
            "projectId", cr.getProject().getId()));
    }

    private static <T> T parse(String body, Class<T> type) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }
}
