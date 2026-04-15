# Move CustomerBoqController to Customer API — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move 3 customer-facing BOQ endpoints from portal API into customer API, eliminating the JWT coupling that forces portal API to verify customer tokens.

**Architecture:** The customer API already has `CustomerBoqController` at `/api/projects/{projectId}/boq/...`. We add the 3 missing endpoints there using the same URL pattern and `DashboardService.getProjectByUuidAndEmail()` for access control. The portal API is cleaned up by deleting the dead controller, its DTOs, CUSTOMER token handling in the JWT filter, and related security rules.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Spring Security (stateless JWT), Maven

---

## File Structure

**Customer API — Modified:**
- `wd_customer_api/src/main/java/com/wd/custapi/model/BoqDocument.java` — add `customerAcknowledgedAt` + `customerAcknowledgedBy` fields and setters (currently missing; needed for acknowledge write)
- `wd_customer_api/src/main/java/com/wd/custapi/repository/BoqDocumentRepository.java` — add `findTopByProjectIdAndStatusNotOrderByRevisionNumberDesc` (for latest non-rejected BOQ fallback)
- `wd_customer_api/src/main/java/com/wd/custapi/controller/CustomerBoqController.java` — add 3 new endpoints + `CustomerUserRepository` dependency

**Portal API — Deleted:**
- `wd_portal_api/src/main/java/com/wd/api/controller/CustomerBoqController.java`
- `wd_portal_api/src/main/java/com/wd/api/dto/CustomerBoqSummary.java`
- `wd_portal_api/src/main/java/com/wd/api/dto/CustomerPaymentStageView.java`

**Portal API — Modified:**
- `wd_portal_api/src/main/java/com/wd/api/security/JwtAuthenticationFilter.java` — remove CUSTOMER token branch + `handleCustomerAuthentication()` method
- `wd_portal_api/src/main/java/com/wd/api/security/ProjectAccessGuard.java` — rename `verifyCustomerAccess` → `verifyCustomerMembership`
- `wd_portal_api/src/main/java/com/wd/api/service/BoqDocumentService.java` — update caller to use `verifyCustomerMembership`, remove `acknowledgeDocument()`
- `wd_portal_api/src/main/java/com/wd/api/security/SecurityConfig.java` — remove dead `/api/customer/**` matchers

---

## Tasks

### Task 1: Customer API — Add acknowledge fields to BoqDocument

**Files:**
- Modify: `wd_customer_api/src/main/java/com/wd/custapi/model/BoqDocument.java`

The model is currently missing `customer_acknowledged_at` and `customer_acknowledged_by` columns (portal API has them; both APIs share the same DB table).

- [ ] **Step 1: Add the two new JPA columns after the existing `createdAt` field (line 59)**

```java
@Column(name = "customer_acknowledged_at")
private LocalDateTime customerAcknowledgedAt;

@Column(name = "customer_acknowledged_by")
private Long customerAcknowledgedBy;
```

- [ ] **Step 2: Add getters and setters for these fields after the existing `getCreatedAt()` getter**

```java
public LocalDateTime getCustomerAcknowledgedAt() { return customerAcknowledgedAt; }
public void setCustomerAcknowledgedAt(LocalDateTime customerAcknowledgedAt) { this.customerAcknowledgedAt = customerAcknowledgedAt; }
public Long getCustomerAcknowledgedBy() { return customerAcknowledgedBy; }
public void setCustomerAcknowledgedBy(Long customerAcknowledgedBy) { this.customerAcknowledgedBy = customerAcknowledgedBy; }
```

- [ ] **Step 3: Compile to verify no errors**

```bash
cd "wd_customer_api" && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add wd_customer_api/src/main/java/com/wd/custapi/model/BoqDocument.java
git commit -m "feat(customer-api): add customerAcknowledgedAt/By fields to BoqDocument"
```

---

### Task 2: Customer API — Add fallback repository query to BoqDocumentRepository

**Files:**
- Modify: `wd_customer_api/src/main/java/com/wd/custapi/repository/BoqDocumentRepository.java`

The `getSummary` endpoint needs: prefer APPROVED doc; fall back to latest non-REJECTED doc. The existing `findByProjectIdAndStatus` handles the APPROVED case. We need a new derived query for the fallback.

- [ ] **Step 1: Add the derived query method to the interface**

Full file after edit:

```java
package com.wd.custapi.repository;

import com.wd.custapi.model.BoqDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoqDocumentRepository extends JpaRepository<BoqDocument, Long> {

    Optional<BoqDocument> findByProjectIdAndStatus(Long projectId, String status);

    Optional<BoqDocument> findTopByProjectIdOrderByRevisionNumberDesc(Long projectId);

    Optional<BoqDocument> findTopByProjectIdAndStatusNotOrderByRevisionNumberDesc(Long projectId, String status);
}
```

