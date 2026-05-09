package com.wd.api.repository;

import com.wd.api.model.IdempotencyResponse;
import com.wd.api.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyResponseRepositoryTest extends TestcontainersPostgresBase {

    @Autowired IdempotencyResponseRepository repo;

    @Test
    void findByKeyMethodPath_returnsRow() {
        IdempotencyResponse row = IdempotencyResponse.builder()
                .idempotencyKey("k-roundtrip")
                .requestMethod("POST")
                .requestPath("/api/site-reports")
                .responseStatus(201)
                .responseBody("{\"id\":1}")
                .responseContentType("application/json")
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        repo.save(row);

        Optional<IdempotencyResponse> hit = repo.findByIdempotencyKeyAndRequestMethodAndRequestPath(
                "k-roundtrip", "POST", "/api/site-reports");
        assertThat(hit).isPresent();
        assertThat(hit.get().getResponseStatus()).isEqualTo(201);
    }

    @Test
    void deleteByExpiresAtBefore_removesOnlyExpired() {
        LocalDateTime now = LocalDateTime.now();
        repo.save(IdempotencyResponse.builder().idempotencyKey("k-exp")
                .requestMethod("POST").requestPath("/api/site-reports")
                .responseStatus(201).responseBody("{}").responseContentType("application/json")
                .cachedAt(now.minusHours(48)).expiresAt(now.minusHours(24)).build());
        repo.save(IdempotencyResponse.builder().idempotencyKey("k-live")
                .requestMethod("POST").requestPath("/api/site-reports")
                .responseStatus(201).responseBody("{}").responseContentType("application/json")
                .cachedAt(now).expiresAt(now.plusHours(24)).build());

        repo.deleteByExpiresAtBefore(now);

        assertThat(repo.findById("k-exp")).isEmpty();
        assertThat(repo.findById("k-live")).isPresent();
    }
}
