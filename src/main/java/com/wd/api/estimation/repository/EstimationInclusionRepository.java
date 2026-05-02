package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.EstimationInclusion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EstimationInclusionRepository extends JpaRepository<EstimationInclusion, UUID> {

    List<EstimationInclusion> findByEstimationIdOrderByDisplayOrderAsc(UUID estimationId);
}
