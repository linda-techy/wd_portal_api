-- V49: Ensure support ticket tables exist for portal staff access.
-- Tables created by customer API migration V1012; this ensures sequence exists.

CREATE SEQUENCE IF NOT EXISTS support_ticket_seq START WITH 1 INCREMENT BY 1;
