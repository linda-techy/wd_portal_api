package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.Estimation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstimationRepository extends JpaRepository<Estimation, UUID> {

    List<Estimation> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    Optional<Estimation> findByPublicViewToken(UUID publicViewToken);
}
