# BOQ Security, Visibility & Integrity — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add project-level access control, a customer-facing BOQ summary API, dual-track approval identity capture, and `ItemKind` enum enforcement to the BOQ module.

**Architecture:** A new `ProjectAccessGuard` bean enforces project membership in service methods. A new `CustomerBoqController` serves a lean summary to customer JWT holders. The BOQ document approval flow is updated to require a real `customerSignedById`; a new customer acknowledge endpoint stores digital consent. `BoqItem.itemKind` is promoted from free-text `String` to a `ItemKind` enum with DB check constraint.

**Tech Stack:** Spring Boot 3, Spring Security, JPA/Hibernate, PostgreSQL, Flyway, Jakarta Validation

**Spec:** `docs/superpowers/specs/2026-04-14-boq-security-visibility-integrity-design.md`

---

## File Map

### New files
| File | Purpose |
|------|---------|
| `src/main/java/com/wd/api/security/ProjectAccessGuard.java` | Project membership enforcement bean |
| `src/main/java/com/wd/api/model/enums/ItemKind.java` | Enum replacing free-text itemKind |
| `src/main/java/com/wd/api/dto/CustomerBoqSummary.java` | Customer-facing BOQ summary DTO |
| `src/main/java/com/wd/api/dto/CustomerPaymentStageView.java` | Customer-facing payment stage DTO |
| `src/main/java/com/wd/api/controller/CustomerBoqController.java` | Customer-facing BOQ endpoints |
| `src/main/resources/db/migration/V30__boq_customer_ack_itemkind.sql` | Flyway migration |

### Modified files
| File | Change |
|------|--------|
| `src/main/java/com/wd/api/repository/ProjectMemberRepository.java` | Add `existsByProject_IdAndPortalUser_Id` |
| `src/main/java/com/wd/api/model/BoqItem.java` | `itemKind` String → ItemKind enum |
| `src/main/java/com/wd/api/model/BoqDocument.java` | Add `customerAcknowledgedAt`, `customerAcknowledgedBy` fields |
| `src/main/java/com/wd/api/dto/CreateBoqItemRequest.java` | `itemKind` String → ItemKind; relax quantity `@DecimalMin` to allow 0 |
| `src/main/java/com/wd/api/dto/UpdateBoqItemRequest.java` | `itemKind` String → ItemKind |
| `src/main/java/com/wd/api/dto/BoqItemResponse.java` | `itemKind` field: String (`.name()` from enum — no API change) |
| `src/main/java/com/wd/api/service/BoqService.java` | Inject guard; replace `validateQuantity` with `validateQuantityForKind` |
| `src/main/java/com/wd/api/service/BoqDocumentService.java` | Inject guard; update `recordCustomerApproval`; add `acknowledgeDocument` |
| `src/main/java/com/wd/api/controller/BoqDocumentController.java` | `CustomerApproveBoqRequest` gains required `customerSignedById` |
| `src/main/java/com/wd/api/dto/BoqDocumentResponse.java` | Add `customerAcknowledgedAt`, `customerAcknowledgedBy` fields |

---

## Task 1: Flyway Migration V30

**Files:**
- Create: `src/main/resources/db/migration/V30__boq_customer_ack_itemkind.sql`

- [ ] **Step 1: Create the migration file**

```sql
-- =============================================================================
-- V30: BOQ customer acknowledgement + itemKind check constraint
-- =============================================================================

-- 1. Customer acknowledgement columns on boq_documents
ALTER TABLE boq_documents
    ADD COLUMN IF NOT EXISTS customer_acknowledged_at  TIMESTAMP,
    ADD COLUMN IF NOT EXISTS customer_acknowledged_by  BIGINT REFERENCES customer_users(id);

CREATE INDEX IF NOT EXISTS idx_boq_doc_acknowledged
    ON boq_documents(customer_acknowledged_by)
    WHERE customer_acknowledged_by IS NOT NULL;

-- 2. Normalise any legacy free-text item_kind values before constraining
UPDATE boq_items
SET item_kind = 'BASE'
WHERE item_kind IS NULL
   OR item_kind NOT IN ('BASE', 'ADDON', 'OPTIONAL', 'EXCLUSION');

-- 3. Add check constraint (idempotent via DO block)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_item_kind'
          AND conrelid = 'boq_items'::regclass
    ) THEN
        ALTER TABLE boq_items
            ADD CONSTRAINT chk_item_kind
            CHECK (item_kind IN ('BASE', 'ADDON', 'OPTIONAL', 'EXCLUSION'));
    END IF;
END $$;
```

- [ ] **Step 2: Verify migration runs**

