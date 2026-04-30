package com.wd.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Append-only audit row recording each customer-side hit on the public
 * token-gated quotation endpoint (V77).
 *
 * <p>Powers the "viewed 4 times in the last 24 h" badge on the lead screen
 * and the first-view timestamp that {@code LeadQuotation.viewedAt} should
 * be backfilled from. Source is optional and captured from a {@code ?source=}
 * query param so we can attribute the share channel (WhatsApp vs email vs
 * direct copy-paste).
 */
@Entity
@Table(name = "quotation_view_log",
        indexes = @Index(
                name = "idx_quotation_view_log_quotation_at",
                columnList = "quotation_id, viewed_at DESC"))
public class QuotationViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private LeadQuotation quotation;

    @Column(name = "viewed_at", nullable = false, updatable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(length = 20)
    private String source;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) viewedAt = LocalDateTime.now();
    }

    public QuotationViewLog() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LeadQuotation getQuotation() { return quotation; }
    public void setQuotation(LeadQuotation quotation) { this.quotation = quotation; }

    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