- [ ] **Step 2: Compile to verify**

```bash
cd "wd_customer_api" && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add wd_customer_api/src/main/java/com/wd/custapi/repository/BoqDocumentRepository.java
git commit -m "feat(customer-api): add findTopByProjectIdAndStatusNot query to BoqDocumentRepository"
```

---

### Task 3: Customer API — Add 3 endpoints to CustomerBoqController

**Files:**
- Modify: `wd_customer_api/src/main/java/com/wd/custapi/controller/CustomerBoqController.java`

New endpoints:
- `GET /api/projects/{projectId}/boq/summary` — approved (or latest non-rejected) BOQ with embedded payment stages and `pendingAcknowledgement` flag
- `GET /api/projects/{projectId}/boq/payment-stages` — lean payment stage list (customer-safe fields only)
- `PATCH /api/projects/{projectId}/boq/documents/{documentId}/acknowledge` — record customer acknowledgement, return updated summary

- [ ] **Step 1: Add `CustomerUserRepository` import and inject it into the controller**

Add to imports:
```java
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.repository.CustomerUserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
```

Add field:
```java
private final CustomerUserRepository customerUserRepository;
```

Replace existing constructor with:
```java
public CustomerBoqController(DashboardService dashboardService,
                               BoqDocumentRepository boqDocumentRepository,
                               PaymentStageRepository paymentStageRepository,
                               CustomerChangeOrderService changeOrderService,
                               CustomerUserRepository customerUserRepository) {
    this.dashboardService = dashboardService;
    this.boqDocumentRepository = boqDocumentRepository;
    this.paymentStageRepository = paymentStageRepository;
    this.changeOrderService = changeOrderService;
    this.customerUserRepository = customerUserRepository;
}
```

- [ ] **Step 2: Add the `stageSummaryToMap` private helper after existing `changeOrderToMap`**

This is the lean version that excludes internal billing fields (`appliedCreditAmount`, `netPayableAmount`, `paidAmount`, `certifiedBy`, retention data).

```java
/**
 * Lean payment stage view for customer — excludes internal billing fields.
 */
private Map<String, Object> stageSummaryToMap(PaymentStage s) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("stageNumber", s.getStageNumber());
    m.put("stageName", s.getStageName());
    m.put("stageAmountExGst", s.getStageAmountExGst());
    m.put("gstAmount", s.getGstAmount());
    m.put("stageAmountInclGst", s.getStageAmountInclGst());
    m.put("stagePercentage", s.getStagePercentage());
    m.put("status", s.getStatus());
    m.put("dueDate", s.getDueDate());
    m.put("milestoneDescription", s.getMilestoneDescription());
    return m;
}
```

- [ ] **Step 3: Add `boqSummaryToMap` helper after `stageSummaryToMap`**

```java
private Map<String, Object> boqSummaryToMap(BoqDocument doc, List<Map<String, Object>> stages) {
    boolean pendingAcknowledgement = "APPROVED".equals(doc.getStatus())
            && doc.getCustomerAcknowledgedAt() == null;
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("documentId", doc.getId());
    m.put("projectId", doc.getProject().getId());
    m.put("totalValueExGst", doc.getTotalValueExGst());
    m.put("totalGstAmount", doc.getTotalGstAmount());
    m.put("totalValueInclGst", doc.getTotalValueInclGst());
    m.put("gstRate", doc.getGstRate());
    m.put("status", doc.getStatus());
    m.put("revisionNumber", doc.getRevisionNumber());
    m.put("approvedAt", doc.getCustomerApprovedAt());
    m.put("acknowledgedAt", doc.getCustomerAcknowledgedAt());
    m.put("pendingAcknowledgement", pendingAcknowledgement);
    m.put("paymentStages", stages);
    return m;
}
```

- [ ] **Step 4: Add the `GET /summary` endpoint after `getBoqDocument`**

