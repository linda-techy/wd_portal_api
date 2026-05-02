-- ===========================================================================
-- V103 — Drop legacy quotation tables (Sub-project C.PR-3)
--
-- Replaced entirely by the estimation module (`estimation`, `estimation_line_item`,
-- `estimation_package_*`, `estimation_market_index_snapshot`). The legacy
-- LeadQuotation* + QuotationCatalog* + PublicQuotation* code paths are deleted
-- in this same PR; this migration drops the tables they wrote to.
--
-- Drop order matches FK-dependency direction (children first). Each statement
-- is `IF EXISTS` so partial drops or repeated runs are safe.
-- ===========================================================================

DROP TABLE IF EXISTS quotation_view_log               CASCADE;
DROP TABLE IF EXISTS quotation_payment_milestone      CASCADE;
DROP TABLE IF EXISTS quotation_assumption             CASCADE;
DROP TABLE IF EXISTS quotation_exclusion              CASCADE;
DROP TABLE IF EXISTS quotation_inclusion              CASCADE;
DROP TABLE IF EXISTS lead_quotation_items             CASCADE;
DROP TABLE IF EXISTS lead_quotations                  CASCADE;
DROP TABLE IF EXISTS quotation_item_catalog           CASCADE;

-- The QUOTATION_CATALOG_* permissions are orphaned now. Remove them so the
-- ACL admin UI doesn't show dead entries. The permissions table is named
-- `portal_permissions` (see com.wd.api.config.AppConfig.PERMISSION_TABLE).
-- Linking tables (role_permissions, etc.) cascade-delete via FK constraints.
DELETE FROM portal_permissions WHERE name LIKE 'QUOTATION_CATALOG_%';
