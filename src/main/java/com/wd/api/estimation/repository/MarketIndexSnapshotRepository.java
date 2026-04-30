package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.MarketIndexSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MarketIndexSnapshotRepository extends JpaRepository<MarketIndexSnapshot, UUID> {

    /**
     * Returns the single is_active = true row. The partial unique index uq_estimation_market_index_active
     * (created in V90) guarantees at most one such row exists.
     */
    @Query("SELECT s FROM MarketIndexSnapshot s WHERE s.active = true")
    Optional<MarketIndexSnapshot> findActive();
}
