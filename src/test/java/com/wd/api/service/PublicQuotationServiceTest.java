package com.wd.api.service;

import com.wd.api.model.LeadQuotation;
import com.wd.api.model.QuotationViewLog;
import com.wd.api.repository.LeadQuotationRepository;
import com.wd.api.repository.QuotationViewLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PublicQuotationService}.
 *
 * <p>Covers the security-sensitive code paths on the public token-gated
 * endpoint: unknown tokens must not leak, DRAFT tokens must masquerade as
 * not-found, the SENT → VIEWED transition must fire exactly once, and the
 * audit log must always be appended on a successful resolution.
 */
@ExtendWith(MockitoExtension.class)
class PublicQuotationServiceTest {

    @Mock private LeadQuotationRepository quotationRepository;
    @Mock private QuotationViewLogRepository viewLogRepository;

    private PublicQuotationService service;

    @BeforeEach
    void setUp() {
        service = new PublicQuotationService(quotationRepository, viewLogRepository);
    }

    @Test
    void recordViewAndFetch_returnsEmptyForUnknownToken() {
        UUID token = UUID.randomUUID();
        when(quotationRepository.findByPublicViewToken(token)).thenReturn(Optional.empty());

        Optional<LeadQuotation> hit = service.recordViewAndFetch(
                token, "10.0.0.1", "ua", "WHATSAPP_LINK");

        assertThat(hit).isEmpty();
        verify(viewLogRepository, never()).save(any());
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void recordViewAndFetch_treatsDraftAsNotFound() {
        // A leaked / mistakenly-shared token pointing to a DRAFT quote must
        // be opaque to the customer — same response shape as "unknown token".
        UUID token = UUID.randomUUID();
        LeadQuotation draft = quotation("DRAFT", token);
        when(quotationRepository.findByPublicViewToken(token)).thenReturn(Optional.of(draft));

        Optional<LeadQuotation> hit = service.recordViewAndFetch(token, "10.0.0.1", "ua", null);

        assertThat(hit).isEmpty();
        // Crucially, no view-log row written either — we don't want DRAFT
        // probes leaving forensic traces on the lead screen.
        verify(viewLogRepository, never()).save(any());
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void recordViewAndFetch_returnsEmptyForNullToken() {
        assertThat(service.recordViewAndFetch(null, "10.0.0.1", "ua", null)).isEmpty();
        verify(quotationRepository, never()).findByPublicViewToken(any());
    }

    @Test
    void recordViewAndFetch_appendsViewLogAndTransitionsSentToViewed() {
        UUID token = UUID.randomUUID();
        LeadQuotation quote = quotation("SENT", token);
        when(quotationRepository.findByPublicViewToken(token)).thenReturn(Optional.of(quote));

        Optional<LeadQuotation> hit = service.recordViewAndFetch(
                token, "203.0.113.42", "Mozilla/5.0", "WHATSAPP_LINK");

        assertThat(hit).contains(quote);

        // View-log row written with the captured request metadata.
        ArgumentCaptor<QuotationViewLog> logCap = ArgumentCaptor.forClass(QuotationViewLog.class);
        verify(viewLogRepository).save(logCap.capture());
        QuotationViewLog logged = logCap.getValue();
        assertThat(logged.getQuotation()).isSameAs(quote);
        assertThat(logged.getIpAddress()).isEqualTo("203.0.113.42");
        assertThat(logged.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(logged.getSource()).isEqualTo("WHATSAPP_LINK");

        // Status transitioned, viewed_at stamped, parent saved.
        assertThat(quote.getStatus()).isEqualTo("VIEWED");
        assertThat(quote.getViewedAt()).isNotNull();
        verify(quotationRepository).save(quote);
    }

    @Test
    void recordViewAndFetch_secondHitDoesNotResetStatusButAppendsLog() {
        // Already VIEWED — still record the hit, but don't redundantly save
        // the parent (it already has viewed_at and the right status).
        UUID token = UUID.randomUUID();
        LeadQuotation quote = quotation("VIEWED", token);
        quote.setViewedAt(LocalDateTime.of(2026, 4, 1, 10, 0));
        when(quotationRepository.findByPublicViewToken(token)).thenReturn(Optional.of(quote));

        Optional<LeadQuotation> hit = service.recordViewAndFetch(token, "10.0.0.1", "ua", null);

        assertThat(hit).contains(quote);
        verify(viewLogRepository).save(any(QuotationViewLog.class));
        // Status unchanged, no re-save of the parent.
        assertThat(quote.getStatus()).isEqualTo("VIEWED");
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void recordViewAndFetch_acceptedQuoteWithoutPriorViewStampsViewedAt() {
        // Edge case: ACCEPTED quote that was never observed via the public
        // endpoint (e.g. customer accepted in-person). First public hit
        // should still backfill viewed_at for analytics.
        UUID token = UUID.randomUUID();
        LeadQuotation quote = quotation("ACCEPTED", token);
        quote.setViewedAt(null);
        when(quotationRepository.findByPublicViewToken(token)).thenReturn(Optional.of(quote));

        service.recordViewAndFetch(token, "10.0.0.1", "ua", null);

        assertThat(quote.getStatus()).isEqualTo("ACCEPTED"); // not regressed
        assertThat(quote.getViewedAt()).isNotNull();
        verify(quotationRepository).save(quote);
    }

    @Test
    void regenerateToken_setsFreshUuidOnQuotation() {
        LeadQuotation quote = quotation("SENT", UUID.randomUUID());
        UUID original = quote.getPublicViewToken();
        when(quotationRepository.findById(42L)).thenReturn(Optional.of(quote));

        UUID fresh = service.regenerateToken(42L);

        assertThat(fresh).isNotEqualTo(original);
        assertThat(quote.getPublicViewToken()).isEqualTo(fresh);
        verify(quotationRepository).save(quote);
    }

    @Test
    void regenerateToken_failsForUnknownQuotation() {
        when(quotationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.regenerateToken(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quotation not found: 404");
    }

    @Test
    void viewCount_delegatesToRepository() {
        when(viewLogRepository.countByQuotationId(42L)).thenReturn(7L);
        assertThat(service.viewCount(42L)).isEqualTo(7L);
    }

    private static LeadQuotation quotation(String status, UUID token) {
        LeadQuotation q = new LeadQuotation();
        q.setId(7L);
        q.setStatus(status);
        q.setPublicViewToken(token);
        return q;
    }
}