Start the application (or run `mvn flyway:migrate`). Check logs for:
```
Successfully applied 1 migration to schema "public", now at version v30
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V30__boq_customer_ack_itemkind.sql
git commit -m "feat: V30 migration - customer acknowledgement columns and itemKind constraint"
```

---

## Task 2: ItemKind Enum

**Files:**
- Create: `src/main/java/com/wd/api/model/enums/ItemKind.java`
- Modify: `src/main/java/com/wd/api/model/BoqItem.java`
- Modify: `src/main/java/com/wd/api/dto/CreateBoqItemRequest.java`
- Modify: `src/main/java/com/wd/api/dto/UpdateBoqItemRequest.java`
- Modify: `src/main/java/com/wd/api/dto/BoqItemResponse.java`

- [ ] **Step 1: Create ItemKind enum**

Create `src/main/java/com/wd/api/model/enums/ItemKind.java`:

```java
package com.wd.api.model.enums;

public enum ItemKind {
    /** Always included in the contract; quantity must be > 0 */
    BASE,
    /** Charged extra if selected; quantity must be > 0 */
    ADDON,
    /** Customer may choose; quantity = 0 permitted */
    OPTIONAL,
    /** Explicitly out of scope; listed for transparency; quantity = 0 permitted */
    EXCLUSION
}
```

- [ ] **Step 2: Update BoqItem entity**

In `src/main/java/com/wd/api/model/BoqItem.java`, find the `itemKind` field (around line 91) and replace:

```java
    @Column(name = "item_kind", nullable = false, length = 20)
    private String itemKind = "BASE";
```

with:

```java
    @Enumerated(EnumType.STRING)
    @Column(name = "item_kind", nullable = false, length = 20)
    private ItemKind itemKind = ItemKind.BASE;
```

Add the import at the top of `BoqItem.java`:
```java
import com.wd.api.model.enums.ItemKind;
```

Update the getter and setter (around lines 261–262):
```java
    public ItemKind getItemKind() { return itemKind != null ? itemKind : ItemKind.BASE; }
    public void setItemKind(ItemKind itemKind) { this.itemKind = itemKind != null ? itemKind : ItemKind.BASE; }
```

- [ ] **Step 3: Update CreateBoqItemRequest**

Replace the entire file `src/main/java/com/wd/api/dto/CreateBoqItemRequest.java`:

```java
package com.wd.api.dto;

import com.wd.api.model.enums.ItemKind;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateBoqItemRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        Long categoryId,

        Long workTypeId,

        @Size(max = 50, message = "Item code must not exceed 50 characters")
        String itemCode,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotBlank(message = "Unit is required")
        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", message = "Quantity cannot be negative")
        BigDecimal quantity,

        @NotNull(message = "Unit rate is required")
        @DecimalMin(value = "0.0", message = "Unit rate cannot be negative")
        BigDecimal unitRate,

        Long materialId,

        @Size(max = 5000, message = "Specifications must not exceed 5000 characters")
        String specifications,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        // Defaults to BASE when null
        ItemKind itemKind
) {}
```

Note: `@DecimalMin` loosened to `"0.0"` — service layer enforces `quantity > 0` for BASE/ADDON.

- [ ] **Step 4: Update UpdateBoqItemRequest**

Replace the entire file `src/main/java/com/wd/api/dto/UpdateBoqItemRequest.java`:

```java
package com.wd.api.dto;

import com.wd.api.model.enums.ItemKind;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateBoqItemRequest(
        Long categoryId,

        Long workTypeId,

        @Size(max = 50, message = "Item code must not exceed 50 characters")
        String itemCode,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        @DecimalMin(value = "0.0", message = "Quantity cannot be negative")
        BigDecimal quantity,

        @DecimalMin(value = "0.0", message = "Unit rate cannot be negative")
        BigDecimal unitRate,

        Long materialId,

        @Size(max = 5000, message = "Specifications must not exceed 5000 characters")
        String specifications,

        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes,

        // null = no change
        ItemKind itemKind
) {}
```

- [ ] **Step 5: Update BoqItemResponse**

In `src/main/java/com/wd/api/dto/BoqItemResponse.java`, the record field `itemKind` stays as `String` (for backward API compatibility). No change needed to the record signature. The `fromEntity` call already does:
```java
item.getItemKind()
```
Update that line to:
```java
item.getItemKind() != null ? item.getItemKind().name() : "BASE"
```

Also add import to `BoqItem.java` where `getItemKind()` is used — already handled in Step 2.

- [ ] **Step 6: Build to confirm no compile errors**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/wd/api/model/enums/ItemKind.java \
        src/main/java/com/wd/api/model/BoqItem.java \
        src/main/java/com/wd/api/dto/CreateBoqItemRequest.java \
        src/main/java/com/wd/api/dto/UpdateBoqItemRequest.java \
        src/main/java/com/wd/api/dto/BoqItemResponse.java
