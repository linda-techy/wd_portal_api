package com.wd.api.repository;

import com.wd.api.model.SiteVisitViolation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SiteVisitViolationRepository extends JpaRepository<SiteVisitViolation, Long> {

    /**
     * Filtered + paginated query for the portal admin view.
     * Any filter parameter may be null — null means "do not filter on this".
     *
     * Uses JOIN FETCH for project + user because the DTO mapping touches
     * `.getProject().getName()` and `.getUser().getFirstName()/getLastName()/getEmail()`
     * on every row; without the fetch joins we'd trigger 2N additional queries
     * per page. The count query Spring Data generates for pagination does NOT
     * inherit the fetch joins (countQuery uses a separate JPQL), so the
     * pagination still works correctly.
     */
    @Query(value = """
        SELECT v FROM SiteVisitViolation v
        JOIN FETCH v.project
        JOIN FETCH v.user
        WHERE (:projectId IS NULL OR v.project.id = :projectId)
          AND (:userId    IS NULL OR v.user.id    = :userId)
          AND (:from      IS NULL OR v.attemptedAt >= :from)
          AND (:to        IS NULL OR v.attemptedAt <= :to)
        ORDER BY v.attemptedAt DESC
        """,
        countQuery = """
        SELECT COUNT(v) FROM SiteVisitViolation v
        WHERE (:projectId IS NULL OR v.project.id = :projectId)
          AND (:userId    IS NULL OR v.user.id    = :userId)
          AND (:from      IS NULL OR v.attemptedAt >= :from)
          AND (:to        IS NULL OR v.attemptedAt <= :to)
        """)
    Page<SiteVisitViolation> search(@Param("projectId") Long projectId,
                                    @Param("userId") Long userId,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to,
                                    Pageable pageable);
}
