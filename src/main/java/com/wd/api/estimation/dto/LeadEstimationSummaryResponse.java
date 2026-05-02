package com.wd.api.estimation.dto;

import com.wd.api.estimation.domain.Estimation;
import com.wd.api.estimation.domain.enums.EstimationStatus;
import com.wd.api.estimation.domain.enums.ProjectType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LeadEstimationSummaryResponse(
        UUID id,
        String estimationNo,
        Long leadId,
        ProjectType projectType,
        UUID packageId,
        EstimationStatus status,
        BigDecimal grandTotal,
        LocalDate validUntil,
        LocalDateTime createdAt) {

    public static LeadEstimationSummaryResponse fromEntity(Estimation e) {
        return new LeadEstimationSummaryResponse(
                e.getId(),
                e.getEstimationNo(),
                e.getLeadId(),
                e.getProjectType(),
                e.getPackageId(),
                e.getStatus(),
                e.getGrandTotal(),
                e.getValidUntil(),
                e.getCreatedAt());
    }
}