```java
/**
 * BOQ summary: approved (or latest non-rejected) document with embedded lean payment stages.
 * Includes pendingAcknowledgement flag so the app can prompt the customer to acknowledge.
 */
@GetMapping("/summary")
public ResponseEntity<Map<String, Object>> getBoqSummary(
        @PathVariable("projectId") String projectUuid,
        Authentication auth) {
    try {
        String email = auth.getName();
        Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

        // Prefer approved document; fall back to latest non-rejected
        Optional<BoqDocument> docOpt = boqDocumentRepository.findByProjectIdAndStatus(
                project.getId(), "APPROVED");
        if (docOpt.isEmpty()) {
            docOpt = boqDocumentRepository.findTopByProjectIdAndStatusNotOrderByRevisionNumberDesc(
                    project.getId(), "REJECTED");
        }
        if (docOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "No BOQ available yet", "data", (Object) null));
        }

        BoqDocument doc = docOpt.get();
        List<Map<String, Object>> stages = paymentStageRepository
                .findByBoqDocumentIdOrderByStageNumberAsc(doc.getId())
                .stream().map(this::stageSummaryToMap).toList();

        return ResponseEntity.ok(Map.of("success", true, "data", boqSummaryToMap(doc, stages)));
    } catch (Exception e) {
        logger.error("Failed to fetch BOQ summary for project {}", projectUuid, e);
        return ResponseEntity.status(500).body(
                Map.of("success", false, "message", "An internal error occurred"));
    }
}
```

- [ ] **Step 5: Add the `GET /payment-stages` endpoint after `getPaymentSchedule`**

```java
/**
 * Payment stages for the project — lean customer view, no internal billing fields.
 */
@GetMapping("/payment-stages")
public ResponseEntity<Map<String, Object>> getBoqPaymentStages(
        @PathVariable("projectId") String projectUuid,
        Authentication auth) {
    try {
        String email = auth.getName();
        Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

        List<Map<String, Object>> stages = paymentStageRepository
                .findByProjectIdOrderByStageNumberAsc(project.getId())
                .stream().map(this::stageSummaryToMap).toList();

        return ResponseEntity.ok(Map.of("success", true, "stages", stages));
    } catch (Exception e) {
        logger.error("Failed to fetch BOQ payment stages for project {}", projectUuid, e);
        return ResponseEntity.status(500).body(
                Map.of("success", false, "message", "An internal error occurred"));
    }
}
```

- [ ] **Step 6: Add the `PATCH /documents/{documentId}/acknowledge` endpoint**

```java
/**
 * Records the customer's digital acknowledgement of a BOQ document. Idempotent.
 * Cross-checks document belongs to the requested project before writing.
 */
@Transactional
@PatchMapping("/documents/{documentId}/acknowledge")
public ResponseEntity<Map<String, Object>> acknowledgeBoq(
        @PathVariable("projectId") String projectUuid,
        @PathVariable Long documentId,
        Authentication auth) {
    try {
        String email = auth.getName();
        Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

        BoqDocument doc = boqDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("BOQ document not found: " + documentId));

        if (!doc.getProject().getId().equals(project.getId())) {
            return ResponseEntity.status(403).body(
                    Map.of("success", false, "message", "Document does not belong to this project"));
        }

        CustomerUser customer = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

        doc.setCustomerAcknowledgedAt(LocalDateTime.now());
        doc.setCustomerAcknowledgedBy(customer.getId());
        boqDocumentRepository.save(doc);

        List<Map<String, Object>> stages = paymentStageRepository
                .findByBoqDocumentIdOrderByStageNumberAsc(doc.getId())
                .stream().map(this::stageSummaryToMap).toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "BOQ acknowledged",
                "data", boqSummaryToMap(doc, stages)));
    } catch (IllegalArgumentException e) {
        return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
    } catch (Exception e) {
        logger.error("Failed to acknowledge BOQ document {} for project {}", documentId, projectUuid, e);
        return ResponseEntity.status(500).body(
                Map.of("success", false, "message", "An internal error occurred"));
    }
}
```

- [ ] **Step 7: Compile to verify**

```bash
cd "wd_customer_api" && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 8: Start customer API and smoke-test all 3 new endpoints**

```bash
# 1. Get a customer JWT
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<customer-email>","password":"<password>"}' | jq -r '.token')

# 2. GET summary — replace <uuid> with a project UUID the customer belongs to
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8081/api/projects/<uuid>/boq/summary" | jq .
# Expected: { "success": true, "data": { "documentId": N, "pendingAcknowledgement": true/false, "paymentStages": [...] } }

# 3. GET payment-stages
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8081/api/projects/<uuid>/boq/payment-stages" | jq .
# Expected: { "success": true, "stages": [ { "stageNumber": 1, "stageName": "...", ... } ] }

# 4. PATCH acknowledge — replace <docId> with the BOQ document ID
curl -s -X PATCH -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8081/api/projects/<uuid>/boq/documents/<docId>/acknowledge" | jq .
# Expected: { "success": true, "message": "BOQ acknowledged", "data": { ..., "acknowledgedAt": "2026-...", "pendingAcknowledgement": false } }

