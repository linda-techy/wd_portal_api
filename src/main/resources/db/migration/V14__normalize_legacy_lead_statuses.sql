-- Normalize legacy lead status values to canonical DB values.
-- Canonical "won" status in DB is: project_won

UPDATE leads
SET lead_status = 'project_won'
WHERE lower(replace(replace(coalesce(lead_status, ''), ' ', ''), '_', '')) IN ('won', 'projectwon', 'converted');

UPDATE leads
SET lead_status = 'qualified'
WHERE lower(replace(replace(coalesce(lead_status, ''), ' ', ''), '_', '')) = 'qualifiedlead';

UPDATE leads
SET lead_status = 'proposal_sent'
WHERE lower(replace(replace(coalesce(lead_status, ''), ' ', ''), '_', '')) = 'proposalsent';

UPDATE leads
SET lead_status = 'new_inquiry'
WHERE lower(replace(replace(coalesce(lead_status, ''), ' ', ''), '_', '')) IN ('new', 'newinquiry');
