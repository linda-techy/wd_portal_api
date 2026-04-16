-- Normalize lead_source to lowercase snake_case convention.
-- Fixes inconsistency where some sources were inserted as uppercase (e.g. CUSTOMER_APP).
UPDATE leads SET lead_source = LOWER(lead_source) WHERE lead_source != LOWER(lead_source);