git commit -m "feat: promote itemKind from String to ItemKind enum"
```

---

## Task 3: ProjectAccessGuard + Repository Method

**Files:**
- Create: `src/main/java/com/wd/api/security/ProjectAccessGuard.java`
- Modify: `src/main/java/com/wd/api/repository/ProjectMemberRepository.java`

- [ ] **Step 1: Add portal membership method to ProjectMemberRepository**

In `src/main/java/com/wd/api/repository/ProjectMemberRepository.java`, add one method after the existing `existsByProject_IdAndCustomerUser_Id`:

```java
    /** Check if a PortalUser is a member of a specific project. */
    boolean existsByProject_IdAndPortalUser_Id(Long projectId, Long portalUserId);
```

- [ ] **Step 2: Create ProjectAccessGuard**

Create `src/main/java/com/wd/api/security/ProjectAccessGuard.java`:

```java
package com.wd.api.security;

import com.wd.api.repository.ProjectMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Enforces project-level access control for BOQ operations.
 *
 * Portal rules:
 *   ADMIN and DIRECTOR roles bypass the membership check (global access).
 *   All other roles must appear in project_members for the requested project.
 *
 * Customer rules:
 *   Customer users must appear in project_members for the requested project.
 */
@Component
public class ProjectAccessGuard {

    private static final java.util.Set<String> GLOBAL_ACCESS_ROLES =
            java.util.Set.of("ROLE_ADMIN", "ROLE_DIRECTOR");

    private final ProjectMemberRepository projectMemberRepository;

    public ProjectAccessGuard(ProjectMemberRepository projectMemberRepository) {
        this.projectMemberRepository = projectMemberRepository;
    }

    /**
     * Verifies that the current portal user may access the given project.
     * Reads authorities from the current SecurityContext — no extra DB query for role lookup.
     *
     * @throws AccessDeniedException (HTTP 403) if access is not permitted
     */
    public void verifyPortalAccess(Long portalUserId, Long projectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (hasGlobalAccess(auth)) {
            return;
        }
        if (!projectMemberRepository.existsByProject_IdAndPortalUser_Id(projectId, portalUserId)) {
            throw new AccessDeniedException("You do not have access to project " + projectId);
        }
    }

    /**
     * Verifies that the given customer user may access the given project.
     *
     * @throws AccessDeniedException (HTTP 403) if not a project member
     */
    public void verifyCustomerAccess(Long customerUserId, Long projectId) {
        if (!projectMemberRepository.existsByProject_IdAndCustomerUser_Id(projectId, customerUserId)) {
            throw new AccessDeniedException("You do not have access to project " + projectId);
        }
    }

    private boolean hasGlobalAccess(Authentication auth) {
        if (auth == null) return false;
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(GLOBAL_ACCESS_ROLES::contains);
    }
}
```

- [ ] **Step 3: Build to confirm no compile errors**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/wd/api/security/ProjectAccessGuard.java \
        src/main/java/com/wd/api/repository/ProjectMemberRepository.java
git commit -m "feat: add ProjectAccessGuard and portal membership repository method"
```

---

## Task 4: Wire ProjectAccessGuard into BoqService

**Files:**
- Modify: `src/main/java/com/wd/api/service/BoqService.java`

- [ ] **Step 1: Inject ProjectAccessGuard and update constructor**

In `BoqService.java`, add the field after existing fields (around line 33):
```java
    private final ProjectAccessGuard projectAccessGuard;
```

Update the constructor to include it:
```java
    public BoqService(BoqItemRepository boqItemRepository,
                      BoqWorkTypeRepository boqWorkTypeRepository,
                      CustomerProjectRepository customerProjectRepository,
                      BoqCategoryRepository categoryRepository,
                      MaterialRepository materialRepository,
                      BoqAuditService auditService,
                      ProjectAccessGuard projectAccessGuard) {
        this.boqItemRepository = boqItemRepository;
        this.boqWorkTypeRepository = boqWorkTypeRepository;
        this.customerProjectRepository = customerProjectRepository;
        this.categoryRepository = categoryRepository;
        this.materialRepository = materialRepository;
        this.auditService = auditService;
        this.projectAccessGuard = projectAccessGuard;
    }
```

Add import:
```java
import com.wd.api.security.ProjectAccessGuard;
```

- [ ] **Step 2: Add validateQuantityForKind and replace validateQuantity calls**

Replace the existing `validateQuantity` private method (around line 496) with:

```java
    private void validateQuantityForKind(BigDecimal quantity, ItemKind kind) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if ((ItemKind.BASE == kind || ItemKind.ADDON == kind)
                && quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(
                kind.name() + " items must have quantity > 0. " +
                "Use OPTIONAL or EXCLUSION for zero-quantity scope items.");
        }
    }
```

Add import:
```java
import com.wd.api.model.enums.ItemKind;
```

In `createBoqItem()` (around line 67), replace the call to `validateQuantity` with:
```java
        ItemKind kind = request.itemKind() != null ? request.itemKind() : ItemKind.BASE;
        validateQuantityForKind(request.quantity(), kind);
```

In `updateBoqItem()` (around line 122), replace the call to `validateQuantity(request.quantity())` with:
```java
            ItemKind resolvedKind = request.itemKind() != null ? request.itemKind() : item.getItemKind();
            validateQuantityForKind(request.quantity(), resolvedKind);
```

Also in `createBoqItem()`, update the `item.setItemKind` line (around line 74):
```java
        item.setItemKind(kind);
```

- [ ] **Step 3: Add guard calls to project-scoped methods**

At the top of `createBoqItem()`, after fetching the project (after line 54 where project is loaded), add:
```java
        projectAccessGuard.verifyPortalAccess(userId, request.projectId());
```

At the top of `getProjectBoqPaged()` (before the filter is built), add a `userId` parameter and guard call. The method signature becomes:
```java
    public Page<BoqItemResponse> getProjectBoqPaged(Long projectId, int page, int size,
                                                     Long workTypeId, Long categoryId,
                                                     String status, Long userId) {
```
Add at top of method body:
```java
        projectAccessGuard.verifyPortalAccess(userId, projectId);
```

At the top of `getFinancialSummary()`, add `userId` parameter and guard:
```java
    public BoqFinancialSummary getFinancialSummary(Long projectId, Long userId) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
```

At the top of `getProjectBoq()`, add `userId` parameter and guard:
```java
    public List<BoqItemResponse> getProjectBoq(Long projectId, Long userId) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
```

For single-item operations (`updateBoqItem`, `softDeleteBoqItem`, `approveBoqItem`, `lockBoqItem`, `markAsCompleted`, `recordExecution`, `recordBilling`, `correctExecution`), add the guard after the item is loaded (use `item.getProject().getId()`):
```java
        projectAccessGuard.verifyPortalAccess(userId, item.getProject().getId());
```

- [ ] **Step 4: Update BoqController to pass userId to changed method signatures**

In `BoqController.java`, update these calls:

`getProjectBoq` call (around line 252):
```java
Page<BoqItemResponse> items = boqService.getProjectBoqPaged(
        projectId, page, size, workTypeId, categoryId, status, getCurrentUserId());
```

`getFinancialSummary` call (around line 264):
```java
BoqFinancialSummary summary = boqService.getFinancialSummary(projectId, getCurrentUserId());
```

- [ ] **Step 5: Build to confirm no compile errors**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wd/api/service/BoqService.java \
        src/main/java/com/wd/api/controller/BoqController.java
git commit -m "feat: wire ProjectAccessGuard and ItemKind validation into BoqService"
```

---

## Task 5: Wire ProjectAccessGuard into BoqDocumentService + Dual-Track Approval

**Files:**
- Modify: `src/main/java/com/wd/api/model/BoqDocument.java`
- Modify: `src/main/java/com/wd/api/service/BoqDocumentService.java`
- Modify: `src/main/java/com/wd/api/controller/BoqDocumentController.java`
- Modify: `src/main/java/com/wd/api/dto/BoqDocumentResponse.java`

- [ ] **Step 1: Add acknowledgement fields to BoqDocument entity**

In `src/main/java/com/wd/api/model/BoqDocument.java`, add two fields after the `rejectionReason` field (around line 78):

```java
    // Customer digital acknowledgement (optional — set by customer app)
    @Column(name = "customer_acknowledged_at")
    private LocalDateTime customerAcknowledgedAt;

    @Column(name = "customer_acknowledged_by")
    private Long customerAcknowledgedBy;
```

Add getters and setters at the bottom of the getters/setters block:
```java
    public LocalDateTime getCustomerAcknowledgedAt() { return customerAcknowledgedAt; }
    public void setCustomerAcknowledgedAt(LocalDateTime customerAcknowledgedAt) { this.customerAcknowledgedAt = customerAcknowledgedAt; }

    public Long getCustomerAcknowledgedBy() { return customerAcknowledgedBy; }
    public void setCustomerAcknowledgedBy(Long customerAcknowledgedBy) { this.customerAcknowledgedBy = customerAcknowledgedBy; }
