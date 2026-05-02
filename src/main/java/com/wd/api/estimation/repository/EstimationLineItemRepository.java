package com.wd.api.estimation.repository;

import com.wd.api.estimation.domain.EstimationLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EstimationLineItemRepository extends JpaRepository<EstimationLineItem, UUID> {

    List<EstimationLineItem> findByEstimationIdOrderByDisplayOrderAsc(UUID estimationId);
}
