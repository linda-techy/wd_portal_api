package com.wd.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_responses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyResponse {

    @Id
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "request_method", length = 8, nullable = false)
    private String requestMethod;

    @Column(name = "request_path", length = 255, nullable = false)
    private String requestPath;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT", nullable = false)
    private String responseBody;

    @Column(name = "response_content_type", length = 80, nullable = false)
    private String responseContentType;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