```

- [ ] **Step 2: Update BoqDocumentResponse to include acknowledgement fields**

In `src/main/java/com/wd/api/dto/BoqDocumentResponse.java`, read its current content then add the two new fields and populate them in the `from(BoqDocument)` factory method. The fields to add:

```java
    LocalDateTime customerAcknowledgedAt,
    Long customerAcknowledgedBy
```

In the `from()` factory, map:
```java
    doc.getCustomerAcknowledgedAt(),
    doc.getCustomerAcknowledgedBy()
```

- [ ] **Step 3: Inject guard and CustomerUserRepository into BoqDocumentService**

Add fields to `BoqDocumentService`:
```java
    private final ProjectAccessGuard projectAccessGuard;
    private final CustomerUserRepository customerUserRepository;
```

Update constructor signature:
```java
    public BoqDocumentService(BoqDocumentRepository boqDocumentRepository,
                               BoqItemRepository boqItemRepository,
                               PaymentStageRepository paymentStageRepository,
                               CustomerProjectRepository projectRepository,
                               PortalUserRepository portalUserRepository,
                               ProjectAccessGuard projectAccessGuard,
                               CustomerUserRepository customerUserRepository) {
        // ... existing assignments ...
        this.projectAccessGuard = projectAccessGuard;
        this.customerUserRepository = customerUserRepository;
    }
```

Add imports:
```java
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.security.ProjectAccessGuard;
```

- [ ] **Step 4: Add guard calls to BoqDocumentService methods**

At the top of `createDocument()`, after loading the project:
```java
        projectAccessGuard.verifyPortalAccess(userId, projectId);
```

At the top of `submitForApproval()`, after loading the doc:
```java
        projectAccessGuard.verifyPortalAccess(userId, doc.getProject().getId());
```

At the top of `approveInternally()`, after loading the doc:
```java
        projectAccessGuard.verifyPortalAccess(userId, doc.getProject().getId());
```

At the top of `reject()`, after loading the doc:
```java
        projectAccessGuard.verifyPortalAccess(rejectorId, doc.getProject().getId());
```

For `getProjectDocuments()` and `getApprovedDocument()`, add `userId` parameter and guard:
```java
    public List<BoqDocument> getProjectDocuments(Long projectId, Long userId) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
        return boqDocumentRepository.findActiveByProjectId(projectId);
    }

    public BoqDocument getApprovedDocument(Long projectId, Long userId) {
        projectAccessGuard.verifyPortalAccess(userId, projectId);
        return boqDocumentRepository.findApprovedByProjectId(projectId)
                .orElseThrow(() -> new IllegalStateException(
                    "No approved BOQ document found for project " + projectId));
    }
```

- [ ] **Step 5: Update recordCustomerApproval to require customerSignedById**

Replace the `recordCustomerApproval` signature and add validation:

```java
    public BoqDocument recordCustomerApproval(Long documentId,
                                               Long customerSignedById,
                                               List<StageConfig> stageConfigs) {
        BoqDocument doc = getDocument(documentId);

        if (doc.getStatus() != BoqDocumentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "BOQ must be PENDING_APPROVAL for customer approval. Current: " + doc.getStatus());
        }

        // Validate customerSignedById
        if (customerSignedById == null) {
            throw new IllegalArgumentException("customerSignedById is required");
        }
        com.wd.api.model.CustomerUser signer = customerUserRepository.findById(customerSignedById)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Customer user not found: " + customerSignedById));
        // Verify they are a member of the project
        projectAccessGuard.verifyCustomerAccess(customerSignedById, doc.getProject().getId());

        validateStagePercentages(stageConfigs);

        doc.setStatus(BoqDocumentStatus.APPROVED);
        doc.setCustomerApprovedAt(LocalDateTime.now());
        doc.setCustomerApprovedBy(customerSignedById);   // now always non-null
        BoqDocument saved = boqDocumentRepository.save(doc);

        generatePaymentStages(saved, stageConfigs);

        logger.info("BOQ document {} approved for project {}. Signed by customer user {}. {} payment stages generated.",
                documentId, doc.getProject().getId(), customerSignedById, stageConfigs.size());

        return saved;
    }
```

- [ ] **Step 6: Add acknowledgeDocument method to BoqDocumentService**

Add a new public method:

```java
    /**
     * Records the customer's digital acknowledgement of the BOQ.
     * Idempotent — safe to call multiple times; overwrites with latest timestamp.
     * Does not change the document status.
     */
    public BoqDocument acknowledgeDocument(Long documentId, Long customerUserId) {
        BoqDocument doc = getDocument(documentId);
        projectAccessGuard.verifyCustomerAccess(customerUserId, doc.getProject().getId());

        doc.setCustomerAcknowledgedAt(LocalDateTime.now());
        doc.setCustomerAcknowledgedBy(customerUserId);
        return boqDocumentRepository.save(doc);
    }
