package com.wd.api.controller;

import com.wd.api.model.ProjectVariation;
import com.wd.api.repository.ProjectVariationRepository;
import com.wd.api.security.InternalHmacVerifier;
import com.wd.api.service.ProjectVariationService;
import com.wd.api.service.scheduling.OtpRateLimitException;
import com.wd.api.service.scheduling.OtpService;
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

        // Capture hash BEFORE verify(), since verify() consumes the active token.
        String hash = otpService.hashFor(TARGET_TYPE, body.crId(), body.customerUserId());
        OtpVerifyResult result = otpService.verify(
            TARGET_TYPE, body.crId(), body.customerUserId(), body.otpCode());

        if (result == OtpVerifyResult.VERIFIED) {
            projectVariationService.approveByCustomer(
                body.crId(), body.customerUserId(), hash, body.customerIp());
        }
        return ResponseEntity.ok(Map.of("status", result.name()));
    }

    private static <T> T parse(String body, Class<T> type) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
        }
    }
}
