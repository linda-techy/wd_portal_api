-- ===========================================================================
-- V140 — Design Package Template catalog
--
-- Until now the customer-facing "Custom / Premium / Bespoke" tier list was
-- hardcoded in wd_customer_app_flutter — marketing couldn't change a rate
-- or add a tier without an app release, and portal staff couldn't see the
-- offerings they were selling. This migration introduces a proper template
-- catalog managed from the admin portal and consumed by the customer app.
--
-- Idempotent throughout (CREATE TABLE IF NOT EXISTS / ON CONFLICT DO NOTHING)
-- to match the rest of this project's migration conventions.
-- ===========================================================================

-- 1) Table
CREATE TABLE IF NOT EXISTS design_package_templates (
    id                          BIGSERIAL    PRIMARY KEY,
    code                        VARCHAR(50)  NOT NULL UNIQUE, -- machine code (CUSTOM, PREMIUM, BESPOKE)
    name                        VARCHAR(100) NOT NULL,        -- display name
    tagline                     VARCHAR(255),                 -- short marketing line
    description                 TEXT,
    rate_per_sqft               NUMERIC(10,2) NOT NULL,       -- default ₹/sqft
    full_payment_discount_pct   NUMERIC(5,2)  NOT NULL DEFAULT 0,
    revisions_included          INTEGER       NOT NULL DEFAULT 2,
    features                    TEXT,                          -- newline-separated bullets
    display_order               INTEGER       NOT NULL DEFAULT 0,
    is_active                   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by_user_id          BIGINT,
    updated_by_user_id          BIGINT
);

CREATE INDEX IF NOT EXISTS idx_design_package_templates_active_order
    ON design_package_templates (is_active, display_order);

-- 2) Seed the three tiers the customer app already hardcodes — so existing
--    customer projects keep showing the same content during cutover.
INSERT INTO design_package_templates
    (code, name, tagline, description, rate_per_sqft, full_payment_discount_pct,
     revisions_included, features, display_order, is_active)
VALUES
    ('CUSTOM', 'Custom',
     'Budget-friendly tailored design',
     'Standard plans with personal customisations. Best for first-time home builders.',
     95.00, 10.00, 2,
     E'2D floor plans\n3D exterior view\nBasic structural design\nBasic MEP layout\n2 revisions included',
     1, TRUE),
    ('PREMIUM', 'Premium',
     'Architect-led, fully personalised',
     'Architect-driven design with detailed 3D walkthroughs and interior styling.',
     140.00, 12.00, 3,
     E'2D + detailed working drawings\n3D exterior + interior views\nFull structural design\nFull MEP design\nInterior concept boards\n3 revisions included',
     2, TRUE),
    ('BESPOKE', 'Bespoke',
     'Luxury, no-compromise design',
     'Top-tier bespoke design with premium materials specs, lighting, landscape and bespoke interiors.',
     240.00, 15.00, 5,
     E'Bespoke architecture\nPhoto-realistic 3D + VR walkthroughs\nFull structural + MEP + HVAC\nInterior + furniture selection\nLandscape + lighting design\n5 revisions included',
     3, TRUE)
ON CONFLICT (code) DO NOTHING;

-- 3) Permissions for the portal admin UI
INSERT INTO portal_permissions (name, description) VALUES
    ('DESIGN_PACKAGE_VIEW',   'View the catalog of design package templates'),
    ('DESIGN_PACKAGE_MANAGE', 'Create / edit / activate / archive design package templates')
ON CONFLICT (name) DO NOTHING;

-- 4) Grant to ADMIN (other roles can be added in a future seed). Mirrors
--    V117's join-based grant pattern so the migration is rerunnable.
INSERT INTO portal_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM portal_roles r
JOIN portal_permissions p ON p.name IN ('DESIGN_PACKAGE_VIEW', 'DESIGN_PACKAGE_MANAGE')
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
ON CONFLICT DO NOTHING;
