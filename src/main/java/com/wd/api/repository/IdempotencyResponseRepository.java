package com.wd.api.repository;

import com.wd.api.model.IdempotencyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyResponseRepository extends JpaRepository<IdempotencyResponse, String> {

    Optional<IdempotencyResponse> findByIdempotencyKeyAndRequestMethodAndRequestPath(
            String idempotencyKey, String requestMethod, String requestPath);

    @Modifying
    @Transactional
    @Query("DELETE FROM IdempotencyResponse r WHERE r.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(LocalDateTime cutoff);
}
