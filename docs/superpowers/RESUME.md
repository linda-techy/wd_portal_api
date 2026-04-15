# Resume State — Move CustomerBoqController to Customer API

**Branch:** fix/security-precision-concurrency-and-rate-limiting  
**Next task:** Write implementation plan then execute it

## Goal
Move customer-facing BOQ endpoints out of portal API into customer API.
Both apps share the same DB — no cross-app calls needed.

## Why
Portal API is company-side. Customer API is customer-side.
`CustomerBoqController` in the portal API forces the portal to verify customer JWTs.
Moving it eliminates the JWT coupling entirely.

## What to move FROM portal API

### Controller to delete
`src/main/java/com/wd/api/controller/CustomerBoqController.java`

3 endpoints:
- `GET /api/customer/boq/project/{projectId}/summary`
- `GET /api/customer/boq/project/{projectId}/payment-stages`
- `PATCH /api/customer/boq/documents/{documentId}/acknowledge`

### DTOs to copy then delete from portal API
- `src/main/java/com/wd/api/dto/CustomerBoqSummary.java` — record, uses `BoqDocument` + `CustomerPaymentStageView`
- `src/main/java/com/wd/api/dto/CustomerPaymentStageView.java` — record, maps `PaymentStage`

### Portal API cleanup after move
- `JwtAuthenticationFilter.java` — remove `CUSTOMER` token handling (lines ~70-88)
- `ProjectAccessGuard.java` — remove `verifyCustomerAccess()` method (only used by CustomerBoqController and BoqDocumentService.acknowledgeDocument)
- `BoqDocumentService.acknowledgeDocument()` — move to customer API or inline into new controller
- `BoqDocumentController.java` — remove `customer-approve` endpoint? (NO — keep it, it's called by portal users approving on behalf of customer. But remove ROLE_CUSTOMER auth logic from filter)

### Key question: keep `verifyCustomerAccess` in portal?
It's used in TWO places in portal API:
1. `CustomerBoqController` (being moved) ✓
2. `BoqDocumentService.recordCustomerApproval()` line 218 — verifies the customerSignedById is a project member

Place 2 is PORTAL-side (admin recording approval). It calls `verifyCustomerAccess(customerSignedById, projectId)` to check the customer is a project member — this is a DB check, not JWT. Keep a renamed version `verifyCustomerMembership()` for this use case.

## What already exists in customer API
Package root: `com.wd.custapi`

Models already present:
- `model/BoqDocument.java` ✓
- `model/PaymentStage.java` ✓

Repositories already present:
- `repository/BoqDocumentRepository.java` ✓
- `repository/PaymentStageRepository.java` ✓
- `repository/CustomerUserRepository.java` ✓

Needs to be added to customer API:
- `dto/CustomerBoqSummary.java` (copy + adapt package name)
- `dto/CustomerPaymentStageView.java` (copy + adapt package name)
- `controller/BoqController.java` (new — the moved endpoints)
- `service/BoqAcknowledgeService.java` or inline in controller
- Access guard equivalent (project membership check for customer)

## Customer API auth context
Customer API already has its own `JwtAuthenticationFilter` and security config.
Customers are already authenticated when they hit customer API endpoints.
Principal is email string (same pattern as portal API's CUSTOMER handling).
Current customer API auth uses `CustomerUser` loaded from DB.

## Confirm: what stays in portal API
- `BoqDocumentController` — all portal-user workflows including `customer-approve` (portal admin records approval)
- `BoqDocumentService.recordCustomerApproval()` — stays, uses `verifyCustomerMembership` (renamed)
- `BoqDocumentService.acknowledgeDocument()` — DELETE from portal (moved to customer API)
- Portal JWT filter: remove CUSTOMER token handling

## DB state (test data — don't rely on for plan)
- Doc 2 project 47: APPROVED, customer_approved_by=23, customer_acknowledged_by=23
- Payment stages for doc 2: IDs 9-10 (Foundation 30%, Completion 70%)

## How to resume
1. Open fresh session
2. Read this file
3. Run: write implementation plan using superpowers:writing-plans skill
4. Plan should cover: customer API additions + portal API cleanup
5. Then execute plan
