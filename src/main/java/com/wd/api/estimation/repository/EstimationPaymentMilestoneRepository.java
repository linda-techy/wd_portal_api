package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.EstimationPaymentMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EstimationPaymentMilestoneRepository extends JpaRepository<EstimationPaymentMilestone, UUID> {

    List<EstimationPaymentMilestone> findByEstimationIdOrderByDisplayOrderAsc(UUID estimationId);
}
