package com.wd.api.repository;

import com.wd.api.dao.model.Leads;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadsRepository extends JpaRepository<Leads, Long>, JpaSpecificationExecutor<Leads> {

    // Custom query methods matching Controller requirements

    List<Leads> findByLeadStatus(String leadStatus);

    List<Leads> findByAssignedTeam(String assignedTeam);

    List<Leads> findByLeadSource(String leadSource);

    List<Leads> findByPriority(String priority);

    // For date range (using generic derived query)
    // List<Leads> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime
    // endDate);
    // But LeadController uses DateOfEnquiry or CreatedAt?
    // Controller: getLeadsByDateRange -> calls DAO.getLeadsByDateRange
    // DAO maps it to "date_of_enquiry".

    List<Leads> findByDateOfEnquiryBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

    // Search
    // findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase...
    // But better to use Specifications for complex search.

    // For simple search:
    List<Leads> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(String name,
            String email, String phone);

    // Analytics Helper Methods (counting)
    long countByLeadStatus(String status);

    List<Leads> findByNextFollowUpBeforeAndLeadStatusNotIn(java.time.LocalDateTime date,
            java.util.Collection<String> statuses);
}
