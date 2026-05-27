-- ============================================================================
-- V162: Align change_orders.chk_co_status CHECK with the ChangeOrderStatus enum
-- ============================================================================
-- V34 introduced the internal-approval gate and added INTERNALLY_APPROVED /
-- INTERNALLY_REJECTED to the ChangeOrderStatus enum + ChangeOrderService, but the
-- V21 chk_co_status CHECK constraint was never widened. Result: the canonical CO
-- workflow breaks at internal approval —
--   PATCH /api/change-orders/{id}/approve-internal -> 500
--   ERROR: new row for relation "change_orders" violates check constraint "chk_co_status"
--
-- Recreate the constraint with the FULL enum set. Idempotent (DROP IF EXISTS).
-- VARCHAR(25) already fits INTERNALLY_APPROVED/INTERNALLY_REJECTED (19 chars).
-- ============================================================================

ALTER TABLE change_orders DROP CONSTRAINT IF EXISTS chk_co_status;

ALTER TABLE change_orders ADD CONSTRAINT chk_co_status CHECK (
    status IN (
        'DRAFT','SUBMITTED','INTERNALLY_APPROVED','INTERNALLY_REJECTED',
        'CUSTOMER_REVIEW','APPROVED','REJECTED',
        'IN_PROGRESS','COMPLETED','CLOSED'
    )
);
