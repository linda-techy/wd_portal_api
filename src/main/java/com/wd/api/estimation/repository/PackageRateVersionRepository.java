package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.PackageRateVersion;
import com.wd.api.estimation.domain.enums.ProjectType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackageRateVersionRepository extends JpaRepository<PackageRateVersion, UUID> {

    /**
     * Internal: returns up to N rate-version candidates active at asOf, latest first.
     * Use {@link #findActive(UUID, ProjectType, LocalDate)} from callers.
     *
     * <p>Defensive: if two rate versions have overlapping windows (should not happen,
     * but the schema doesn't physically prevent it), returning a List + paging means we
     * never throw IncorrectResultSizeDataAccessException — we just return the newest.
     */
    @Query("""
        SELECT rv FROM PackageRateVersion rv
         WHERE rv.packageId    = :packageId
           AND rv.projectType  = :projectType
           AND rv.effectiveFrom <= :asOf
           AND (rv.effectiveTo IS NULL OR rv.effectiveTo > :asOf)
         ORDER BY rv.effectiveFrom DESC
        """)
    List<PackageRateVersion> findActiveCandidates(
            @Param("packageId")   UUID packageId,
            @Param("projectType") ProjectType projectType,
            @Param("asOf")        LocalDate asOf,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Returns the rate version that is active for the given package + project type at the given date.
     * "Active" means effective_from &lt;= asOf AND (effective_to IS NULL OR effective_to &gt; asOf).
     * If multiple rows match (overlapping windows — should not happen but is defensive), returns the one
     * with the latest effective_from.
     */
    default Optional<PackageRateVersion> findActive(UUID packageId, ProjectType projectType, LocalDate asOf) {
        return findActiveCandidates(packageId, projectType, asOf, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }
}
