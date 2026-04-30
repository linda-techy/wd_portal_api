package com.wd.api.service;

import com.wd.api.model.LeadQuotation;
import com.wd.api.model.QuotationViewLog;
import com.wd.api.repository.LeadQuotationRepository;
import com.wd.api.repository.QuotationViewLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer-facing quotation lookup, gated by the {@code public_view_token}
 * UUID on {@link LeadQuotation}.
 *
 * <p>Two responsibilities:
 *
 * <ol>
 *   <li>Resolve a token to its quotation, returning empty for unknown tokens
 *       and for DRAFT quotations (those should never be customer-visible
 *       even with a leaked token).</li>
 *   <li>Append an audit row to {@code quotation_view_log} on every hit and
 *       transition the parent's status from SENT → VIEWED on the first one
 *       (also stamps {@code viewed_at} for the lead screen badge).</li>
 * </ol>
 *
 * <p>Token rotation lives here too — staff should be able to invalidate a
 * shared link if a customer claims it leaked, without affecting the parent
 * quotation's content.
 */
@Service
public class PublicQuotationService {

    private final LeadQuotationRepository quotationRepository;
    private final QuotationViewLogRepository viewLogRepository;

    public PublicQuotationService(
            LeadQuotationRepository quotationRepository,
            QuotationViewLogRepository viewLogRepository) {
        this.quotationRepository = quotationRepository;
        this.viewLogRepository = viewLogRepository;
    }

    /**
     * Look up a quotation by its public token and append a view-log row.
     * Returns empty when the token is unknown OR when it points to a DRAFT
     * — both render as 404 to the customer (no information leak about
     * which tokens exist for unsent quotations).
     */
    @Transactional
    public java.util.Optional<LeadQuotation> recordViewAndFetch(
            UUID token, String ipAddress, String userAgent, String source) {
        if (token == null) return java.util.Optional.empty();

        java.util.Optional<LeadQuotation> hit = quotationRepository.findByPublicViewToken(token);
        if (hit.isEmpty()) return java.util.Optional.empty();

        LeadQuotation quotation = hit.get();
        if ("DRAFT".equals(quotation.getStatus())) {
            // Token may exist for a draft (e.g. staff regenerated for testing),
            // but customer view must still be denied.
            return java.util.Optional.empty();
        }

        QuotationViewLog log = new QuotationViewLog();
        log.setQuotation(quotation);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setSource(source);
        viewLogRepository.save(log);

        // First view: SENT → VIEWED transition. Subsequent views just append.
        if ("SENT".equals(quotation.getStatus())) {
            quotation.setStatus("VIEWED");
            quotation.setViewedAt(LocalDateTime.now());
            quotationRepository.save(quotation);
        } else if (quotation.getViewedAt() == null) {
            // Already past SENT (e.g. ACCEPTED with no prior view tracking).
            // Stamp viewed_at so downstream analytics has the first-touch time.
            quotation.setViewedAt(LocalDateTime.now());
            quotationRepository.save(quotation);
        }

        return java.util.Optional.of(quotation);
    }

    /**
     * Generate (or regenerate) the public_view_token. Used both at "Send"
     * time when the quotation goes SENT, and on staff-triggered rotation
     * when a customer reports a leaked link. Returns the new token so the
     * caller can build a fresh share URL.
     */
    @Transactional
    public UUID regenerateToken(Long quotationId) {
        LeadQuotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Quotation not found: " + quotationId));
        UUID fresh = UUID.randomUUID();
        quotation.setPublicViewToken(fresh);
        quotationRepository.save(quotation);
        return fresh;
    }

    @Transactional(readOnly = true)
    public long viewCount(Long quotationId) {
        return viewLogRepository.countByQuotationId(quotationId);
    }
}
