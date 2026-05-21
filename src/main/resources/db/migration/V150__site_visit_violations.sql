-- Site-visit geofence violations.
--
-- When an employee attempts check-in or check-out from outside the geofence
-- (currently 200 m — see GeoUtils.MAX_CHECKIN_DISTANCE_KM), the attempt is
-- BLOCKED and an audit row is written here. Portal admins / managers see the
-- list; customers never do (no customer-api endpoint exposes this table).
--
-- This is purely an audit log — there is no workflow on the rows themselves.
-- A future revision can add a `status` column (PENDING / REVIEWED / DISMISSED)
-- if the business decides to track manager review.

CREATE TABLE site_visit_violations (
    id                  BIGSERIAL PRIMARY KEY,
    project_id          BIGINT NOT NULL REFERENCES customer_projects(id),
    user_id             BIGINT NOT NULL REFERENCES portal_users(id),
    attempt_type        VARCHAR(20) NOT NULL,           -- CHECK_IN | CHECK_OUT
    attempted_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    attempted_latitude  DOUBLE PRECISION NOT NULL,
    attempted_longitude DOUBLE PRECISION NOT NULL,
    project_latitude    DOUBLE PRECISION,
    project_longitude   DOUBLE PRECISION,
    distance_km         DOUBLE PRECISION NOT NULL,
    allowed_radius_km   DOUBLE PRECISION NOT NULL,
    visit_id            BIGINT,                          -- null for failed CHECK_IN; set for failed CHECK_OUT
    error_message       TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT site_visit_violations_attempt_type_check
        CHECK (attempt_type IN ('CHECK_IN', 'CHECK_OUT'))
);

CREATE INDEX idx_site_visit_violations_project_attempted
    ON site_visit_violations (project_id, attempted_at DESC);

CREATE INDEX idx_site_visit_violations_user_attempted
    ON site_visit_violations (user_id, attempted_at DESC);

COMMENT ON TABLE  site_visit_violations IS
    'Audit log of failed (out-of-geofence) site-visit check-in/check-out attempts. Portal-internal; not exposed to customers.';
COMMENT ON COLUMN site_visit_violations.distance_km IS
    'Distance in km from the project site at the time of the attempt.';
COMMENT ON COLUMN site_visit_violations.allowed_radius_km IS
    'The geofence radius the attempt was checked against, captured for historical accuracy if the policy changes later.';

-- ── Permission for viewing the violations list ──────────────────────────────
-- Deliberately narrower than SITE_VISIT_VIEW: violation review is a
-- compliance/management activity, not an everyday operational view. Site
-- engineers / foremen should not be auditing their peers.

INSERT INTO portal_permissions (name, description) VALUES
    ('SITE_VISIT_VIOLATION_VIEW', 'View geofence violation audit log for site visits')
ON CONFLICT (name) DO NOTHING;

WITH role_permission_mapping(role_code, perm_name) AS (
    VALUES
    ('ADMIN',           'SITE_VISIT_VIOLATION_VIEW'),
    ('PROJECT_MANAGER', 'SITE_VISIT_VIOLATION_VIEW')
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
