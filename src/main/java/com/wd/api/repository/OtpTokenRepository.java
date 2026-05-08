package com.wd.api.repository;

import com.wd.api.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    @Query("""
        SELECT t FROM OtpToken t
        WHERE t.targetType = :targetType
          AND t.targetId = :targetId
          AND t.customerUserId = :customerUserId
          AND t.usedAt IS NULL
        ORDER BY t.createdAt DESC
        """)
    Optional<OtpToken> findActive(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId,
        @Param("customerUserId") Long customerUserId);

    @Query("""
        SELECT COUNT(t) FROM OtpToken t
        WHERE t.targetType = :targetType
          AND t.targetId = :targetId
          AND t.customerUserId = :customerUserId
          AND t.createdAt >= :since
        """)
    long countCreatedSince(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId,
        @Param("customerUserId") Long customerUserId,
        @Param("since") LocalDateTime since);
}
