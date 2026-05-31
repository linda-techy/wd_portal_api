package com.wd.api.dto;

import com.wd.api.model.ProjectVariation;
import com.wd.api.model.enums.VariationStatus;
import java.math.BigDecimal;

public record ProjectVariationRequest(
        String description, BigDecimal estimatedAmount, Boolean clientApproved,
        VariationStatus status, String notes, BigDecimal costImpact,
        Integer timeImpactWorkingDays, String rejectionReason) {
    public ProjectVariation toEntity() {
        ProjectVariation v = new ProjectVariation();
        v.setDescription(description); v.setEstimatedAmount(estimatedAmount);
        v.setClientApproved(clientApproved); v.setStatus(status); v.setNotes(notes);
        v.setCostImpact(costImpact); v.setTimeImpactWorkingDays(timeImpactWorkingDays);
        v.setRejectionReason(rejectionReason);
        return v;
    }
}
