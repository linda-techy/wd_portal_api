package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.EstimationAssumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EstimationAssumptionRepository extends JpaRepository<EstimationAssumption, UUID> {

    List<EstimationAssumption> findByEstimationIdOrderByDisplayOrderAsc(UUID estimationId);
}
