-- ===========================================================================
-- V123 — S3 PR2 site-report task linkage + report-type CHECK
--
-- Adds task_id (nullable FK to tasks) so a SiteReport can attest to the
-- completion of a specific task. report_type is added IF NOT EXISTS because
-- Hibernate's create-drop bootstrap already auto-generates it on fresh DBs;
-- this migration ensures Flyway-only environments get it too. The CHECK
-- constraint enforces the closed enum {DAILY_PROGRESS, COMPLETION, ISSUE,
-- QUALITY_CHECK, SAFETY_INCIDENT, MATERIAL_DELIVERY, SITE_VISIT_SUMMARY,
-- OTHER} so the Java ReportType enum cannot drift open-endedly. The partial
-- index supports the existsBy… query in TaskCompletionService.
-- ===========================================================================

ALTER TABLE site_reports
    ADD COLUMN IF NOT EXISTS task_id BIGINT NULL
        REFERENCES tasks(id) ON DELETE SET NULL;

ALTER TABLE site_reports
    ADD COLUMN IF NOT EXISTS report_type VARCHAR(50) NOT NULL
        DEFAULT 'DAILY_PROGRESS';

ALTER TABLE site_reports
    DROP CONSTRAINT IF EXISTS site_reports_report_type_chk;

ALTER TABLE site_reports
    ADD CONSTRAINT site_reports_report_type_chk
        CHECK (report_type IN (
            'DAILY_PROGRESS',
            'COMPLETION',
            'ISSUE',
            'QUALITY_CHECK',
            'SAFETY_INCIDENT',
            'MATERIAL_DELIVERY',
            'SITE_VISIT_SUMMARY',
            'OTHER'
        ));

CREATE INDEX IF NOT EXISTS idx_site_reports_task_completion
    ON site_reports (task_id, report_type)
    WHERE task_id IS NOT NULL;
