-- G-19: Add @Version column for optimistic locking on final_account.
--
-- Two concurrent edits (Finance + PM) on the same final account can otherwise
-- silently overwrite each other's changes. Adding a Hibernate-managed version
-- column makes the second save fail loudly with OptimisticLockingFailureException,
-- which the controller translates to a 409 Conflict so the UI can re-fetch and
-- prompt the user to merge.
--
-- Existing rows start at version 0; Hibernate increments on every update.

ALTER TABLE final_account
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
