package com.wd.api.repository;

import com.wd.api.model.RetentionRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RetentionReleaseRepository extends JpaRepository<RetentionRelease, Long> {

    List<RetentionRelease> findByPaymentIdOrderByReleaseDateDesc(Long paymentId);

    @Query("SELECT COALESCE(SUM(r.releaseAmount), 0) FROM RetentionRelease r WHERE r.paymentId = :paymentId")
    BigDecimal getTotalReleasedAmount(@Param("paymentId") Long paymentId);
}
