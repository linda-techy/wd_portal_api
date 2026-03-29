package com.wd.api.repository;

import com.wd.api.model.PortalPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PortalPasswordResetTokenRepository extends JpaRepository<PortalPasswordResetToken, Long> {

    /** Find a valid (unused) token record by its hash. */
    Optional<PortalPasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    /** Delete all expired tokens (cleanup job). */
    @Modifying
    @Query("DELETE FROM PortalPasswordResetToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /** Invalidate all existing unused tokens for an email (before issuing a new one). */
    @Modifying
    @Query("UPDATE PortalPasswordResetToken t SET t.used = true WHERE t.email = :email AND t.used = false")
    int invalidateTokensForEmail(@Param("email") String email);
}
