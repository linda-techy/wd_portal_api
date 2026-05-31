package com.wd.api.dto;

import com.wd.api.model.ProjectWarranty;
import com.wd.api.model.enums.WarrantyStatus;
import java.time.LocalDate;

public record ProjectWarrantyRequest(
        String componentName, String description, String providerName,
        LocalDate startDate, LocalDate endDate, WarrantyStatus status,
        String coverageDetails) {
    public ProjectWarranty toEntity() {
        ProjectWarranty w = new ProjectWarranty();
        w.setComponentName(componentName); w.setDescription(description);
        w.setProviderName(providerName); w.setStartDate(startDate); w.setEndDate(endDate);
        w.setStatus(status); w.setCoverageDetails(coverageDetails);
        return w;
    }
}
