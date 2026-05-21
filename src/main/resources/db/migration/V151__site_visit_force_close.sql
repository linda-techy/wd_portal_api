-- Admin force-close for stuck site visits.
--
-- A "stuck" visit is one that was checked in under the old 2 km geofence
-- and now cannot be checked out because the user is more than 200 m away.
-- This migration does THREE things:
--
--   1.  Adds columns to record WHO force-closed a visit and WHY.
--   2.  Auto-closes any visits already stuck at the moment the geofence
--       policy changes from 2 km → 200 m. Conservative criteria: only rows
--       whose recorded check-in distance is strictly > 200 m and ≤ 2 km
--       (i.e. those that would have been valid under the old policy AND
--       invalid under the new one). Visits with no recorded distance are
--       left alone.
--   3.  Adds a SITE_VISIT_FORCE_CLOSE permission so the new admin endpoint
--       has an auth guard. Granted to ADMIN and PROJECT_MANAGER only.

-- ── 1. Schema additions ────────────────────────────────────────────────────
ALTER TABLE site_visits
    ADD COLUMN IF NOT EXISTS force_closed_by_user_id BIGINT REFERENCES portal_users(id),
    ADD COLUMN IF NOT EXISTS force_close_reason      TEXT;

COMMENT ON COLUMN site_visits.force_closed_by_user_id IS
    'When non-null, this visit was administratively closed by the named portal user (geofence GPS check bypassed). NULL for normal check-outs.';
COMMENT ON COLUMN site_visits.force_close_reason IS
    'Free-text justification entered by the admin (or the data-migration string for visits closed in V151).';

-- ── 2. One-shot fix for visits already stuck ───────────────────────────────
-- distance_from_project_checkin is in km. The old policy allowed up to 2 km;
-- the new policy allows up to 0.2 km. Rows in the open interval (0.200, 2.000]
-- are exactly the set that was valid before and invalid after.
UPDATE site_visits
SET
    check_out_time          = NOW(),
    visit_status            = 'CHECKED_OUT',
    check_out_notes         = COALESCE(check_out_notes, '') ||
                              CASE WHEN check_out_notes IS NULL OR check_out_notes = ''
                                   THEN ''
                                   ELSE E'\n'
                              END ||
                              'Auto-closed by V151: geofence policy changed from 2 km to 200 m. Original check-in distance was '
                                || ROUND(distance_from_project_checkin::numeric, 3) || ' km.',
    force_close_reason      = 'Auto-closed by V151: geofence policy changed from 2 km to 200 m.',
    duration_minutes        = COALESCE(
                                duration_minutes,
                                CAST(EXTRACT(EPOCH FROM (NOW() - check_in_time)) / 60 AS INTEGER)
                              )
WHERE check_out_time IS NULL
  AND visit_status = 'CHECKED_IN'
  AND distance_from_project_checkin IS NOT NULL
  AND distance_from_project_checkin >  0.200
  AND distance_from_project_checkin <= 2.000;

-- ── 3. New permission for the admin endpoint ───────────────────────────────
INSERT INTO portal_permissions (name, description) VALUES
    ('SITE_VISIT_FORCE_CLOSE',
     'Force-close another user''s active site visit, bypassing the GPS geofence check')
ON CONFLICT (name) DO NOTHING;

WITH role_permission_mapping(role_code, perm_name) AS (
    VALUES
    ('ADMIN',           'SITE_VISIT_FORCE_CLOSE'),
    ('PROJECT_MANAGER', 'SITE_VISIT_FORCE_CLOSE')
)
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM role_permission_mapping rpm
JOIN portal_roles r        ON r.code = rpm.role_code
JOIN portal_permissions p  ON p.name = rpm.perm_name
WHERE NOT EXISTS (
    SELECT 1 FROM portal_role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
