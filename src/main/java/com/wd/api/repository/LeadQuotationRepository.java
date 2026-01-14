package com.wd.api.repository;

import com.wd.api.model.LeadQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
