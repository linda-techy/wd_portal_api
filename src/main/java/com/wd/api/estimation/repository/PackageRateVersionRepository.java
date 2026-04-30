package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.ProjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PackageRateVersionRepository extends JpaRepository<PackageRateVersion, UUID> {

    /**
     * Returns the rate version that is active for the given package + project type at the given date.
     * "Active" means effective_from <= asOf AND (effective_to IS NULL OR effective_to > asOf).
     * If multiple rows match (overlapping windows — should not happen but is defensive), returns the one
     * with the latest effective_from.
     */
    @Query("""
        SELECT rv FROM PackageRateVersion rv
         WHERE rv.packageId    = :packageId
           AND rv.projectType  = :projectType
           AND rv.effectiveFrom <= :asOf
           AND (rv.effectiveTo IS NULL OR rv.effectiveTo > :asOf)
         ORDER BY rv.effectiveFrom DESC
        """)
    Optional<PackageRateVersion> findActive(
            @Param("packageId")   UUID packageId,
            @Param("projectType") ProjectType projectType,
            @Param("asOf")        LocalDate asOf);
}
