-- Test-only schema additions: Postgres functions created by Flyway in
-- production but not materialised by Hibernate create-drop.
--
-- Use a pure SQL function (not plpgsql) so Spring's default ';' statement
-- separator doesn't split the body.
CREATE OR REPLACE FUNCTION generate_receipt_number() RETURNS text AS
'SELECT ''RCP-'' || LPAD((COALESCE(MAX(id), 0) + 1)::text, 6, ''0'') FROM payment_transactions'
LANGUAGE SQL;
