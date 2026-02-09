package com.wd.api.repository;

import com.wd.api.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Lead Repository - Standardized for unified security model
 * 
 * Key Queries:
 * - Lead status, source, and priority tracking
 * - Assignment-based retrieval
 * - Analytics aggregations for dashboard
 * - Follow-up monitoring
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    List<Lead> findByLeadStatus(String leadStatus);

    List<Lead> findByAssignedTeam(String assignedTeam);

    List<Lead> findByAssignedTo_Id(Long assignedToId);

    List<Lead> findByLeadSource(String leadSource);

    List<Lead> findByPriority(String priority);

    List<Lead> findByDateOfEnquiryBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

    List<Lead> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(
            String name, String email, String phone);

    long countByLeadStatus(String status);

    List<Lead> findByNextFollowUpBeforeAndLeadStatusNotIn(
            java.time.LocalDateTime date, java.util.Collection<String> statuses);

    @Query("SELECT l.leadSource, COUNT(l) FROM Lead l GROUP BY l.leadSource")
    List<Object[]> countLeadsBySource();

    @Query("SELECT l.priority, COUNT(l) FROM Lead l GROUP BY l.priority")
    List<Object[]> countLeadsByPriority();

    List<Lead> findByLeadSourceAndNotesContaining(String leadSource, String notesFragment);
}
