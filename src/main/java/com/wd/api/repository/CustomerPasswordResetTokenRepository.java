package com.wd.api.repository;

import com.wd.api.model.CustomerPasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CustomerPasswordResetTokenRepository
        extends JpaRepository<CustomerPasswordResetToken, Long> {

    /** Remove all existing (used or unused) tokens for an email before issuing a new one. */
    @Modifying
    @Query("DELETE FROM CustomerPasswordResetToken t WHERE t.email = :email")
    void deleteAllByEmail(@Param("email") String email);

    /** Remove tokens that have already expired (nightly cleanup). */
    @Modifying
    @Query("DELETE FROM CustomerPasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
