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
}
