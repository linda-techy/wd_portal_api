package com.wd.api.repository;

import com.wd.api.model.CustomerPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CustomerPasswordResetTokenRepository
        extends JpaRepository<CustomerPasswordResetToken, Long> {

    /** Find a valid (unused) token by email + hash for reset confirmation. */
    Optional<CustomerPasswordResetToken> findByEmailAndResetCodeAndUsedFalse(
            String email, String resetCode);

    /** Atomically mark token as used — returns 1 if successful, 0 if already used. */
    @Modifying
    @Query("UPDATE CustomerPasswordResetToken t SET t.used = true WHERE t.id = :id AND t.used = false")
    int markUsedById(@Param("id") Long id);

    /** Remove all existing tokens for an email before issuing a new one. */
    @Modifying
    @Query("DELETE FROM CustomerPasswordResetToken t WHERE t.email = :email")
    void deleteAllByEmail(@Param("email") String email);

    /** Nightly cleanup of expired tokens. */
    @Modifying
    @Query("DELETE FROM CustomerPasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
