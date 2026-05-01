-- ===========================================================================
-- V87 — Defensive: add any PortalUser columns the entity declares but
--        the schema is missing.
--
-- V86 added designation + department, but on the next boot Hibernate
-- failed again on `column pu1_0.whatsapp does not exist`. The entity
-- has accumulated several columns over time (whatsapp, fcm_token, phone,
-- designation, department) and migrations were added piecemeal; this
-- defensive migration adds anything still missing in one shot so the
-- next entity addition doesn't crash startup the same way.
--
-- Idempotent — every ALTER uses IF NOT EXISTS, so safe against any
-- partially-applied schema.
-- ===========================================================================

ALTER TABLE portal_users
    ADD COLUMN IF NOT EXISTS whatsapp     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS phone        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS fcm_token    VARCHAR(512),
    ADD COLUMN IF NOT EXISTS designation  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS department   VARCHAR(100);

COMMENT ON COLUMN portal_users.whatsapp IS
    'WhatsApp contact number — surfaced as the click-to-chat link on '
    'lead/customer screens. Optional; format-free at the DB layer.';
