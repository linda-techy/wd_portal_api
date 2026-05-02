package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.EstimationExclusion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EstimationExclusionRepository extends JpaRepository<EstimationExclusion, UUID> {

    List<EstimationExclusion> findByEstimationIdOrderByDisplayOrderAsc(UUID estimationId);
}
