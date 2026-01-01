package com.wd.api.repository;

import com.wd.api.model.LeadDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadDocumentRepository extends JpaRepository<LeadDocument, Long> {
    List<LeadDocument> findByLeadIdAndIsActiveTrue(Long leadId);
}
