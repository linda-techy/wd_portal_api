-- V61__add_customer_visible_to_tasks.sql
-- Adds visibility flag to control which tasks the customer-facing
-- timeline endpoint exposes. Defaults TRUE because most construction
-- tasks are customer-visible; portal can mark internal-only tasks
-- (rare) by setting this FALSE.

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS customer_visible BOOLEAN NOT NULL DEFAULT TRUE;
