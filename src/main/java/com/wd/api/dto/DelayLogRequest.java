package com.wd.api.dto;

import com.wd.api.model.DelayLog;
import com.wd.api.model.ProjectPhase;
import java.time.LocalDate;

public record DelayLogRequest(
        ProjectPhase phase, String delayType, LocalDate fromDate, LocalDate toDate, String reasonText,
        String reasonCategory, String responsibleParty, Integer durationDays, String impactDescription,
        boolean customerVisible, String customerSummary, String impactOnHandover) {
    public DelayLog toEntity() {
        DelayLog d = new DelayLog();
        d.setPhase(phase);
        d.setDelayType(delayType);
        d.setFromDate(fromDate);
        d.setToDate(toDate);
        d.setReasonText(reasonText);
        d.setReasonCategory(reasonCategory);
        d.setResponsibleParty(responsibleParty);
        d.setDurationDays(durationDays);
        d.setImpactDescription(impactDescription);
        d.setCustomerVisible(customerVisible);
        d.setCustomerSummary(customerSummary);
        d.setImpactOnHandover(impactOnHandover);
        return d;
    }
}