```

- [ ] **Step 7: Update BoqDocumentController**

Update `CustomerApproveBoqRequest` record inside `BoqDocumentController.java`:

```java
    public record CustomerApproveBoqRequest(
            @NotNull(message = "customerSignedById is required") Long customerSignedById,
            @NotNull @Size(min = 1) List<StageConfigDto> stages
    ) {}
```

Update the `customerApprove` method body to pass `customerSignedById`:
```java
        BoqDocument doc = boqDocumentService.recordCustomerApproval(
                id, request.customerSignedById(), stages);
```

Update `getProjectDocuments` call to pass userId:
```java
            List<BoqDocumentResponse> docs = boqDocumentService.getProjectDocuments(projectId, getCurrentUserId())
                    .stream().map(BoqDocumentResponse::from).toList();
```

Update `getApproved` call:
```java
            return ResponseEntity.ok(ApiResponse.success("OK",
                    BoqDocumentResponse.from(boqDocumentService.getApprovedDocument(projectId, getCurrentUserId()))));
```

- [ ] **Step 8: Build to confirm no compile errors**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/wd/api/model/BoqDocument.java \
        src/main/java/com/wd/api/service/BoqDocumentService.java \
        src/main/java/com/wd/api/controller/BoqDocumentController.java \
        src/main/java/com/wd/api/dto/BoqDocumentResponse.java
git commit -m "feat: require customerSignedById on BOQ approval; add customer acknowledgement; wire ProjectAccessGuard into BoqDocumentService"
```

---

## Task 6: Customer-Facing BOQ Controller + DTOs

**Files:**
- Create: `src/main/java/com/wd/api/dto/CustomerBoqSummary.java`
- Create: `src/main/java/com/wd/api/dto/CustomerPaymentStageView.java`
- Create: `src/main/java/com/wd/api/controller/CustomerBoqController.java`

- [ ] **Step 1: Create CustomerPaymentStageView DTO**

Create `src/main/java/com/wd/api/dto/CustomerPaymentStageView.java`:

```java
package com.wd.api.dto;

import com.wd.api.model.PaymentStage;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Customer-facing view of a single payment stage.
 * Deliberately excludes: boqValueSnapshot, retentionHeld, retentionPct,
 * certifiedBy, appliedCreditAmount, netPayableAmount, paidAmount, internal user IDs.
 */
public record CustomerPaymentStageView(
        Integer stageNumber,
        String stageName,
        BigDecimal stageAmountExGst,
        BigDecimal gstAmount,
        BigDecimal stageAmountInclGst,
        BigDecimal stagePercentage,
        String status,
        LocalDate dueDate,
        String milestoneDescription
) {
    public static CustomerPaymentStageView from(PaymentStage stage) {
        return new CustomerPaymentStageView(
                stage.getStageNumber(),
                stage.getStageName(),
                stage.getStageAmountExGst(),
                stage.getGstAmount(),
                stage.getStageAmountInclGst(),
                stage.getStagePercentage(),
                stage.getStatus() != null ? stage.getStatus().name() : null,
                stage.getDueDate(),
                stage.getMilestoneDescription()
        );
    }
}
```

- [ ] **Step 2: Create CustomerBoqSummary DTO**

Create `src/main/java/com/wd/api/dto/CustomerBoqSummary.java`:

```java
package com.wd.api.dto;

import com.wd.api.model.BoqDocument;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer-facing summary of a BOQ document.
 * Deliberately excludes: unit rates, execution quantities, billing data,
 * cost-to-complete, internal notes, and internal user IDs.
 */
public record CustomerBoqSummary(
        Long documentId,
        Long projectId,
        String projectName,
        BigDecimal totalValueExGst,
        BigDecimal totalGstAmount,
        BigDecimal totalValueInclGst,
        BigDecimal gstRate,
        String status,
        Integer revisionNumber,
        LocalDateTime approvedAt,
        LocalDateTime acknowledgedAt,
        boolean pendingAcknowledgement,
        List<CustomerPaymentStageView> paymentStages
) {
    public static CustomerBoqSummary from(BoqDocument doc, List<CustomerPaymentStageView> stages) {
        boolean pending = doc.isPendingApproval() && doc.getCustomerAcknowledgedAt() == null;
        return new CustomerBoqSummary(
                doc.getId(),
                doc.getProject().getId(),
                doc.getProject().getName(),
                doc.getTotalValueExGst(),
                doc.getTotalGstAmount(),
                doc.getTotalValueInclGst(),
                doc.getGstRate(),
                doc.getStatus() != null ? doc.getStatus().name() : null,
                doc.getRevisionNumber(),
                doc.getCustomerApprovedAt(),
                doc.getCustomerAcknowledgedAt(),
                pending,
                stages
        );
    }
}
```