# 5. Verify idempotency — calling acknowledge again should succeed (not error)
curl -s -X PATCH -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8081/api/projects/<uuid>/boq/documents/<docId>/acknowledge" | jq .
# Expected: same 200 response with updated acknowledgedAt

# 6. Cross-project check — passing a documentId from a different project should return 403
curl -s -o /dev/null -w "%{http_code}" -X PATCH -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8081/api/projects/<other-uuid>/boq/documents/<docId>/acknowledge"
# Expected: 403
```

- [ ] **Step 9: Commit**

```bash
git add wd_customer_api/src/main/java/com/wd/custapi/controller/CustomerBoqController.java
git commit -m "feat(customer-api): add BOQ summary, payment-stages, and acknowledge endpoints"
```

---

### Task 4: Portal API — Delete CustomerBoqController and its DTOs

**Files:**
- Delete: `wd_portal_api/src/main/java/com/wd/api/controller/CustomerBoqController.java`
- Delete: `wd_portal_api/src/main/java/com/wd/api/dto/CustomerBoqSummary.java`
- Delete: `wd_portal_api/src/main/java/com/wd/api/dto/CustomerPaymentStageView.java`

- [ ] **Step 1: Delete the three files**

```bash
rm "wd_portal_api/src/main/java/com/wd/api/controller/CustomerBoqController.java"
rm "wd_portal_api/src/main/java/com/wd/api/dto/CustomerBoqSummary.java"
rm "wd_portal_api/src/main/java/com/wd/api/dto/CustomerPaymentStageView.java"
```

- [ ] **Step 2: Verify no remaining references to the deleted types**

```bash
grep -r "CustomerBoqSummary\|CustomerPaymentStageView\|CustomerBoqController" \
  wd_portal_api/src/main/ --include="*.java"
```

Expected: no output

- [ ] **Step 3: Compile portal API**

```bash
cd "wd_portal_api" && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add -u wd_portal_api/src/main/java/com/wd/api/controller/CustomerBoqController.java
git add -u wd_portal_api/src/main/java/com/wd/api/dto/CustomerBoqSummary.java
git add -u wd_portal_api/src/main/java/com/wd/api/dto/CustomerPaymentStageView.java
git commit -m "feat(portal-api): delete CustomerBoqController and DTOs — moved to customer API"
```

---

### Task 5: Portal API — Remove CUSTOMER token handling from JwtAuthenticationFilter

**Files:**
- Modify: `wd_portal_api/src/main/java/com/wd/api/security/JwtAuthenticationFilter.java`

Context: The current `doFilterInternal` dispatches on token type: PARTNER → `handlePartnerAuthentication`, CUSTOMER → `handleCustomerAuthentication`, else → `handlePortalAuthentication`. We remove the CUSTOMER branch and its method entirely.

- [ ] **Step 1: Replace the 3-branch if/else-if/else with a 2-branch if/else**

Current block (lines 67–78):
```java
if ("PARTNER".equals(tokenType)) {
    // Partnership user authentication
    handlePartnerAuthentication(jwt, actualSubject, request);
} else if ("CUSTOMER".equals(tokenType)) {
    // CustomerUser authentication (project owners, architects, 3rd-party viewers)
    handleCustomerAuthentication(actualSubject, request);
} else {
    // Portal user authentication (company employees only)
    handlePortalAuthentication(jwt, actualSubject, request);
}
```

Replace with:
```java
if ("PARTNER".equals(tokenType)) {
    // Partnership user authentication
    handlePartnerAuthentication(jwt, actualSubject, request);
} else {
    // Portal user authentication (company employees only)
    handlePortalAuthentication(jwt, actualSubject, request);
}
```

- [ ] **Step 2: Delete the entire `handleCustomerAuthentication` method (lines 81–89)**

```java
private void handleCustomerAuthentication(String email, HttpServletRequest request) {
    // Customer users get ROLE_CUSTOMER authority — they can only access /api/customer/** endpoints
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            email, null, authorities);
    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authToken);
    logger.debug("Authenticated customer user: {}", email);
}
```

Delete this method entirely.

- [ ] **Step 3: Compile to verify**

```bash
cd "wd_portal_api" && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add wd_portal_api/src/main/java/com/wd/api/security/JwtAuthenticationFilter.java
git commit -m "feat(portal-api): remove CUSTOMER JWT token handling from filter"
```

---

### Task 6: Portal API — Rename verifyCustomerAccess + remove acknowledgeDocument

**Files:**
- Modify: `wd_portal_api/src/main/java/com/wd/api/security/ProjectAccessGuard.java`
- Modify: `wd_portal_api/src/main/java/com/wd/api/service/BoqDocumentService.java`

`verifyCustomerAccess` remains needed in portal API for `BoqDocumentService.recordCustomerApproval()` (portal admin records that a specific customer user approved — it verifies project membership, not JWT). Rename clarifies it is a DB-level membership check, not JWT auth.

- [ ] **Step 1: In ProjectAccessGuard.java, rename the method and update its Javadoc**

Old method (lines 57–62):
```java
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
```

Replace with:
```java
/**
 * Verifies that the given customer user is a member of the given project.
 * Used by portal-side operations that reference a customer (e.g. recording customer approval).
 * This is a pure DB check — no JWT involved.
 *
 * @throws AccessDeniedException (HTTP 403) if not a project member
 */
