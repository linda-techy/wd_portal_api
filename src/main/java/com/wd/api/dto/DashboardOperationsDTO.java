package com.wd.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOperationsDTO {

    private long labourOnSiteToday;    // LabourAttendance WHERE date=today AND status=PRESENT
    private long siteReportsThisWeek;  // SiteReport WHERE reportDate >= startOfWeek
    private long totalOverdueTasks;    // Task WHERE dueDate < today AND status NOT IN [COMPLETED,CANCELLED]
    private long tasksDueToday;        // Task WHERE dueDate = today AND status NOT IN [COMPLETED,CANCELLED]
    private long openObservations;     // Observation WHERE status IN [OPEN, IN_PROGRESS]
    private long pendingApprovals;     // ApprovalRequest WHERE status = PENDING
    private long activeDelays;         // DelayLog WHERE toDate IS NULL (still ongoing)
}