- [ ] **Step 3: Add PaymentStageRepository query method**

In `src/main/java/com/wd/api/repository/PaymentStageRepository.java`, verify or add:
```java
    List<PaymentStage> findByBoqDocument_IdOrderByStageNumberAsc(Long documentId);
    List<PaymentStage> findByProject_IdOrderByStageNumberAsc(Long projectId);
```

- [ ] **Step 4: Create CustomerBoqController**

Create `src/main/java/com/wd/api/controller/CustomerBoqController.java`:

```java
package com.wd.api.controller;

import com.wd.api.dto.ApiResponse;
import com.wd.api.dto.CustomerBoqSummary;
import com.wd.api.dto.CustomerPaymentStageView;
import com.wd.api.model.BoqDocument;
import com.wd.api.model.CustomerUser;
import com.wd.api.repository.BoqDocumentRepository;
import com.wd.api.repository.CustomerUserRepository;
import com.wd.api.repository.PaymentStageRepository;
import com.wd.api.security.ProjectAccessGuard;
import com.wd.api.service.BoqDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Customer-facing BOQ endpoints.
 * Accessible only to authenticated CustomerUser principals (ROLE_CUSTOMER).
 * Returns lean DTOs — no unit rates, no execution data, no internal user IDs.
 */
@RestController
@RequestMapping("/api/customer/boq")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerBoqController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerBoqController.class);

    private final BoqDocumentRepository boqDocumentRepository;
    private final PaymentStageRepository paymentStageRepository;
    private final CustomerUserRepository customerUserRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final BoqDocumentService boqDocumentService;

    public CustomerBoqController(BoqDocumentRepository boqDocumentRepository,
                                  PaymentStageRepository paymentStageRepository,
                                  CustomerUserRepository customerUserRepository,
                                  ProjectAccessGuard projectAccessGuard,
                                  BoqDocumentService boqDocumentService) {
        this.boqDocumentRepository = boqDocumentRepository;
        this.paymentStageRepository = paymentStageRepository;
        this.customerUserRepository = customerUserRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.boqDocumentService = boqDocumentService;
    }

    /**
     * Returns the BOQ summary for a project.
     * Shows the approved document if one exists; falls back to the latest
     * non-rejected draft/pending document so the customer sees progress.
     */
    @GetMapping("/project/{projectId}/summary")
    public ResponseEntity<ApiResponse<CustomerBoqSummary>> getSummary(@PathVariable Long projectId) {
        try {
            Long customerUserId = getCurrentCustomerUserId();
            projectAccessGuard.verifyCustomerAccess(customerUserId, projectId);

            // Prefer approved document; fall back to latest active document
            Optional<BoqDocument> docOpt = boqDocumentRepository.findApprovedByProjectId(projectId);
            if (docOpt.isEmpty()) {
                List<BoqDocument> active = boqDocumentRepository.findActiveByProjectId(projectId);
                docOpt = active.stream()
                        .filter(d -> !"REJECTED".equals(d.getStatus().name()))
                        .max(java.util.Comparator.comparing(BoqDocument::getRevisionNumber));
            }
            if (docOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No BOQ available yet", null));
            }

            BoqDocument doc = docOpt.get();
            List<CustomerPaymentStageView> stages = paymentStageRepository
                    .findByBoqDocument_IdOrderByStageNumberAsc(doc.getId())
                    .stream().map(CustomerPaymentStageView::from).toList();

            return ResponseEntity.ok(ApiResponse.success("OK", CustomerBoqSummary.from(doc, stages)));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ summary for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    /**
     * Returns the payment stages for a project's approved BOQ.
     * Returns empty list if no approved BOQ exists yet.
     */
    @GetMapping("/project/{projectId}/payment-stages")
    public ResponseEntity<ApiResponse<List<CustomerPaymentStageView>>> getPaymentStages(
            @PathVariable Long projectId) {
        try {
            Long customerUserId = getCurrentCustomerUserId();
            projectAccessGuard.verifyCustomerAccess(customerUserId, projectId);

            List<CustomerPaymentStageView> stages = paymentStageRepository
                    .findByProject_IdOrderByStageNumberAsc(projectId)
                    .stream().map(CustomerPaymentStageView::from).toList();

            return ResponseEntity.ok(ApiResponse.success("OK", stages));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch payment stages for project {}", projectId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    /**
     * Records the customer's digital acknowledgement of a BOQ document.
     * Idempotent — safe to call multiple times.
     */
    @PatchMapping("/documents/{documentId}/acknowledge")
    public ResponseEntity<ApiResponse<CustomerBoqSummary>> acknowledge(@PathVariable Long documentId) {
        try {
            Long customerUserId = getCurrentCustomerUserId();
            BoqDocument doc = boqDocumentService.acknowledgeDocument(documentId, customerUserId);
            List<CustomerPaymentStageView> stages = paymentStageRepository
                    .findByBoqDocument_IdOrderByStageNumberAsc(doc.getId())
                    .stream().map(CustomerPaymentStageView::from).toList();
            return ResponseEntity.ok(ApiResponse.success("BOQ acknowledged", CustomerBoqSummary.from(doc, stages)));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to acknowledge BOQ document {}", documentId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("An internal error occurred"));
        }
    }

    /**
     * Resolves the authenticated CustomerUser from the security context.
     * The JwtAuthenticationFilter stores the customer's email as principal for CUSTOMER tokens.
     */
    private Long getCurrentCustomerUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
        String email = (String) auth.getPrincipal();
        return customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Customer user not found for email: " + email))
                .getId();
    }
}
```