public void verifyCustomerMembership(Long customerUserId, Long projectId) {
    if (!projectMemberRepository.existsByProject_IdAndCustomerUser_Id(projectId, customerUserId)) {
        throw new AccessDeniedException("Customer user " + customerUserId
                + " is not a member of project " + projectId);
    }
}
```

- [ ] **Step 2: In BoqDocumentService.java, update the call site in `recordCustomerApproval` (line 218)**

Old:
```java
projectAccessGuard.verifyCustomerAccess(customerSignedById, doc.getProject().getId());
```

New:
```java
projectAccessGuard.verifyCustomerMembership(customerSignedById, doc.getProject().getId());
```

- [ ] **Step 3: In BoqDocumentService.java, delete the `acknowledgeDocument` method (lines 240–247)**

Delete this entire method:
```java
public BoqDocument acknowledgeDocument(Long documentId, Long customerUserId) {
    BoqDocument doc = getDocument(documentId);
    projectAccessGuard.verifyCustomerAccess(customerUserId, doc.getProject().getId());

    doc.setCustomerAcknowledgedAt(LocalDateTime.now());
    doc.setCustomerAcknowledgedBy(customerUserId);
    return boqDocumentRepository.save(doc);
}
```

- [ ] **Step 4: Compile to verify**

```bash
cd "wd_portal_api" && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add wd_portal_api/src/main/java/com/wd/api/security/ProjectAccessGuard.java
git add wd_portal_api/src/main/java/com/wd/api/service/BoqDocumentService.java
git commit -m "feat(portal-api): rename verifyCustomerAccess→verifyCustomerMembership, remove acknowledgeDocument"
```

---

### Task 7: Portal API — Remove dead /api/customer/** security rules from SecurityConfig

**Files:**
- Modify: `wd_portal_api/src/main/java/com/wd/api/security/SecurityConfig.java`

These 5 matchers are dead — no controller in portal API handles `/api/customer/**`. Customer auth is served entirely by the customer API.

- [ ] **Step 1: Remove 6 lines from `securityFilterChain` (comment + 5 request matchers)**

Delete these lines:
```java
// Customer portal endpoints (public auth)
.requestMatchers("/api/customer/login").permitAll()
.requestMatchers("/api/customer/logout").permitAll()
.requestMatchers("/api/customer/forgot-password").permitAll()
.requestMatchers("/api/customer/reset-password").permitAll()

// Customer portal endpoints (protected - requires ROLE_CUSTOMER)
.requestMatchers("/api/customer/**").hasRole("CUSTOMER")
```

- [ ] **Step 2: Compile to verify**

```bash
cd "wd_portal_api" && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Final integration smoke-test**

```bash
# Customer JWT from customer API
CUST_TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<customer-email>","password":"<password>"}' | jq -r '.token')

# Old portal customer endpoint must now 401 (CUSTOMER token unknown to portal API)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $CUST_TOKEN" \
  "http://localhost:8080/api/customer/boq/project/47/summary")
echo "Portal old endpoint: $STATUS"
# Expected: 401 or 403 — never 200

# New customer API endpoint must still 200
STATUS2=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $CUST_TOKEN" \
  "http://localhost:8081/api/projects/<uuid>/boq/summary")
echo "Customer API new endpoint: $STATUS2"
# Expected: 200

# Portal user JWT should still work for portal operations
PORTAL_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<portal-user-email>","password":"<password>"}' | jq -r '.token')

STATUS3=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $PORTAL_TOKEN" \
  "http://localhost:8080/api/boq/documents/2")
echo "Portal API BOQ doc: $STATUS3"
# Expected: 200
```

- [ ] **Step 4: Commit**

```bash
git add wd_portal_api/src/main/java/com/wd/api/security/SecurityConfig.java
git commit -m "feat(portal-api): remove dead /api/customer/** security matchers"
```
