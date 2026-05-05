-- ===========================================================================
-- R__seed_wbs_templates_marker — repeatable Flyway migration that touches
-- the wbs_template_seed_marker row whenever its checksum changes. The
-- actual YAML→DB seeding is performed by WbsTemplateSeeder
-- (CommandLineRunner) at application boot — that runner is content-hash
-- driven and version-bumps templates whose YAML changed.
--
-- Bump the comment block below (e.g. add a "rev N" line) to force Flyway
-- to re-run this marker, which is purely cosmetic; the seeder always runs
-- on boot regardless.
--
-- rev 1 — initial scaffold for S1 PR2.
-- ===========================================================================

INSERT INTO wbs_template_seed_marker (id, last_applied_at, note)
VALUES (1, NOW(), 'rev 1 — S1 PR2 initial')
ON CONFLICT (id) DO UPDATE
    SET last_applied_at = EXCLUDED.last_applied_at,
        note            = EXCLUDED.note;