- [ ] **Step 5: Build to confirm no compile errors**

```bash
mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wd/api/dto/CustomerBoqSummary.java \
        src/main/java/com/wd/api/dto/CustomerPaymentStageView.java \
        src/main/java/com/wd/api/controller/CustomerBoqController.java
git commit -m "feat: add CustomerBoqController with summary, payment stages, and acknowledge endpoints"
```

---

## Task 7: Verify Security Config Allows /api/customer/** 

**Files:**
- Read: `src/main/java/com/wd/api/security/SecurityConfig.java`

- [ ] **Step 1: Check SecurityConfig permits /api/customer/** routes for ROLE_CUSTOMER**

Read the security config and verify that `/api/customer/**` is permitted for authenticated customers. If not, add the rule.

Look for a block like:
```java
.requestMatchers("/api/customer/**").hasRole("CUSTOMER")
```

If the pattern `.requestMatchers("/api/customer/**").authenticated()` exists and the JWT filter already sets `ROLE_CUSTOMER`, that is sufficient. If `/api/customer/**` is not explicitly listed, add:
```java
.requestMatchers("/api/customer/**").hasRole("CUSTOMER")
```
in the `authorizeHttpRequests` chain before the catch-all rule.

- [ ] **Step 2: Build and start application**

```bash
mvn spring-boot:run
```

Check startup logs for Flyway migration V30:
```
Successfully applied 1 migration to schema "public", now at version v30
```

- [ ] **Step 3: Smoke test with curl (portal user, no project membership)**

Replace `{TOKEN}` with a portal user JWT that does NOT have ADMIN/DIRECTOR role and is NOT a member of project 1:

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer {TOKEN}" \
  http://localhost:8080/api/boq/project/1/financial-summary
```

Expected: `403`

- [ ] **Step 4: Smoke test customer summary endpoint**

Replace `{CUSTOMER_TOKEN}` with a valid customer JWT and use a `{projectId}` the customer belongs to:

```bash
curl -s \
  -H "Authorization: Bearer {CUSTOMER_TOKEN}" \
  http://localhost:8080/api/customer/boq/project/{projectId}/summary
```

Expected: `200` with JSON containing `documentId`, `status`, `totalValueInclGst`.

- [ ] **Step 5: Commit security config change (if any was needed)**

```bash
git add src/main/java/com/wd/api/security/SecurityConfig.java
git commit -m "fix: ensure /api/customer/** routes require ROLE_CUSTOMER"
```

---

## Self-Review

### Spec coverage check

| Spec requirement | Task |
|-----------------|------|
| ProjectAccessGuard bean | Task 3 |
| ADMIN/DIRECTOR bypass | Task 3 (GLOBAL_ACCESS_ROLES set) |
| Portal membership check | Task 3 + Tasks 4/5 |
| Customer membership check | Task 3 + Task 6 |
| `CustomerBoqSummary` DTO | Task 6 |
| `CustomerPaymentStageView` DTO | Task 6 |
| `/api/customer/boq/project/{id}/summary` | Task 6 |
| `/api/customer/boq/project/{id}/payment-stages` | Task 6 |
| `/api/customer/boq/documents/{id}/acknowledge` | Task 6 |
| `customerSignedById` required on approve | Task 5 |
| `customer_approved_by` non-null after approval | Task 5 |
| `customer_acknowledged_at` + `customer_acknowledged_by` | Tasks 1 + 5 |
| `ItemKind` enum | Task 2 |
| `quantity > 0` for BASE/ADDON | Task 4 |
| V30 migration | Task 1 |
| Excluded fields from customer DTOs | Task 6 (Step 1 + 2 comments) |
| Retention deferred | Not implemented — correct |

All spec requirements covered. No gaps found.
