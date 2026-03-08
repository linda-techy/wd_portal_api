package com.wd.api.repository;

import com.wd.api.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUser_Id(Long userId);

    /**
     * Bulk-delete all tokens that are expired OR revoked.
     * Called nightly by the cleanup scheduler to prevent unbounded table growth.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now OR rt.revoked = true")
    int deleteExpiredAndRevoked(@Param("now") LocalDateTime now);
}