# BOQ Module — Security, Visibility & Integrity
**Date:** 2026-04-14  
**Branch:** fix/security-precision-concurrency-and-rate-limiting  
**Status:** Approved for implementation

---

## Problem Statement

A deep analysis of the BOQ module identified four categories of issues requiring resolution before production launch:

1. **No project-level access scoping** — any portal user with `BOQ_VIEW` can query any project's BOQ by guessing project IDs.
2. **No customer-facing BOQ API** — the customer app cannot see contract value, payment schedule, or BOQ status; customer "approval" is a portal-user proxy with no identity captured.
3. **`customer_approved_by` always null** — the DB FK referencing `customer_users(id)` is never populated, eliminating the audit trail for customer consent.
4. **`itemKind` is an unconstrained free-text String** — any value can be stored; `quantity = 0` is permitted for BASE/ADDON items.

---

## Scope

### In scope
- Project-level access control for all BOQ endpoints
- Customer-facing BOQ summary and payment stage view (portal and customer app)
- Dual-track BOQ approval: portal captures customer identity, customer app records acknowledgement
- `ItemKind` enum promotion + quantity validation for BASE/ADDON items
- Flyway migrations for new columns and check constraints

### Out of scope
- Retention calculation (deferred — will be configurable when implemented)
- `boq_stage_config` table cleanup (separate task)
- `boq_document_id` ORM linkage on `BoqItem` (separate migration risk)
- Change Order access scoping (follow-on from this work)

---

## Architecture

### Request flow after this change

```
Incoming portal request (BoqController / BoqDocumentController)
  └─ ProjectAccessGuard.verifyPortalAccess(userId, projectId)
       ├─ ADMIN / DIRECTOR role → pass (global access)
       └─ all other roles → must appear in project_members for projectId
  └─ BOQ business logic (BoqService / BoqDocumentService)
       └─ itemKind validated as ItemKind enum

Incoming customer request (CustomerBoqController)
  └─ CustomerUser principal extracted from JWT
  └─ ProjectAccessGuard.verifyCustomerAccess(customerUserId, projectId)
       └─ must appear in project_members for projectId
  └─ Returns CustomerBoqSummary or List<CustomerPaymentStageView>
       └─ no unitRate, no execution quantities, no internal user IDs

BOQ customer-approve (portal side)
  └─ customerSignedById (Long, required) validated against customer_users
  └─ customer must be a project member
  └─ stored in customer_approved_by (no longer null)

BOQ customer-acknowledge (customer app)
  └─ PATCH /api/customer/boq/documents/{id}/acknowledge
  └─ stores customer_acknowledged_at + customer_acknowledged_by
  └─ does not change document status
```

---

## Section 1 — Project-Level Access Control

### New class: `ProjectAccessGuard`

**Package:** `com.wd.api.security`

**Responsibilities:**
- Single injectable bean used by `BoqService` and `BoqDocumentService`
- Two public methods:
  - `verifyPortalAccess(Long portalUserId, Long projectId)` — called by portal-side services
  - `verifyCustomerAccess(Long customerUserId, Long projectId)` — called by customer-side controller
- Roles with global portal access (bypass membership check): `ADMIN`, `DIRECTOR`
- All other roles must have a row in `project_members` linking them to the project

**How role checking works:** `verifyPortalAccess` reads authorities from `SecurityContextHolder.getContext().getAuthentication()` directly — no extra DB query, no service signature changes. The `portalUserId` parameter is used only for the `project_members` membership query when the role check does not bypass.

**Error response:** throws `AccessDeniedException` (HTTP 403) with message `"You do not have access to project {projectId}"`

### Roles with global access

| Role | Global BOQ access |
|------|------------------|
| ADMIN | Yes |
| DIRECTOR | Yes |
| All others (PROJECT_MANAGER, ESTIMATOR, etc.) | Project members only |

### Integration points

`ProjectAccessGuard.verifyPortalAccess()` is called at the top of these service methods:
- `BoqService.createBoqItem()` — uses `request.projectId()`
- `BoqService.getProjectBoq()` / `getProjectBoqPaged()` / `getFinancialSummary()`
- `BoqService.searchBoqItems()` — when `filter.projectId` is set
- `BoqDocumentService.createDocument()` / `submitForApproval()` / `approveInternally()` / `recordCustomerApproval()` / `reject()`
- `BoqDocumentService.getProjectDocuments()` / `getApprovedDocument()`

For single-item operations (`getBoqItemById`, `updateBoqItem`, etc.) the project ID is resolved from the loaded entity before the guard is called.

### Repository addition

```java
// ProjectMemberRepository (existing)
boolean existsByProjectIdAndPortalUserId(Long projectId, Long portalUserId);
boolean existsByProjectIdAndCustomerUserId(Long projectId, Long customerUserId);
```

---

## Section 2 — Customer-Facing BOQ View

### New controller: `CustomerBoqController`

**Package:** `com.wd.api.controller`  
**Base path:** `/api/customer/boq`  
**Authentication:** `CustomerUser` principal (customer JWT, existing filter)

### Endpoints

