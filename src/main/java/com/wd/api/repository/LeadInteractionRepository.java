package com.wd.api.repository;

import com.wd.api.model.LeadInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeadInteractionRepository extends JpaRepository<LeadInteraction, Long> {

    List<LeadInteraction> findByLeadIdOrderByInteractionDateDesc(Long leadId);

    List<LeadInteraction> findByInteractionType(String interactionType);

    List<LeadInteraction> findByCreatedById(Long createdById);

    @Query("SELECT li FROM LeadInteraction li WHERE li.nextActionDate IS NOT NULL AND li.nextActionDate BETWEEN ?1 AND ?2 ORDER BY li.nextActionDate")
    List<LeadInteraction> findUpcomingActions(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT li FROM LeadInteraction li WHERE li.nextActionDate IS NOT NULL AND li.nextActionDate < ?1 ORDER BY li.nextActionDate")
    List<LeadInteraction> findOverdueActions(LocalDateTime currentDate);
}
