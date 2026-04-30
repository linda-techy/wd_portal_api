package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.quotation.PublicQuotationResponse;
import com.wd.api.model.LeadQuotation;
import com.wd.api.service.PublicQuotationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Customer-facing token-gated quotation view (V77).
 *
 * <p>Permitted without auth in {@code SecurityConfig} — the UUID token in
 * the path is the bearer credential. Every successful hit appends a row to
 * {@code quotation_view_log} and, on the first hit, transitions the parent
 * status from SENT → VIEWED so the lead screen badge updates.
 *
 * <p>404 is returned for unknown tokens AND for tokens pointing to DRAFT
 * quotations — the customer must not be able to distinguish "no such link"
 * from "link points to an unsent draft."
 */
@RestController
@RequestMapping("/public/quotations")
public class PublicQuotationController {

    private static final Logger logger = LoggerFactory.getLogger(PublicQuotationController.class);

    private final PublicQuotationService publicQuotationService;

    public PublicQuotationController(PublicQuotationService publicQuotationService) {
        this.publicQuotationService = publicQuotationService;
    }

    /**
     * Resolve a share-link token to a sanitised customer-facing payload.
     * Captures the request's IP / UA / source query param into the view-log.
     *
     * @param token  the {@code public_view_token} from the share link
     * @param source optional channel hint — WHATSAPP_LINK, EMAIL_LINK, DIRECT
     */
    @GetMapping("/{token}")
    public ResponseEntity<?> getByToken(
            @PathVariable String token,
            @RequestParam(value = "source", required = false) String source,
            HttpServletRequest request) {
        UUID parsed;
        try {
            parsed = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            // Malformed UUID — treat as not-found, no information leak.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Quotation not found"));
        }

        try {
            Optional<LeadQuotation> hit = publicQuotationService.recordViewAndFetch(
                    parsed,
                    extractIp(request),
                    request.getHeader("User-Agent"),
                    sanitiseSource(source));

            return hit.<ResponseEntity<?>>map(q -> ResponseEntity.ok(PublicQuotationResponse.from(q)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Quotation not found")));
        } catch (Exception e) {
            // Never echo internals to the public endpoint — log and return
            // a generic 500 so a token holder can't probe the server.
            logger.error("Public quotation lookup failed for token {}", parsed, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Unable to load quotation"));
        }
    }

    /**
     * Pull the client IP, preferring the standard reverse-proxy headers
     * over {@code remoteAddr} so deployments behind nginx / Cloudflare
     * still log the real customer IP. We accept the first hop in
     * X-Forwarded-For — sufficient for analytics, not authoritative.
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }

    /**
     * Constrain the source channel hint to known values so a malicious
     * caller can't inject arbitrary text into the view-log column.
     */
    private String sanitiseSource(String raw) {
        if (raw == null) return null;
        return switch (raw.toUpperCase()) {
            case "WHATSAPP_LINK", "EMAIL_LINK", "DIRECT", "IN_APP" -> raw.toUpperCase();
            default -> null;
        };
    }
}
