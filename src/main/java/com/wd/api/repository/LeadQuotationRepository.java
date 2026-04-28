package com.wd.api.repository;

import com.wd.api.model.LeadQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadQuotationRepository extends JpaRepository<LeadQuotation, Long>, JpaSpecificationExecutor<LeadQuotation> {

    List<LeadQuotation> findByLeadId(Long leadId);

    List<LeadQuotation> findByStatus(String status);

    Optional<LeadQuotation> findByQuotationNumber(String quotationNumber);

    List<LeadQuotation> findByLeadIdOrderByVersionDesc(Long leadId);

    List<LeadQuotation> findByCreatedById(Long createdById);

    @Query("SELECT q FROM LeadQuotation q LEFT JOIN FETCH q.items WHERE q.id = :id")
    Optional<LeadQuotation> findByIdWithItems(@Param("id") Long id);

    /**
     * Atomic counter for {@code QUO-{date}-{NNNN}} numbering. Backed by the
     * Postgres SEQUENCE created in V72 — no race conditions, no reuse after
     * delete.
     */
    @Query(value = "SELECT nextval('lead_quotation_number_seq')", nativeQuery = true)
    Long nextQuotationSequenceValue();

    /**
     * One-shot fetch of the rows the pipeline-summary aggregator cares about:
     * everything currently open, plus everything that closed (accepted or
     * rejected) in the last 90 days. Service composes the per-bucket
     * aggregates in Java rather than running 4 separate vendor-specific
     * SQL queries — at residential-builder scale (low thousands of rows
     * over 90 days) this is comfortably fast.
     *
     * @return tuples of {@code [status, finalAmount, sentAt, respondedAt]}
     */
    @Query("SELECT q.status, q.finalAmount, q.sentAt, q.respondedAt "
            + "FROM LeadQuotation q "
            + "WHERE q.status IN ('DRAFT', 'SENT', 'VIEWED') "
            + "   OR (q.respondedAt IS NOT NULL AND q.respondedAt > :since)")
    List<Object[]> pipelineRowsSince(@Param("since") java.time.LocalDateTime since);

    /**
     * Mark every {@code SENT} or {@code VIEWED} quotation whose validity
     * window has elapsed ({@code created_at + validity_days < now()}) as
     * {@code EXPIRED}. Run by the scheduled
     * {@code LeadQuotationExpiryScheduler}. Returns the count of rows updated.
     *
     * <p>Native SQL because JPQL has no clean way to express
     * "timestamp + interval-from-column".
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE lead_quotations "
            + "SET status = 'EXPIRED', updated_at = NOW() "
            + "WHERE status IN ('SENT', 'VIEWED') "
            + "AND validity_days IS NOT NULL "
            + "AND created_at IS NOT NULL "
            + "AND created_at + (validity_days || ' days')::INTERVAL < NOW()",
            nativeQuery = true)
    int markExpiredQuotations();

    /**
     * Restore a soft-deleted quotation by clearing its {@code deleted_at}
     * tombstone. Native query so it bypasses the entity's
     * {@code @SQLRestriction("deleted_at IS NULL")} filter — JPA wouldn't
     * see the tombstoned row otherwise. Used by the Undo flow after a
     * delete inside the Flutter snackbar window.
     *
     * @return number of rows updated (1 if restored, 0 if not deleted or
     *         not present)
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE lead_quotations SET deleted_at = NULL "
            + "WHERE id = :id AND deleted_at IS NOT NULL",
            nativeQuery = true)
    int restoreById(@Param("id") Long id);
}
