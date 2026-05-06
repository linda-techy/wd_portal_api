package com.wd.api.model.enums;

public enum ReportType {
    DAILY_PROGRESS,
    COMPLETION,           // S3 PR2: geotagged photo evidence for task completion
    ISSUE,                // S3 PR2: site issue / blocker reports
    QUALITY_CHECK,
    SAFETY_INCIDENT,
    MATERIAL_DELIVERY,
    SITE_VISIT_SUMMARY,
    OTHER
}