#### `GET /api/customer/boq/project/{projectId}/summary`

Returns `CustomerBoqSummary`. If no approved BOQ exists, returns the latest non-rejected document's status so the customer sees "pending" rather than an error.

#### `GET /api/customer/boq/project/{projectId}/payment-stages`

Returns `List<CustomerPaymentStageView>`. Only available once BOQ is `APPROVED`. Returns empty list if not yet approved.

#### `PATCH /api/customer/boq/documents/{documentId}/acknowledge`

Records customer acknowledgement. Idempotent — re-acknowledging overwrites `acknowledged_at` with the latest timestamp (the customer may re-open and re-confirm).

### New DTO: `CustomerBoqSummary`

```java
public record CustomerBoqSummary(
    Long documentId,
    Long projectId,
    String projectName,
    BigDecimal totalValueExGst,
    BigDecimal totalGstAmount,
    BigDecimal totalValueInclGst,
    BigDecimal gstRate,
    String status,                    // DRAFT | PENDING_APPROVAL | APPROVED | REJECTED
    Integer revisionNumber,
    LocalDateTime approvedAt,         // customer_approved_at (when portal recorded approval)
    LocalDateTime acknowledgedAt,     // customer_acknowledged_at (customer's own confirmation)
    boolean pendingAcknowledgement,   // true if PENDING_APPROVAL and not yet acknowledged
    List<CustomerPaymentStageView> paymentStages
)
```

### New DTO: `CustomerPaymentStageView`

```java
public record CustomerPaymentStageView(
    Integer stageNumber,
    String stageName,
    BigDecimal stageAmountExGst,
    BigDecimal gstAmount,
    BigDecimal stageAmountInclGst,
    BigDecimal stagePercentage,
    String status,                    // UPCOMING | DUE | INVOICED | PAID | OVERDUE | ON_HOLD
    LocalDate dueDate,
    String milestoneDescription
)
```

### Fields explicitly excluded from customer DTOs

- `unitRate` (internal pricing)
- `executedQuantity`, `billedQuantity`, `remainingQuantity`, `remainingBillableQuantity`
- `totalExecutedAmount`, `totalBilledAmount`, `costToComplete`
- `boqValueSnapshot` (internal snapshot)
- `retentionHeld`, `retentionPct`, `certifiedBy`
- `createdByUserId`, `updatedByUserId`, `appliedCreditAmount`, `netPayableAmount`
- `specifications`, `notes` (may contain internal annotations)

---

## Section 3 — Dual-Track Approval

### Portal side: capture customer identity

`CustomerApproveBoqRequest` (in `BoqDocumentController`) gains a required field:
```java
public record CustomerApproveBoqRequest(
    @NotNull Long customerSignedById,         // which CustomerUser gave approval
    @NotNull @Size(min = 1) List<StageConfigDto> stages
)
```

`BoqDocumentService.recordCustomerApproval()` signature updated:
```java
public BoqDocument recordCustomerApproval(Long documentId, Long customerSignedById, List<StageConfig> stages)
```

**Validation in service before storing:**
1. `customerSignedById` must reference a real `CustomerUser`
2. That `CustomerUser` must be a member of the project (`project_members`)
3. If validation passes, store in `customer_approved_by` (no longer null)

The existing DB column already has the FK: `customer_approved_by BIGINT REFERENCES customer_users(id)`.

### Customer side: digital acknowledgement

**New Flyway migration (V30):**
```sql
ALTER TABLE boq_documents
  ADD COLUMN customer_acknowledged_at  TIMESTAMP,
  ADD COLUMN customer_acknowledged_by  BIGINT REFERENCES customer_users(id);

CREATE INDEX idx_boq_doc_acknowledged
  ON boq_documents(customer_acknowledged_by)
  WHERE customer_acknowledged_by IS NOT NULL;
```

**Acknowledge endpoint behaviour:**
- Resolves document from `{documentId}`
- Verifies the calling `CustomerUser` is a member of the document's project
- Sets `customer_acknowledged_at = NOW()`, `customer_acknowledged_by = customerUserId`
- Idempotent — safe to call multiple times
- Does **not** change `status` field

### Full approval audit trail

| Column | Populated by | Meaning |
|--------|-------------|---------|
| `submitted_by` | Portal user | Who prepared and submitted the BOQ |
| `approved_by` | Portal user | Internal portal sign-off |
| `customer_approved_by` | Portal user (required) | Which CustomerUser gave consent |
| `customer_approved_at` | Portal user | When portal recorded the customer approval |
| `customer_acknowledged_by` | Customer user | Customer's own digital confirmation |
| `customer_acknowledged_at` | Customer user | When customer tapped acknowledge in their app |

---

## Section 4 — `ItemKind` Enum + Quantity Validation

### New enum: `ItemKind`

**Package:** `com.wd.api.model.enums`

```java
public enum ItemKind {
    BASE,       // always included in the contract; quantity must be > 0
    ADDON,      // charged extra if selected; quantity must be > 0
    OPTIONAL,   // customer may choose; quantity = 0 permitted
    EXCLUSION   // explicitly out of scope; quantity = 0 permitted
}
```

