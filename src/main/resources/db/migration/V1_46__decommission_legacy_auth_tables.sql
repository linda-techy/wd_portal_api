-- Decommission legacy security tables and unmapped orphaned artifacts
-- These are replaced by portal_users, portal_roles, customer_users, etc.

-- 1. Drop join table and primary legacy auth tables
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

-- 2. Drop orphaned customer-side legacy tables (not mapped/used in Java)
DROP TABLE IF EXISTS customer_project_members;
DROP TABLE IF EXISTS customer_project_team_members;
DROP TABLE IF EXISTS customer_role_permissions;
DROP TABLE IF EXISTS customer_permissions;
