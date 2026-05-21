-- ===========================================================================
-- V156 — Seed missing LEAD_* activity_types rows.
--
-- Audit derived from greppping every `logProjectActivity / logActivity /
-- logSystemActivity("TYPE_NAME", ...)` literal in the codebase and comparing
-- against rows inserted by V33 / V34 / V155. Result: six lead-side type
-- names are referenced in code but never seeded:
--
--   LEAD_CREATED         (LeadService createLead)
--   LEAD_UPDATED         (LeadService updateLead)
--   LEAD_ASSIGNED        (LeadService assignLead)
--   LEAD_CONVERTED       (LeadConversionService convertToProject)
--   LEAD_STATUS_CHANGED  (LeadService status transitions)
--   LEAD_SCORE_UPDATED   (LeadScoringService)
--
-- Same failure mode as TASK_STATUS_CHANGED (V155): the call throws
-- `RuntimeException: Activity Type not found: ...` and the wrapping
-- @Transactional rolls back the entire write — meaning the lead-side write
-- itself silently fails. Probably masked in production by exception-swallow
-- logging elsewhere; surfaced loud and clear once any caller actually checks
-- the response.
--
-- Idempotent — uses WHERE NOT EXISTS, safe everywhere.
-- ===========================================================================

INSERT INTO activity_types (name, description)
SELECT 'LEAD_CREATED', 'New lead added to the pipeline'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'LEAD_CREATED');

INSERT INTO activity_types (name, description)
SELECT 'LEAD_UPDATED', 'Lead record updated (contact / scope / notes)'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'LEAD_UPDATED');

INSERT INTO activity_types (name, description)
SELECT 'LEAD_ASSIGNED', 'Lead reassigned to a different sales user'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'LEAD_ASSIGNED');

INSERT INTO activity_types (name, description)
SELECT 'LEAD_CONVERTED', 'Lead converted to project'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'LEAD_CONVERTED');

INSERT INTO activity_types (name, description)
SELECT 'LEAD_STATUS_CHANGED', 'Lead pipeline-stage transition (e.g. NEW -> QUALIFIED)'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'LEAD_STATUS_CHANGED');

INSERT INTO activity_types (name, description)
SELECT 'LEAD_SCORE_UPDATED', 'Lead score recomputed by scoring engine'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'LEAD_SCORE_UPDATED');