### `BoqItem` entity change

`itemKind` field changes from `String` to `ItemKind`:
```java
@Enumerated(EnumType.STRING)
@Column(name = "item_kind", nullable = false, length = 20)
private ItemKind itemKind = ItemKind.BASE;
```

### Updated quantity validation in `BoqService`

```java
private void validateQuantityForKind(BigDecimal quantity, ItemKind kind) {
    if (quantity.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("Quantity cannot be negative");
    }
    if ((kind == ItemKind.BASE || kind == ItemKind.ADDON)
            && quantity.compareTo(BigDecimal.ZERO) == 0) {
        throw new IllegalArgumentException(
            kind.name() + " items must have quantity > 0. Use OPTIONAL or EXCLUSION for zero-quantity scope items.");
    }
}
```

Called from `createBoqItem()` and `updateBoqItem()` — replacing the existing `validateQuantity()` call.

### Flyway migration (V30, same migration as acknowledgement columns)

```sql
-- Normalise any legacy free-text values before adding constraint
UPDATE boq_items SET item_kind = 'BASE'
  WHERE item_kind NOT IN ('BASE','ADDON','OPTIONAL','EXCLUSION');

ALTER TABLE boq_items
  ADD CONSTRAINT chk_item_kind
  CHECK (item_kind IN ('BASE','ADDON','OPTIONAL','EXCLUSION'));
```

---

## Database Migrations

### V30 — Single migration covering Sections 3 and 4

```sql
-- BOQ Document: customer acknowledgement tracking
ALTER TABLE boq_documents
  ADD COLUMN IF NOT EXISTS customer_acknowledged_at  TIMESTAMP,
  ADD COLUMN IF NOT EXISTS customer_acknowledged_by  BIGINT REFERENCES customer_users(id);

CREATE INDEX IF NOT EXISTS idx_boq_doc_acknowledged
  ON boq_documents(customer_acknowledged_by)
  WHERE customer_acknowledged_by IS NOT NULL;

-- BOQ Items: itemKind check constraint
UPDATE boq_items SET item_kind = 'BASE'
  WHERE item_kind IS NULL
     OR item_kind NOT IN ('BASE','ADDON','OPTIONAL','EXCLUSION');

ALTER TABLE boq_items
  ADD CONSTRAINT IF NOT EXISTS chk_item_kind
  CHECK (item_kind IN ('BASE','ADDON','OPTIONAL','EXCLUSION'));
```

No new table is required. All changes are additive (new columns + constraint on existing column).

---

## New Files Summary

| File | Type | Purpose |
|------|------|---------|
| `security/ProjectAccessGuard.java` | New class | Project membership enforcement |
| `controller/CustomerBoqController.java` | New controller | Customer-facing BOQ endpoints |
| `dto/CustomerBoqSummary.java` | New DTO | Customer contract summary |
| `dto/CustomerPaymentStageView.java` | New DTO | Customer payment stage view |
| `model/enums/ItemKind.java` | New enum | Replaces free-text itemKind |
| `db/migration/V30__boq_customer_ack_itemkind.sql` | New migration | DB changes for Sections 3 & 4 |

## Modified Files Summary

| File | Change |
|------|--------|
| `BoqService.java` | Inject + call `ProjectAccessGuard`; replace `validateQuantity` with `validateQuantityForKind` |
| `BoqDocumentService.java` | Inject + call `ProjectAccessGuard`; update `recordCustomerApproval` signature; add acknowledge method |
| `BoqDocumentController.java` | Update `CustomerApproveBoqRequest` to require `customerSignedById` |
| `BoqItem.java` | Change `itemKind` from `String` to `ItemKind` enum |
| `BoqItemResponse.java` | Change `itemKind` field type to `String` (`.name()` from enum — no consumer change) |

---

## Testing Checklist

- [ ] Portal user with `BOQ_VIEW` on a project they're not a member of → 403
- [ ] ADMIN/DIRECTOR can access any project's BOQ
- [ ] Customer user can call `/api/customer/boq/project/{id}/summary` for their project
- [ ] Customer user cannot call summary for a project they're not a member of → 403
- [ ] `customer-approve` without `customerSignedById` → 400
- [ ] `customer-approve` with a `customerSignedById` not on the project → 400
- [ ] `customer_approved_by` is populated (not null) after successful approval
- [ ] Customer `acknowledge` endpoint stores `customer_acknowledged_at` and `customer_acknowledged_by`
- [ ] Re-acknowledging overwrites (idempotent)
- [ ] Creating a BASE item with `quantity = 0` → 400
- [ ] Creating an EXCLUSION item with `quantity = 0` → 200
- [ ] Creating a BOQ item with invalid `itemKind` string → compile error (enum)
- [ ] V30 migration runs cleanly on a DB with existing legacy `item_kind` data

---

## Non-Goals (explicitly deferred)

- Retention calculation (`computeRetention()` wiring) — retention percentage will be configurable; implementation deferred
- `boq_stage_config` table cleanup
- `BoqItem.boq_document_id` ORM linkage
- Change Order and Invoice access scoping
