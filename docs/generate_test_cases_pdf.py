"""
Production-grade Test Case Catalog generator for the WallDot Construction
Management Portal (wd_portal_api + wd_portal_app_flutter).

Author : Senior QA (Construction-tech, India)
Scope  : Lead-to-handover lifecycle, residential / commercial / interior /
         renovation, India tax + compliance, role-based access, mobile
         field-ops, offline sync, financial integrity.

Run:    python docs/generate_test_cases_pdf.py
Output: docs/PORTAL_TEST_CASES.pdf
"""

import os
from datetime import date

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak,
    KeepTogether, NextPageTemplate, PageTemplate, Frame, BaseDocTemplate
)


# ---------------------------------------------------------------------------
# Styles
# ---------------------------------------------------------------------------
styles = getSampleStyleSheet()

H1 = ParagraphStyle('H1', parent=styles['Heading1'], fontSize=18,
                    spaceAfter=8, textColor=colors.HexColor('#0B3D91'))
H2 = ParagraphStyle('H2', parent=styles['Heading2'], fontSize=13,
                    spaceBefore=10, spaceAfter=4,
                    textColor=colors.HexColor('#0B3D91'))
H3 = ParagraphStyle('H3', parent=styles['Heading3'], fontSize=11,
                    spaceBefore=6, spaceAfter=3,
                    textColor=colors.HexColor('#444444'))
BODY = ParagraphStyle('BODY', parent=styles['BodyText'], fontSize=9,
                      leading=11, alignment=TA_LEFT)
SMALL = ParagraphStyle('SMALL', parent=styles['BodyText'], fontSize=8,
                       leading=10, alignment=TA_LEFT)
CELL = ParagraphStyle('CELL', parent=styles['BodyText'], fontSize=8,
                      leading=10, alignment=TA_LEFT)
CELLBOLD = ParagraphStyle('CELLBOLD', parent=CELL, fontName='Helvetica-Bold')
COVER_T = ParagraphStyle('COVERT', parent=styles['Title'], fontSize=26,
                         leading=30, alignment=TA_CENTER,
                         textColor=colors.HexColor('#0B3D91'))
COVER_S = ParagraphStyle('COVERS', parent=styles['Title'], fontSize=14,
                         leading=18, alignment=TA_CENTER,
                         textColor=colors.HexColor('#444444'))


def P(text):
    """Shortcut for paragraph in a cell."""
    return Paragraph(text.replace('\n', '<br/>'), CELL)


def steps_to_html(steps):
    out = []
    for i, s in enumerate(steps, 1):
        out.append(f"<b>{i}.</b> {s}")
    return '<br/>'.join(out)


# ---------------------------------------------------------------------------
# Test-case dataset
#
# Each test case is a dict:
#   id, title, module, priority, type, pre, steps (list), expected,
#   construction (optional), risk (optional defect/gap reference)
# ---------------------------------------------------------------------------
TC = []  # filled below


def add(tc):
    TC.append(tc)


# ============================== 1. AUTH & SESSION ==========================
add(dict(
    id="TC-AUTH-001", title="Portal staff login with valid credentials",
    module="Authentication", priority="P1", type="Functional",
    pre="Active PortalUser with assigned role; correct email & password known.",
    steps=[
        "Open portal web app login screen.",
        "Enter valid email, valid password.",
        "Click 'Sign In'.",
        "Inspect network response for /auth/login and local secure storage.",
    ],
    expected=(
        "HTTP 200 with accessToken (JWT), refreshToken, expiresIn (~3600s), "
        "userInfo, and permissions[] returned. Tokens persisted in secure "
        "storage; user is routed to role-based landing page (Admin/PM/Site)."
    ),
    risk="P1 financial-access surface — verify least-privilege landing.",
))
add(dict(
    id="TC-AUTH-002", title="Login fails with wrong password",
    module="Authentication", priority="P1", type="Negative",
    pre="Active user; password intentionally wrong.",
    steps=[
        "Enter correct email, incorrect password.",
        "Submit login form.",
    ],
    expected=(
        "HTTP 401 with generic error message ('Invalid credentials'). "
        "No information leakage about whether email exists. "
        "Repeated failures should trigger account-lock policy after N attempts (verify policy)."
    ),
    risk="GAP: account-lockout policy not visible in code — confirm.",
))
add(dict(
    id="TC-AUTH-003", title="Login is rate-limited / brute-force protected",
    module="Authentication", priority="P1", type="Security",
    pre="Same email targeted from same IP.",
    steps=[
        "Submit 10 incorrect login attempts in 60 seconds.",
        "Observe rate-limit and lockout behaviour.",
    ],
    expected=(
        "HTTP 429 after threshold; subsequent attempts blocked for cooldown. "
        "Audit log records failed-login burst with IP and user-agent."
    ),
    risk="GAP — no explicit rate limit found on /auth/login; high CVSS risk.",
))
add(dict(
    id="TC-AUTH-004", title="Refresh-token rotation & replay detection",
    module="Authentication", priority="P1", type="Security",
    pre="Valid login session; refreshToken captured.",
    steps=[
        "Call POST /auth/refresh-token with valid refreshToken — receive new pair.",
        "Replay the OLD refreshToken a second time.",
    ],
    expected=(
        "First call: 200 with new accessToken + new refreshToken. "
        "Second call: 401; backend revokes ALL refreshTokens for the user "
        "(replay-attack mitigation per RefreshTokenService)."
    ),
))
add(dict(
    id="TC-AUTH-005", title="Access token expiry forces refresh on next call",
    module="Authentication", priority="P2", type="Functional",
    pre="Login complete; wait until accessToken expiry (~1h) or force expire.",
    steps=[
        "Allow access token to expire.",
        "Call any protected endpoint (e.g. GET /projects).",
    ],
    expected=(
        "Client interceptor detects 401, calls /auth/refresh-token, retries "
        "original request with new token. No user-visible logout."
    ),
))
add(dict(
    id="TC-AUTH-006", title="Logout revokes refresh token",
    module="Authentication", priority="P2", type="Security",
    pre="User logged in.",
    steps=[
        "Tap 'Log out'.",
        "Try to use captured refreshToken via /auth/refresh-token.",
    ],
    expected="401 on refresh — token revoked server-side (refresh_tokens.revoked=true).",
))
add(dict(
    id="TC-AUTH-007", title="Password reset flow with token expiry",
    module="Authentication", priority="P1", type="Security",
    pre="Active user; mailbox accessible.",
    steps=[
        "Click 'Forgot password' and submit email.",
        "Open reset link; reset password with new strong value.",
        "Login with new password.",
        "Attempt to reuse the same reset link.",
    ],
    expected=(
        "Reset succeeds first time. Second use returns 400/410 (token "
        "consumed). Tokens older than 24h are rejected. "
        "Generic 'check your email' response avoids account enumeration."
    ),
    risk="Confirm code path enforces PortalPasswordResetToken.expiryDateTime check.",
))
add(dict(
    id="TC-AUTH-008", title="FCM token registration after login",
    module="Authentication", priority="P2", type="Functional",
    pre="Login on mobile (Flutter) app with FCM available.",
    steps=[
        "Login.",
        "Inspect network for POST /auth/fcm-token immediately after login.",
        "Force-rotate the device FCM token; relogin or open app.",
    ],
    expected="Server stores fcmToken on PortalUser. Re-login updates token. No duplicate registrations.",
))
add(dict(
    id="TC-AUTH-009", title="Concurrent multi-device login allowed but isolatable",
    module="Authentication", priority="P2", type="Functional",
    pre="Same user logs in on web and mobile.",
    steps=[
        "Login on Device A.",
        "Login on Device B with same credentials.",
        "Logout on Device B.",
    ],
    expected="Both sessions valid until explicit logout. Logout on B does NOT invalidate A.",
))
add(dict(
    id="TC-AUTH-010", title="Customer portal login (CustomerUser distinct from PortalUser)",
    module="Authentication", priority="P1", type="Functional",
    pre="Lead converted; CustomerUser created with welcome email + temp password.",
    steps=[
        "Customer follows email link, sets password.",
        "Customer logs into /customer login.",
        "Try to log into staff portal with the same credentials.",
    ],
    expected="Customer access granted only to /customer portal. Staff portal returns 403 — separate identity space.",
    construction="Residential customer with single project access.",
))

# ============================== 2. ACL / ROLES =============================
add(dict(
    id="TC-ACL-001", title="ADMIN role permissions are immutable",
    module="ACL & Roles", priority="P1", type="Security",
    pre="Logged in as ADMIN.",
    steps=[
        "Navigate to Roles → Admin → Edit permissions.",
        "Attempt to remove a permission and Save via PUT /acl/roles/{adminId}/permissions.",
    ],
    expected="Edit blocked client-side; server returns 400/403 — ADMIN immutable per spec.",
))
add(dict(
    id="TC-ACL-002", title="Site Engineer cannot certify stage payments",
    module="ACL & Roles", priority="P1", type="Security",
    pre="User assigned SITE_ENGINEER role.",
    steps=[
        "Attempt POST /api/projects/{id}/stages/{stageId}/certify.",
    ],
    expected="403 — STAGE_CERTIFY permission not granted to SITE_ENGINEER.",
))
add(dict(
    id="TC-ACL-003", title="Director can approve VO but cannot create",
    module="ACL & Roles", priority="P2", type="Functional",
    pre="DIRECTOR role.",
    steps=[
        "POST /change-orders — expect 403.",
        "PATCH /change-orders/{id}/approve — expect 200 (VO_APPROVE granted).",
    ],
    expected="Separation of duties enforced: creator ≠ approver.",
))
add(dict(
    id="TC-ACL-004", title="Project-scoped access for PROJECT_MANAGER",
    module="ACL & Roles", priority="P1", type="Security",
    pre="PM-A is project member of P1 only; project P2 exists.",
    steps=[
        "PM-A calls GET /api/boq-documents/project/{P2-id}.",
    ],
    expected="403 via ProjectAccessGuard — not a member of P2.",
))
add(dict(
    id="TC-ACL-005", title="Role permission edit reflects in user's token within session",
    module="ACL & Roles", priority="P2", type="Functional",
    pre="ADMIN edits PM role to remove DEDUCTION_CREATE.",
    steps=[
        "Admin saves edited PM permissions.",
        "PM user calls POST /api/projects/{id}/deductions without re-login.",
        "PM user re-logs in and retries.",
    ],
    expected=(
        "Until token refresh, PM still has cached perms (acceptable). "
        "After re-login, request denied 403. Recommendation: invalidate "
        "active sessions on role-change for sensitive perms."
    ),
    risk="GAP: live permission propagation; no session invalidation on role edit.",
))
add(dict(
    id="TC-ACL-006", title="ACL audit trail of role/permission changes",
    module="ACL & Roles", priority="P2", type="Compliance",
    pre="ADMIN changes a role's permission set.",
    steps=[
        "Inspect audit log for role-edit entry.",
    ],
    expected="Audit row stores: who, when, what role, before/after permission diff, reason.",
    risk="GAP: no role_assignment_history table — compliance hole.",
))

# ============================== 3. LEADS ===================================
add(dict(
    id="TC-LEAD-001", title="Public contact form creates lead (anonymous)",
    module="Leads", priority="P1", type="Functional",
    pre="Marketing site contact form accessible without login.",
    steps=[
        "Open public contact form.",
        "Fill: name='Anita Sharma', phone='+91 98xxxx', email, projectType='residential_construction', state='Karnataka', district='Bengaluru', budget=4500000, sqft=2400, floors=2.",
        "Submit.",
    ],
    expected=(
        "POST /leads/contact returns 201. Lead row created with status "
        "'new_inquiry', source='website', score auto-computed, "
        "assignedToId NULL until manual assignment. "
        "Notification fanned to LEAD_VIEW users."
    ),
    construction="Residential, 2-floor, Tier-1 city.",
))
add(dict(
    id="TC-LEAD-002", title="Public referral form preserves referrer attribution",
    module="Leads", priority="P1", type="Functional",
    pre="Existing customer/partner shares referral link.",
    steps=[
        "Submit POST /leads/referral with referredByEmail/Name/Phone populated.",
    ],
    expected="leadSource='referralClient' or 'referralArchitect'; referrer fields stored verbatim.",
))
add(dict(
    id="TC-LEAD-003", title="Public endpoints are rate-limited and CAPTCHA-protected",
    module="Leads", priority="P1", type="Security",
    pre="No authenticated session.",
    steps=[
        "Script 200 contact-form submissions in 60s from one IP.",
    ],
    expected="429 after threshold; CAPTCHA required after N attempts.",
    risk="GAP — no rate limit found on /leads/contact and /leads/referral.",
))
add(dict(
    id="TC-LEAD-004", title="Lead score recomputation on field change",
    module="Leads", priority="P2", type="Functional",
    pre="Lead with budget=8L (WARM).",
    steps=[
        "Edit budget to 80L (>50L → HIGH bucket).",
        "Save.",
        "Open Lead Score History tab.",
    ],
    expected=(
        "Score increased by +25 (budget factor). New row in LeadScoreHistory "
        "with previousScore/newScore, scoreFactors JSON capture, "
        "category transition COLD/WARM → HOT."
    ),
))
add(dict(
    id="TC-LEAD-005", title="Logging an interaction schedules a follow-up",
    module="Leads", priority="P1", type="Functional",
    pre="Lead in 'qualified' status.",
    steps=[
        "Click 'Log Call' → fill subject, duration=12m, outcome='SCHEDULED_FOLLOWUP', nextActionDate=+3 days.",
        "Save.",
        "Open user's 'My Tasks' or follow-up board.",
    ],
    expected=(
        "LeadInteraction row created; nextFollowUp visible in dashboard. "
        "Notification scheduled for due date."
    ),
))
add(dict(
    id="TC-LEAD-006", title="Overdue follow-ups surface on dashboard",
    module="Leads", priority="P2", type="Functional",
    pre="Lead with nextFollowUp=yesterday and outcome != NOT_INTERESTED.",
    steps=[
        "GET /leads/overdue-followups.",
    ],
    expected="Lead returned with overdueDays>0; UI highlights in red.",
))
add(dict(
    id="TC-LEAD-007", title="Lead status transition forbids skipping won/lost",
    module="Leads", priority="P2", type="Negative",
    pre="Lead in 'project_won' status.",
    steps=[
        "Attempt to call POST /leads/{id}/convert again.",
    ],
    expected="400 — lead already converted; idempotency safeguard.",
))
add(dict(
    id="TC-LEAD-008", title="Concurrent lead conversion does not create duplicate project",
    module="Leads", priority="P1", type="Edge / Concurrency",
    pre="Lead L1 in 'qualified' status.",
    steps=[
        "Fire TWO simultaneous POST /leads/L1/convert calls (e.g., two tabs).",
    ],
    expected=(
        "Exactly ONE CustomerProject created; second call returns 409/400. "
        "Confirms transactional or DB-unique safeguard."
    ),
    risk="GAP confirmed by code review — service does check-then-create without isolation.",
))
add(dict(
    id="TC-LEAD-009", title="Lead conversion migrates documents and activity",
    module="Leads", priority="P2", type="Integration",
    pre="Lead with 3 uploaded documents and 2 LeadInteractions.",
    steps=[
        "Convert lead → project.",
        "Open new project Documents and Activity tabs.",
    ],
    expected="Documents re-linked to project; LeadInteraction entries appear in project activity feed.",
))
add(dict(
    id="TC-LEAD-010", title="Lead soft-delete preserves audit and references",
    module="Leads", priority="P2", type="Functional",
    pre="Lead with interactions and documents.",
    steps=[
        "Delete the lead.",
        "Query DB for deleted_at on Lead row.",
        "Confirm LeadInteraction and LeadDocument still resolvable.",
    ],
    expected="Lead.deleted_at set; cross-references intact for compliance audit.",
))
add(dict(
    id="TC-LEAD-011", title="Lead → Quotation/Estimation linkage",
    module="Leads", priority="P2", type="Integration",
    pre="Lead with projectType='interior_work', sqft=1200.",
    steps=[
        "Open 'Estimation' on lead.",
        "Apply interior template → review rough costing.",
    ],
    expected="Estimation pulls interior-type catalog items; total computed; PDF preview generated.",
    construction="Interior work — design-only scope, no structural items.",
))
add(dict(
    id="TC-LEAD-012", title="Lead phone field accepts international format and validates",
    module="Leads", priority="P3", type="Validation",
    pre="Lead create form open.",
    steps=[
        "Enter phone '+91-98xx-xx-xxxx'; submit.",
        "Repeat with '12345' (invalid).",
    ],
    expected="First accepted; second blocked with helpful validation message. WhatsApp number stored in normalized E.164.",
))

# ============================== 4. CUSTOMER & PROJECT ======================
add(dict(
    id="TC-CUST-001", title="Create customer manually with full GST/PAN",
    module="Customers", priority="P1", type="Functional",
    pre="CUSTOMER_CREATE permission.",
    steps=[
        "Add customer with GSTIN '29ABCDE1234F1Z5', companyName, address.",
        "Save.",
        "Validate via GST format regex (2-digit state, 10-char PAN, 1 entity, Z, checksum).",
    ],
    expected="201 created; GSTIN persisted; UI displays state-derived label (Karnataka=29).",
))
add(dict(
    id="TC-CUST-002", title="GSTIN format validation rejects malformed entries",
    module="Customers", priority="P1", type="Validation",
    pre="Add Customer form.",
    steps=[
        "Enter GSTIN '29ABCDE1234F1Z' (14 chars).",
        "Enter GSTIN '00ABCDE1234F1Z5' (invalid state).",
    ],
    expected="Both rejected with specific error. Server-side validation must match (cannot rely on UI only).",
    risk="GAP — no checksum validation; consider integrating GSTN portal check.",
))
add(dict(
    id="TC-CUST-003", title="Soft-deactivate customer while preserving project history",
    module="Customers", priority="P1", type="Functional",
    pre="Customer C1 linked to active project P1 (recent fix b309934).",
    steps=[
        "PATCH /customers/C1/enabled → false.",
        "Re-open C1 and P1.",
    ],
    expected="C1.enabled=false; cannot login; P1 unaffected; PM and team retain visibility.",
))
add(dict(
    id="TC-CUST-004", title="Strict delete blocked when customer has active references",
    module="Customers", priority="P1", type="Negative",
    pre="Customer C1 has active project P1.",
    steps=[
        "DELETE /customers/C1.",
    ],
    expected="409/422 with clear message listing active references; deletion refused.",
))
add(dict(
    id="TC-CUST-005", title="Soft-delete of customer with only inactive leads succeeds",
    module="Customers", priority="P2", type="Functional",
    pre="Customer C2 has only soft-deleted leads (commit dc5db05).",
    steps=[
        "DELETE /customers/C2.",
    ],
    expected="200; customer soft-deleted; linked leads preserved for audit.",
))
add(dict(
    id="TC-CPRJ-001", title="Create customer project end-to-end with auto-code",
    module="Customer Project", priority="P1", type="Functional",
    pre="CUSTOMER_PROJECT_CREATE permission.",
    steps=[
        "Create project: name, customer C1, type='commercial_construction', state, district, sqft=18000, floors=4, budget=4.5cr, plot=8000, facing='E'.",
        "Save.",
    ],
    expected="Project code auto-generated 'PRJ-YYYY-NNNN'; phase=PLANNING; status=ACTIVE.",
    construction="Commercial, 4-floor.",
))
add(dict(
    id="TC-CPRJ-002", title="GPS lock — admin-only override after initial set",
    module="Customer Project", priority="P2", type="Security",
    pre="Project P1 with GPS coordinates set & gpsLockedAt populated.",
    steps=[
        "Non-admin user attempts to update lat/long.",
        "Admin updates lat/long.",
    ],
    expected="Non-admin: 403/400. Admin: 200, gpsLockedByUserId updated, audit row appended.",
))
add(dict(
    id="TC-CPRJ-003", title="Phase transitions validated as monotonic",
    module="Customer Project", priority="P2", type="Validation",
    pre="Project in 'DESIGN' phase.",
    steps=[
        "PATCH /customer-projects/{id}/phase to 'PLANNING'.",
    ],
    expected=(
        "Expected: 400/422 — reverse transition not allowed without explicit "
        "rollback reason. Today the code lacks this check (GAP)."
    ),
    risk="GAP — phase transitions not enforced as monotonic.",
))
add(dict(
    id="TC-CPRJ-004", title="Project member add/remove updates customer-portal access",
    module="Customer Project", priority="P1", type="Functional",
    pre="Project P1; customer user C-USER not yet a member.",
    steps=[
        "POST /customer-projects/P1/members with role='OWNER' and C-USER.",
        "C-USER logs into customer portal — should see P1.",
        "DELETE that member.",
        "C-USER reloads — should NOT see P1.",
    ],
    expected="Visibility toggled accordingly; cache invalidated; no stale entries.",
))
add(dict(
    id="TC-CPRJ-005", title="Overall progress weighting (milestone/task/budget)",
    module="Customer Project", priority="P2", type="Functional",
    pre="Default weights milestone 40%, task 30%, budget 30%.",
    steps=[
        "Set milestoneProgress=80, taskProgress=60, budgetProgress=50.",
        "Trigger aggregation.",
    ],
    expected="overallProgress = 80*0.4 + 60*0.3 + 50*0.3 = 65 (rounded).",
))
add(dict(
    id="TC-CPRJ-006", title="Status SUSPENDED blocks new financial transactions",
    module="Customer Project", priority="P1", type="Functional",
    pre="Project status set to SUSPENDED.",
    steps=[
        "Attempt to certify a payment stage and create a PO.",
    ],
    expected="Both blocked with descriptive error; only read endpoints allowed.",
    risk="Verify backend enforces this — easy gap.",
))

# ============================== 5. BOQ / ESTIMATION ========================
add(dict(
    id="TC-BOQ-001", title="Create BOQ document in DRAFT and add items",
    module="BOQ", priority="P1", type="Functional",
    pre="Project P1 in DESIGN phase; BOQ_DOCUMENT_CREATE permission.",
    steps=[
        "POST /api/boq-documents with projectId.",
        "POST /api/boq with 3 BASE items (foundation, RCC, MEP), 1 ADDON (premium tiles), 1 DEDUCTION (-1 wall).",
        "GET /summary.",
    ],
    expected=(
        "Document in DRAFT. lineTotal = quantity × unitRate. "
        "totalValueExGst, gstAmount (rate*18%), totalValueInclGst correct. "
        "BASE/ADDON require qty>0; DEDUCTION can have qty>0 but stored as negative impact."
    ),
    construction="Residential, structural + finishing scope.",
))
add(dict(
    id="TC-BOQ-002", title="itemKind enforced as enum",
    module="BOQ", priority="P1", type="Validation",
    pre="BOQ in DRAFT.",
    steps=[
        "POST item with itemKind='FOO'.",
    ],
    expected="400 — enum constraint (post C.PR-3). Only BASE, ADDON, DEDUCTION accepted.",
))
add(dict(
    id="TC-BOQ-003", title="Submit BOQ document for internal approval",
    module="BOQ", priority="P1", type="Functional",
    pre="DRAFT BOQ with at least one item.",
    steps=[
        "POST /api/boq-documents/{id}/submit.",
    ],
    expected="Status DRAFT → PENDING_APPROVAL; submittedAt/By stamped; notification to BOQ_APPROVE users.",
))
add(dict(
    id="TC-BOQ-004", title="Reject BOQ requires reason and bumps revision",
    module="BOQ", priority="P2", type="Functional",
    pre="Document in PENDING_APPROVAL.",
    steps=[
        "POST /reject without reason — expect 400.",
        "POST /reject with reason — expect 200.",
        "Edit items and resubmit.",
    ],
    expected="On reject: status → DRAFT, revisionNumber++; rejectionReason stored. Resubmit increments again or maintains chain.",
))
add(dict(
    id="TC-BOQ-005", title="Approval locks scope; further item edits forbidden",
    module="BOQ", priority="P1", type="Negative",
    pre="Document APPROVED.",
    steps=[
        "PUT /api/boq/{itemId} attempting rate change.",
    ],
    expected="409/403 — scope locked; user directed to create Change Order.",
))
add(dict(
    id="TC-BOQ-006", title="Customer acknowledgement separate from internal approval",
    module="BOQ", priority="P2", type="Functional",
    pre="Document APPROVED.",
    steps=[
        "Customer logs into portal and views BOQ.",
        "Customer clicks 'Acknowledge'.",
    ],
    expected="customerAcknowledgedAt/By stored; status remains APPROVED (no status change). Audit row in BoqAuditLog.",
))
add(dict(
    id="TC-BOQ-007", title="GST split intra-state CGST+SGST vs inter-state IGST",
    module="BOQ", priority="P1", type="Compliance",
    pre="Project state Karnataka; supplier registered Karnataka. Then second test: supplier Maharashtra.",
    steps=[
        "Generate invoice intra-state.",
        "Generate invoice inter-state.",
    ],
    expected=(
        "Intra-state: CGST 9% + SGST 9%. Inter-state: IGST 18%. "
        "Place-of-supply field on TaxInvoice matches; isInterstate flag correct."
    ),
))
add(dict(
    id="TC-BOQ-008", title="Concurrent edits to two BOQ items by two users",
    module="BOQ", priority="P2", type="Concurrency",
    pre="DRAFT BOQ; two users editing distinct items.",
    steps=[
        "User A updates rate of item I1.",
        "User B updates qty of item I2 simultaneously.",
    ],
    expected="Both succeed; totalValueExGst recalculated correctly. No lost updates. Pessimistic or optimistic locking present.",
))
add(dict(
    id="TC-BOQ-009", title="ExecutionQuantity cannot exceed contracted quantity",
    module="BOQ", priority="P1", type="Validation",
    pre="Item I1 qty=100 m3.",
    steps=[
        "Record executionQuantity=120.",
    ],
    expected="Expected: 400 with helpful error; overrun must use Change Order. GAP: confirm enforcement.",
    risk="GAP — overbill risk if not enforced.",
))
add(dict(
    id="TC-BOQ-010", title="Lead estimation produces project-type-specific catalog",
    module="Estimation", priority="P2", type="Functional",
    pre="Lead created with projectType variants.",
    steps=[
        "Open estimation; switch projectType among residential/commercial/interior/renovation.",
    ],
    expected="Different default catalogs and rate templates loaded for each.",
))
add(dict(
    id="TC-BOQ-011", title="BOQ export to Excel with INR and lakh/crore formatting",
    module="BOQ", priority="P3", type="Functional",
    pre="APPROVED BOQ; BOQ_EXPORT permission.",
    steps=[
        "Click 'Export'.",
        "Open the file.",
    ],
    expected="Excel cells use INR symbol, 2 decimal places, group separator ',' Indian numbering (12,34,56,789).",
))
add(dict(
    id="TC-BOQ-012", title="Negative rates / quantities rejected",
    module="BOQ", priority="P2", type="Validation",
    pre="DRAFT BOQ.",
    steps=[
        "Add BASE item qty=-5.",
        "Add BASE item rate=-100.",
    ],
    expected="Both rejected with field-level errors.",
))

# ============================== 6. DPC =====================================
add(dict(
    id="TC-DPC-001", title="Create DPC document only against APPROVED BOQ",
    module="DPC", priority="P1", type="Functional",
    pre="BOQ in DRAFT vs APPROVED.",
    steps=[
        "Try to create DPC referencing DRAFT BOQ → expect failure.",
        "Approve BOQ.",
        "Retry creating DPC.",
    ],
    expected="First: 400. Second: 201 — DPC in DRAFT state.",
))
add(dict(
    id="TC-DPC-002", title="DPC issue freezes PDF and prevents further edits",
    module="DPC", priority="P1", type="Functional",
    pre="DPC in DRAFT.",
    steps=[
        "Set cover text, signatory names, customizations.",
        "Click 'Issue'.",
        "Try editing scopes after issue.",
    ],
    expected="Issue: status DRAFT → ISSUED; PDF generated and stored; issuedAt/By/PDF id recorded. Edit fails.",
))
add(dict(
    id="TC-DPC-003", title="DPC customization pricing must respect catalog rate",
    module="DPC", priority="P2", type="Validation",
    pre="Active catalog item C1 rate=₹1,200/sft.",
    steps=[
        "Add customization line with unitRate=₹600 (50% override).",
    ],
    expected=(
        "Either rejected or override audit-logged with reviewer approval. "
        "Currently no validation — GAP."
    ),
    risk="GAP — pricing override unchecked; audit hole.",
))
add(dict(
    id="TC-DPC-004", title="Revision history preserves prior PDFs",
    module="DPC", priority="P2", type="Functional",
    pre="DPC issued, then changed and issued again.",
    steps=[
        "Issue revision 1.",
        "Modify scope, issue revision 2.",
    ],
    expected="Two PDFs persisted; revisionNumber increments; UI shows revision dropdown.",
))
add(dict(
    id="TC-DPC-005", title="Scope template applied to new project by type",
    module="DPC", priority="P2", type="Functional",
    pre="Templates exist per projectType.",
    steps=[
        "Create project of type 'residential_construction'.",
        "Apply scope template.",
    ],
    expected="Residential-specific scopes pre-populated; interior project would load different defaults.",
    construction="Residential vs Interior — verify template segmentation.",
))

# ============================== 7. PROCUREMENT =============================
add(dict(
    id="TC-PROC-001", title="Material Indent draft → submit → approve flow",
    module="Procurement", priority="P1", type="Functional",
    pre="PROCUREMENT_CREATE + APPROVE permissions; project active.",
    steps=[
        "Create indent with 3 items (cement 200 bags, TMT 5 MT, sand 30 m3).",
        "Submit; another user approves.",
    ],
    expected="DRAFT → SUBMITTED → APPROVED. quantityApproved set per line; notification to procurement officer.",
))
add(dict(
    id="TC-PROC-002", title="RFQ produces multiple vendor quotations",
    module="Procurement", priority="P2", type="Functional",
    pre="Approved indent.",
    steps=[
        "Add 3 vendor quotations against indent — different quotedAmount, deliveryCharges, taxAmount, validUntil.",
        "Compare quotations.",
        "Select winner.",
    ],
    expected="Selected quotation status SELECTED; others REJECTED. selectedAt stamped.",
))
add(dict(
    id="TC-PROC-003", title="Vendor quotation past validUntil cannot be selected",
    module="Procurement", priority="P2", type="Validation",
    pre="Quotation with validUntil = yesterday.",
    steps=[
        "Attempt to select.",
    ],
    expected="400 / 422 with 'quotation expired'. GAP if not enforced.",
    risk="GAP — code stores validUntil but does not enforce.",
))
add(dict(
    id="TC-PROC-004", title="PO issued, partial GRN, then close",
    module="Procurement", priority="P1", type="Functional",
    pre="Issued PO for 100 bags.",
    steps=[
        "Record GRN for 60 bags.",
        "Status → PARTIALLY_RECEIVED.",
        "Record GRN for remaining 40.",
        "Auto-close PO when fully received.",
    ],
    expected="Status transitions: ISSUED → PARTIALLY_RECEIVED → RECEIVED → CLOSED.",
))
add(dict(
    id="TC-PROC-005", title="GRN cannot exceed PO quantity",
    module="Procurement", priority="P1", type="Validation",
    pre="PO of 100 bags; 80 already received.",
    steps=[
        "Attempt GRN for 30 bags (total 110).",
    ],
    expected="400 with overrun error; recommend Change Order or separate PO.",
))
add(dict(
    id="TC-PROC-006", title="PO cancellation requires reason",
    module="Procurement", priority="P2", type="Functional",
    pre="PO in ISSUED status.",
    steps=[
        "POST /purchase-orders/{id}/cancel with empty reason → 400.",
        "Resubmit with reason='Vendor unable to deliver in monsoon' → 200.",
    ],
    expected="Cancellation recorded with reason; downstream GRN blocked.",
))
add(dict(
    id="TC-PROC-007", title="Idempotent payment challan generation",
    module="Procurement", priority="P1", type="Concurrency",
    pre="Payment transaction completed.",
    steps=[
        "Trigger challan generation twice (network retry).",
    ],
    expected=(
        "Exactly one challan WAL/CH/2026-27/NNN created; pessimistic lock on "
        "ChallanSequence prevents duplicate. Verify with concurrent threads."
    ),
))
add(dict(
    id="TC-PROC-008", title="E-way bill capture for inter-state material movement",
    module="Procurement", priority="P1", type="Compliance",
    pre="Vendor in Tamil Nadu; project in Karnataka; PO ₹65,000.",
    steps=[
        "Try to record GRN without e-way bill number.",
    ],
    expected="Expected: mandatory e-way bill > ₹50,000. Currently MISSING — flag as compliance gap.",
    risk="GAP — no e-way bill capture field; serious for GST audit.",
))
add(dict(
    id="TC-PROC-009", title="Vendor GSTIN uniqueness enforced",
    module="Procurement", priority="P2", type="Validation",
    pre="Vendor V1 with GSTIN '29ABCDE1234F1Z5'.",
    steps=[
        "Create V2 with same GSTIN.",
    ],
    expected="409 — uniqueness constraint.",
))
add(dict(
    id="TC-PROC-010", title="Budget threshold blocks PO above material budget",
    module="Procurement", priority="P2", type="Validation",
    pre="Project budget for steel = ₹10L; existing POs sum = ₹9L.",
    steps=[
        "Create PO for ₹3L steel.",
    ],
    expected="Expected: hard stop or warning + approver required. Currently disabled (GAP per code comment).",
    risk="GAP — validateItemBudget() noted as disabled.",
))
add(dict(
    id="TC-PROC-011", title="Vendor TDS deduction on payment (Section 194C)",
    module="Procurement", priority="P1", type="Compliance",
    pre="Vendor invoice ₹2L; 2% TDS applicable for contractor.",
    steps=[
        "Record vendor payment.",
    ],
    expected="net_paid = invoice - TDS - other_deductions; TDS slip generated with section reference. GAP today.",
    risk="GAP — TDS section selection and slip not present.",
))
add(dict(
    id="TC-PROC-012", title="UoM mismatch between indent and quotation flagged",
    module="Procurement", priority="P3", type="Validation",
    pre="Indent line in 'bag'; quotation line in 'MT'.",
    steps=[
        "Attempt to select quotation.",
    ],
    expected="Warning to convert; conversion factor recorded.",
))

# ============================== 8. INVENTORY ===============================
add(dict(
    id="TC-INV-001", title="Stock inflow on GRN updates project inventory",
    module="Inventory", priority="P1", type="Integration",
    pre="GRN posted for 100 bags cement to project P1.",
    steps=[
        "Inspect inventory_stock for material=cement, project=P1.",
    ],
    expected="quantity += 100; lastUpdatedDate=today.",
))
add(dict(
    id="TC-INV-002", title="Reorder level alert",
    module="Inventory", priority="P2", type="Functional",
    pre="Stock quantity=15 bags; reorderLevel=20.",
    steps=[
        "Trigger inventory check.",
    ],
    expected="Alert emitted (notification + dashboard tile). GAP: confirm automation.",
))
add(dict(
    id="TC-INV-003", title="Negative stock impossible (issued > on-hand)",
    module="Inventory", priority="P1", type="Validation",
    pre="On-hand=10 bags.",
    steps=[
        "Issue 25 bags to a task.",
    ],
    expected="400 — insufficient stock; suggestion to raise indent.",
))
add(dict(
    id="TC-INV-004", title="Inter-project transfer keeps ledger balanced",
    module="Inventory", priority="P2", type="Integration",
    pre="Project A stock=200; Project B=0.",
    steps=[
        "Transfer 50 bags A→B.",
    ],
    expected="A=150, B=50. Audit trail records source/dest/by-user.",
))
add(dict(
    id="TC-INV-005", title="Consumption report ties to issued vs returned",
    module="Inventory", priority="P2", type="Functional",
    pre="Material issued 100 bags, returned 5.",
    steps=[
        "GET /api/inventory/reports/consumption/{projectId}.",
    ],
    expected="Report shows consumed=95; reconciled with task progress.",
))

# ============================== 9. LABOUR ==================================
add(dict(
    id="TC-LAB-001", title="Bulk attendance entry for daily-wage labourers",
    module="Labour", priority="P1", type="Functional",
    pre="20 labourers active on Project P1.",
    steps=[
        "Open Attendance screen.",
        "Mark 18 PRESENT, 1 HALF_DAY, 1 ABSENT.",
        "Save.",
    ],
    expected="20 LabourAttendance rows; hours auto-derived; saved offline if network drops then synced.",
))
add(dict(
    id="TC-LAB-002", title="Wage sheet generates for the date range",
    module="Labour", priority="P1", type="Functional",
    pre="Attendance recorded for 26 working days.",
    steps=[
        "POST /api/labour/wagesheet/generate with start/end dates.",
    ],
    expected="WageSheet DRAFT; one WageSheetEntry per labourer with daysWorked, dailyWage, netPayable; advances pre-deducted.",
))
add(dict(
    id="TC-LAB-003", title="Wage sheet approval and payment marking",
    module="Labour", priority="P1", type="Functional",
    pre="DRAFT wagesheet ₹1.8L net.",
    steps=[
        "Submit, then PUT /wagesheet/{id}/approve, then /mark-paid.",
    ],
    expected="States: DRAFT→SUBMITTED→APPROVED→PAID. After PAID no edits.",
))
add(dict(
    id="TC-LAB-004", title="Advance > pending wages blocked",
    module="Labour", priority="P2", type="Validation",
    pre="Labour L1 net payable this month ₹15,000; outstanding advance ₹14,500.",
    steps=[
        "Record new advance ₹2,000.",
    ],
    expected="Warning/refuse — would leave negative net. Otherwise auto-carry-forward and capped to net.",
    risk="GAP — no documented cap check.",
))
add(dict(
    id="TC-LAB-005", title="Duplicate attendance entry per day rejected",
    module="Labour", priority="P2", type="Validation",
    pre="Attendance for L1 on 2026-05-12 exists.",
    steps=[
        "Submit attendance again for same date.",
    ],
    expected="409 with 'duplicate attendance'.",
))
add(dict(
    id="TC-LAB-006", title="PF/ESI deduction for eligible workers (compliance)",
    module="Labour", priority="P1", type="Compliance",
    pre="Labour with monthly wages > ₹15,000 should be PF-eligible.",
    steps=[
        "Generate wagesheet.",
    ],
    expected="Expected: employee & employer PF (12%/12%), ESI for <₹21k. GAP today — module absent.",
    risk="GAP — Indian labour-law non-compliance; flag to product.",
))
add(dict(
    id="TC-LAB-007", title="ID proof type/number captured for compliance",
    module="Labour", priority="P3", type="Validation",
    pre="Add Labour form.",
    steps=[
        "Enter idProofType=Aadhaar, idProofNumber='1234-5678-9012'.",
    ],
    expected="Server masks PII in logs (Aadhaar XXXX-XXXX-9012); UI shows masked.",
    risk="Verify Aadhaar-Act compliance for storage/encryption.",
))

# ============================== 10. SUBCONTRACT ============================
add(dict(
    id="TC-SUB-001", title="Subcontract work order lifecycle (LUMPSUM)",
    module="Subcontract", priority="P1", type="Functional",
    pre="Vendor V1; project P1; BOQ item for plumbing.",
    steps=[
        "Create work order LUMPSUM ₹4.5L; retention 5%.",
        "Issue to contractor.",
        "Contractor submits measurement ₹1.5L.",
        "PM approves; payment recorded.",
        "After completion: release retention.",
    ],
    expected=(
        "Status DRAFT→ISSUED→IN_PROGRESS→COMPLETED→(retention released). "
        "Each payment deducts 5% retention into totalRetentionAccumulated."
    ),
))
add(dict(
    id="TC-SUB-002", title="Unit-rate work order: certified value matches measurement",
    module="Subcontract", priority="P2", type="Functional",
    pre="UNIT_RATE work order: ₹450 per sqm; planned 1000 sqm.",
    steps=[
        "Submit measurement 350 sqm.",
        "Approve.",
    ],
    expected="certifiedValue = 350 × 450 = ₹1,57,500.",
))
add(dict(
    id="TC-SUB-003", title="Measurement rejection cycle preserves history",
    module="Subcontract", priority="P2", type="Functional",
    pre="Measurement SUBMITTED.",
    steps=[
        "PM rejects with reason.",
        "Contractor resubmits revised measurement.",
    ],
    expected="First record REJECTED with rejectionReason; second SUBMITTED. Audit chain intact.",
))
add(dict(
    id="TC-SUB-004", title="Retention release cap respected",
    module="Subcontract", priority="P1", type="Validation",
    pre="totalRetentionAccumulated ₹50,000.",
    steps=[
        "Attempt release ₹70,000.",
    ],
    expected="400 — cannot exceed accumulated retention.",
))
add(dict(
    id="TC-SUB-005", title="DLP enforced — retention not releasable before DLP end",
    module="Subcontract", priority="P2", type="Compliance",
    pre="DLP set to 12 months post completion.",
    steps=[
        "Try release before DLP end.",
    ],
    expected="Expected: 400 or require override approval. GAP — no DLP enforcement today.",
    risk="GAP — defect liability not tracked.",
))
add(dict(
    id="TC-SUB-006", title="GST applied on subcontract payment",
    module="Subcontract", priority="P1", type="Compliance",
    pre="Subcontractor with valid GSTIN.",
    steps=[
        "Generate invoice on certified ₹1L.",
    ],
    expected="GST 18% computed; CGST+SGST or IGST based on place of supply. GAP if missing.",
    risk="GAP — work order missing GST field.",
))

# ============================== 11. PARTNERSHIPS ===========================
add(dict(
    id="TC-PART-001", title="Partner application end-to-end",
    module="Partnerships", priority="P2", type="Functional",
    pre="No active partner with email.",
    steps=[
        "Submit /api/partnerships/apply with firmName, GSTIN, RERA, license.",
        "Admin approves via /admin/partnerships/{id}/status.",
        "Partner logs in.",
    ],
    expected="Status: pending → approved → active. Partner sees dashboard.",
))
add(dict(
    id="TC-PART-002", title="Architect referral creates attributed lead",
    module="Partnerships", priority="P2", type="Integration",
    pre="Active architect partner P-ARCH.",
    steps=[
        "Partner submits POST /api/partnerships/referrals/lead.",
    ],
    expected="Lead created with leadSource='referralArchitect'; partner attribution captured (commission reference).",
))
add(dict(
    id="TC-PART-003", title="Suspended partner cannot submit referrals",
    module="Partnerships", priority="P2", type="Security",
    pre="Partner suspended.",
    steps=[
        "Try POST referral as that partner.",
    ],
    expected="403 with reason.",
))
add(dict(
    id="TC-PART-004", title="Partner password reset cannot enumerate accounts",
    module="Partnerships", priority="P2", type="Security",
    pre="Non-existent email.",
    steps=[
        "POST /partnerships/forgot-password.",
    ],
    expected="Returns 200 always; no info leak.",
))

# ============================== 12. TASKS / WBS / PROGRESS =================
add(dict(
    id="TC-TSK-001", title="Create task with mandatory dueDate validation",
    module="Tasks", priority="P1", type="Validation",
    pre="Project P1; TASK_CREATE.",
    steps=[
        "POST /api/tasks without dueDate.",
        "POST with valid dueDate.",
    ],
    expected="First 400; second 201.",
))
add(dict(
    id="TC-TSK-002", title="Progress update triggers status & CPM recompute",
    module="Tasks", priority="P1", type="Functional",
    pre="Task PENDING.",
    steps=[
        "PATCH progress 0 → 60 → 100.",
    ],
    expected="0→PENDING; 60→IN_PROGRESS; 100→COMPLETED; actualEndDate stamped; CPM recomputed; milestone rollup updated.",
))
add(dict(
    id="TC-TSK-003", title="Predecessor cycle detection on save",
    module="Tasks", priority="P1", type="Validation",
    pre="A→B→C.",
    steps=[
        "Add predecessor C→A.",
    ],
    expected="400 CycleDetectedException; graph state unchanged.",
))
add(dict(
    id="TC-TSK-004", title="Predecessor lag respects working calendar & holidays",
    module="Tasks", priority="P2", type="Functional",
    pre="Holiday config for 2026-05-15 (Buddha Purnima).",
    steps=[
        "Task A finishes 2026-05-14; FS lag 1.",
        "Open Gantt.",
    ],
    expected="Task B starts 2026-05-16 (skipping holiday).",
    construction="Calendar respects regional Indian holidays.",
))
add(dict(
    id="TC-TSK-005", title="Monsoon-sensitive tasks flagged in monsoon window",
    module="Tasks", priority="P3", type="Construction-Type",
    pre="Project in coastal Karnataka; monsoon June-Sep configured.",
    steps=[
        "Schedule roofing task in July.",
    ],
    expected="Warning icon + tooltip 'monsoon-sensitive'; CPM impact note.",
))
add(dict(
    id="TC-TSK-006", title="Reassignment audited",
    module="Tasks", priority="P2", type="Functional",
    pre="Task assigned to U1.",
    steps=[
        "PUT /api/tasks/{id}/assign to U2.",
    ],
    expected="TaskAssignmentHistory row written; GET /assignment-history returns chain.",
))
add(dict(
    id="TC-TSK-007", title="PM rejects completion submission",
    module="Tasks", priority="P2", type="Functional",
    pre="Task in PENDING_PM_APPROVAL.",
    steps=[
        "PM rejects with reason 'Incomplete plastering on east wall'.",
    ],
    expected="Status reverts to IN_PROGRESS; rejectionReason on task; notification to assignee.",
))
add(dict(
    id="TC-TSK-008", title="Negative lag (lead) supported",
    module="Tasks", priority="P3", type="Functional",
    pre="A and B can overlap by 2 days.",
    steps=[
        "Set lagDays=-2.",
    ],
    expected="Stored; CPM treats as lead. Confirm UI/UX correctness or reject if not desired.",
    risk="Confirm intended behaviour — code allows negative.",
))
add(dict(
    id="TC-TSK-009", title="Baseline approval freezes plan; variance available",
    module="Scheduling", priority="P1", type="Functional",
    pre="Plan complete; PM approves baseline.",
    steps=[
        "POST /api/projects/{id}/baseline/approve.",
        "Edit task dates.",
        "Open Variance report.",
    ],
    expected="ProjectBaseline + TaskBaseline rows created; variance report shows delta vs baseline.",
))
add(dict(
    id="TC-TSK-010", title="WBS template clone produces valid graph for project type",
    module="WBS", priority="P2", type="Construction-Type",
    pre="Templates: residential, commercial, interior, renovation.",
    steps=[
        "Apply each template to a fresh project of matching type.",
    ],
    expected="Tasks + predecessors cloned; dates auto-shifted; no cycles introduced.",
    construction="All four major construction types.",
))

# ============================== 13. SITE OPS ==============================
add(dict(
    id="TC-SR-001", title="Site report requires at least one photo",
    module="Site Reports", priority="P1", type="Validation",
    pre="Site engineer on mobile app.",
    steps=[
        "Create daily progress report with no photos; submit.",
    ],
    expected="400 — at least one photo mandatory.",
))
add(dict(
    id="TC-SR-002", title="Geotagged photo within project geofence",
    module="Site Reports", priority="P2", type="Functional",
    pre="Project geofence set; site engineer on-site.",
    steps=[
        "Capture photo; check geotag (lat/lon, locationAccuracy).",
    ],
    expected="distanceFromProject < 200m (configurable). If >, warning surfaced.",
    risk="GAP — no plausibility checks (clock skew, location-jump).",
))
add(dict(
    id="TC-SR-003", title="Offline site report queued and synced",
    module="Site Reports", priority="P1", type="Offline",
    pre="Mobile in airplane mode.",
    steps=[
        "Create daily report with 5 photos.",
        "Save.",
        "Re-enable network.",
    ],
    expected="Report persisted locally; auto-synced on reconnect with no duplicates; idempotency key respected.",
))
add(dict(
    id="TC-SR-004", title="Site report approval triggers notifications",
    module="Site Reports", priority="P2", type="Integration",
    pre="Report SUBMITTED.",
    steps=[
        "PM approves.",
    ],
    expected="Status APPROVED; WebhookPublisher emits event; customer push notified.",
))
add(dict(
    id="TC-SV-001", title="Site visit check-in geolocation captured",
    module="Site Visits", priority="P1", type="Functional",
    pre="Site visit PENDING.",
    steps=[
        "Tap 'Check In' at project gate.",
    ],
    expected="Status PENDING → CHECKED_IN; lat/lon stored; distance from project computed.",
))
add(dict(
    id="TC-SV-002", title="Check-out without check-in blocked",
    module="Site Visits", priority="P2", type="Negative",
    pre="No active visit.",
    steps=[
        "POST /api/site-visits/{id}/check-out.",
    ],
    expected="400 with state error.",
))
add(dict(
    id="TC-SV-003", title="GPS spoofing detection",
    module="Site Visits", priority="P1", type="Security",
    pre="User uses mock-location app.",
    steps=[
        "Check in from off-site coordinates spoofed to project coords.",
    ],
    expected="Mock-location flag detected and stored; visit flagged for review.",
    risk="GAP — no mock-location flag in code today.",
))
add(dict(
    id="TC-OBS-001", title="Observation lifecycle OPEN→RESOLVED with evidence",
    module="Observations", priority="P2", type="Functional",
    pre="Site engineer flags observation with severity HIGH.",
    steps=[
        "Create OPEN observation with image.",
        "PM moves to IN_PROGRESS.",
        "Worker resolves with resolution photo.",
        "PM moves to CLOSED.",
    ],
    expected="State transitions accepted; CLOSED only after RESOLVED.",
))
add(dict(
    id="TC-OBS-002", title="Critical-severity observation blocks task completion",
    module="Observations", priority="P2", type="Integration",
    pre="Critical OPEN observation on Task T.",
    steps=[
        "Attempt to mark T COMPLETED.",
    ],
    expected="Blocked with reference to open critical observation.",
    risk="Verify rule exists.",
))
add(dict(
    id="TC-QC-001", title="Quality check FAIL creates NCR/Observation automatically",
    module="Quality", priority="P2", type="Integration",
    pre="QC checklist for slab pour.",
    steps=[
        "Record check with status=FAIL.",
    ],
    expected="Observation auto-created with link to QC; PM notified.",
))
add(dict(
    id="TC-DEL-001", title="Delay log captures impact on phase",
    module="Delays", priority="P2", type="Functional",
    pre="Phase 'Foundation' active.",
    steps=[
        "Log delay 5 days due to rain.",
    ],
    expected="Phase plannedEnd vs actualEnd diverges; variance dashboard updated.",
))
add(dict(
    id="TC-CCTV-001", title="CCTV stream auth and TLS",
    module="CCTV", priority="P1", type="Security",
    pre="Camera registered.",
    steps=[
        "Open stream URL outside portal.",
    ],
    expected="Stream requires signed URL or VPN. Plain RTSP not exposed publicly.",
    risk="GAP — code stores streamUrl plaintext; verify proxying.",
))
add(dict(
    id="TC-GAL-001", title="Gallery photo upload geo-tagged and quota-limited",
    module="Gallery", priority="P3", type="Functional",
    pre="Project P1; user uploads 200 photos.",
    steps=[
        "Bulk upload.",
    ],
    expected="EXIF geotag retained; files chunked/compressed; per-file size limit (e.g. 25 MB) enforced.",
))

# ============================== 14. CHANGE MGMT ===========================
add(dict(
    id="TC-CO-001", title="Change Order workflow internal+customer with OTP",
    module="Change Orders", priority="P1", type="Functional",
    pre="APPROVED BOQ.",
    steps=[
        "Create CO addition ₹2,50,000.",
        "Submit → internal approve → send to customer.",
        "Customer enters OTP and approves.",
        "CO moves IN_PROGRESS → WBS merge.",
    ],
    expected="Status DRAFT→SUBMITTED→INTERNALLY_APPROVED→CUSTOMER_REVIEW→APPROVED→IN_PROGRESS→COMPLETED→CLOSED. Advance invoice auto-generated for addition.",
))
add(dict(
    id="TC-CO-002", title="Reduction CO triggers credit note",
    module="Change Orders", priority="P1", type="Functional",
    pre="CO reduction amount ₹40,000 against scope.",
    steps=[
        "Approve reduction CO.",
    ],
    expected="CreditNote created automatically; applied against next stage payment.",
))
add(dict(
    id="TC-CO-003", title="WBS merge from CO does not introduce cycles",
    module="Change Orders", priority="P1", type="Concurrency",
    pre="Existing WBS A→B→C; CO adds B'→A predecessor (would loop).",
    steps=[
        "Approve CO and trigger merge.",
    ],
    expected="Merge fails with cycle error; CO held in PENDING_MERGE state.",
    risk="GAP — verify ChangeRequestMergeService runs cycle check.",
))
add(dict(
    id="TC-CO-004", title="Rejection by customer surfaces reason",
    module="Change Orders", priority="P2", type="Functional",
    pre="CO in CUSTOMER_REVIEW.",
    steps=[
        "Customer rejects with reason.",
    ],
    expected="Status REJECTED; rejectionReason stored; resubmission requires revision.",
))
add(dict(
    id="TC-CO-005", title="Concurrent CO numbering uniqueness",
    module="Change Orders", priority="P2", type="Concurrency",
    pre="Two PMs create COs in same second.",
    steps=[
        "Submit both.",
    ],
    expected="Distinct referenceNumbers (e.g., CO-2026-0001, 0002).",
    risk="GAP — pessimistic lock on sequence not verified.",
))
add(dict(
    id="TC-VAR-001", title="Variation cost-impact reflects in budget burn",
    module="Variations", priority="P2", type="Integration",
    pre="Variation COSTED with cost_impact=₹1.2L.",
    steps=[
        "Approve and schedule.",
    ],
    expected="Project budget revised; variance dashboard updated; CO+Variation reconciled (no double-count).",
))

# ============================== 15. STAGE PAYMENTS =========================
add(dict(
    id="TC-STG-001", title="Stage list initialized from approved BOQ snapshot",
    module="Stage Payments", priority="P1", type="Functional",
    pre="BOQ APPROVED with stages 10/20/30/40%.",
    steps=[
        "GET /api/projects/{id}/stages.",
    ],
    expected="Stages UPCOMING with frozen boqValueSnapshot; subsequent BOQ edits do NOT mutate values.",
))
add(dict(
    id="TC-STG-002", title="Certify stage moves status to DUE and computes retention",
    module="Stage Payments", priority="P1", type="Functional",
    pre="Stage UPCOMING, retentionPct 5%.",
    steps=[
        "Submit POST /stages/{id}/certify with required evidence (site report).",
    ],
    expected="Status DUE; retentionHeld = 5% of stageAmountExGst; certifiedBy/At stored. StagePaymentCertifiedEvent published.",
))
add(dict(
    id="TC-STG-003", title="Invoice generation creates tax invoice with GST",
    module="Stage Payments", priority="P1", type="Compliance",
    pre="Stage DUE.",
    steps=[
        "POST /stages/{id}/invoice.",
    ],
    expected="TaxInvoice row with invoiceNumber (fiscal-year suffixed), CGST+SGST or IGST, place_of_supply correct.",
))
add(dict(
    id="TC-STG-004", title="Payment recording is idempotent",
    module="Stage Payments", priority="P1", type="Concurrency",
    pre="Stage INVOICED.",
    steps=[
        "POST /stages/{id}/payment twice with same X-Idempotency-Key.",
    ],
    expected="First creates transaction; second returns same response, no duplicate row.",
    risk="GAP — endpoint may not honour idempotency yet.",
))
add(dict(
    id="TC-STG-005", title="Overdue stage transition by scheduler",
    module="Stage Payments", priority="P2", type="Batch",
    pre="Stage DUE with dueDate=yesterday.",
    steps=[
        "Run scheduler manually (or wait).",
    ],
    expected="Status DUE→OVERDUE; PaymentStageReminderSent OVERDUE row inserted (dedup ensured).",
    risk="GAP — scheduler may not be wired in deployment.",
))
add(dict(
    id="TC-STG-006", title="Credit note application reduces net payable",
    module="Stage Payments", priority="P2", type="Functional",
    pre="Reduction CO credit ₹40k.",
    steps=[
        "Generate invoice for next stage.",
    ],
    expected="appliedCreditAmount reflected; netPayableAmount lower; credit balance decremented.",
))
add(dict(
    id="TC-STG-007", title="Customer cannot certify own stage (separation of duties)",
    module="Stage Payments", priority="P1", type="Security",
    pre="Customer portal user.",
    steps=[
        "Attempt /certify endpoint via API.",
    ],
    expected="403.",
))
add(dict(
    id="TC-STG-008", title="Stage rounding consistent between schedule and invoice",
    module="Stage Payments", priority="P2", type="Compliance",
    pre="stageAmountExGst stored at 6 decimals.",
    steps=[
        "Generate invoice (2 decimals).",
        "Compare totals across stage and invoice.",
    ],
    expected="Rounding HALF_UP, ≤ 1 paise total difference; reconciliation table acknowledges.",
    risk="GAP — risk of cumulative mismatch.",
))

# ============================== 16. DEDUCTIONS =============================
add(dict(
    id="TC-DED-001", title="Deduction PARTIALLY_ACCEPTABLE requires amount ≤ requested",
    module="Deductions", priority="P1", type="Validation",
    pre="Deduction requested ₹60k.",
    steps=[
        "Decide PARTIALLY_ACCEPTABLE with acceptedAmount=₹70k.",
        "Retry ₹40k.",
    ],
    expected="First rejected; second accepted; settledInFinalAccount unaffected until FA closure.",
))
add(dict(
    id="TC-DED-002", title="Reject decision requires reason",
    module="Deductions", priority="P2", type="Validation",
    pre="Pending deduction.",
    steps=[
        "POST /decision REJECTED with empty reason.",
    ],
    expected="400.",
))
add(dict(
    id="TC-DED-003", title="Escalation chain (NONE→ESCALATED→RESOLVED)",
    module="Deductions", priority="P2", type="Functional",
    pre="Deduction PENDING > SLA days.",
    steps=[
        "POST /escalate to director.",
        "Director decides.",
    ],
    expected="escalationStatus transitions appropriately; notifications fired.",
))
add(dict(
    id="TC-DED-004", title="Audit log immutable for deduction decisions",
    module="Deductions", priority="P1", type="Compliance",
    pre="Decided deduction.",
    steps=[
        "Attempt to edit acceptedAmount post-decision.",
    ],
    expected="Either blocked or revision creates new audit row; original preserved.",
    risk="GAP — no DeductionAuditLog.",
))

# ============================== 17. FINAL ACCOUNT ==========================
add(dict(
    id="TC-FA-001", title="Final Account creation pulls live values",
    module="Final Account", priority="P1", type="Functional",
    pre="Project at handover; CONSTRUCTION → COMPLETED.",
    steps=[
        "POST /api/projects/{id}/final-account.",
    ],
    expected="FA DRAFT created. base + additions − accepted deductions + receipts − retention reconciles.",
))
add(dict(
    id="TC-FA-002", title="Status state machine enforced",
    module="Final Account", priority="P1", type="Functional",
    pre="FA DRAFT.",
    steps=[
        "POST /status with SUBMITTED → DISPUTED → AGREED → CLOSED.",
        "Try to revert from AGREED to DRAFT.",
    ],
    expected="Linear transitions accepted; reverse refused.",
))
add(dict(
    id="TC-FA-003", title="Retention released only after DLP end date",
    module="Final Account", priority="P1", type="Compliance",
    pre="dlpEndDate=2026-11-30.",
    steps=[
        "POST /release-retention on 2026-05-12 → expect 400.",
        "Same after DLP → 200.",
    ],
    expected="Release respects DLP boundary; retentionReleased increments; status updated.",
    risk="GAP — verify scheduled job auto-releases.",
))
add(dict(
    id="TC-FA-004", title="Concurrent FA edits handled with optimistic locking",
    module="Final Account", priority="P2", type="Concurrency",
    pre="Two finance users edit same FA.",
    steps=[
        "Both load row, both save.",
    ],
    expected="Second save rejected with 409; UI prompts merge.",
    risk="GAP — confirm @Version annotation.",
))
add(dict(
    id="TC-FA-005", title="FA recompute reconciles with stage payments + deductions",
    module="Final Account", priority="P2", type="Integration",
    pre="Several stages PAID; 2 deductions accepted.",
    steps=[
        "POST /recompute.",
    ],
    expected="Totals match transactional ledger; mismatch surfaced if any.",
))

# ============================== 18. INVOICES / TAX =========================
add(dict(
    id="TC-TAX-001", title="Invoice number sequence per FY",
    module="Tax Invoice", priority="P1", type="Compliance",
    pre="FY 2026-27 in progress.",
    steps=[
        "Generate 3 invoices.",
    ],
    expected="invoiceNumber pattern includes FY 'INV/2026-27/00003'; sequence safe under concurrency.",
))
add(dict(
    id="TC-TAX-002", title="Intra-state invoice splits CGST+SGST",
    module="Tax Invoice", priority="P1", type="Compliance",
    pre="Company GSTIN Karnataka; customer Karnataka.",
    steps=[
        "Generate invoice ₹1,00,000 @ 18%.",
    ],
    expected="CGST 9% ₹9,000; SGST 9% ₹9,000; IGST 0.",
))
add(dict(
    id="TC-TAX-003", title="Inter-state invoice uses IGST",
    module="Tax Invoice", priority="P1", type="Compliance",
    pre="Customer GSTIN Maharashtra; company Karnataka.",
    steps=[
        "Generate invoice ₹1,00,000 @ 18%.",
    ],
    expected="IGST 18% ₹18,000; CGST/SGST 0.",
))
add(dict(
    id="TC-TAX-004", title="Place-of-supply override propagates",
    module="Tax Invoice", priority="P2", type="Compliance",
    pre="Bill-to and ship-to in different states.",
    steps=[
        "Set placeOfSupply to ship-to state.",
    ],
    expected="isInterstate computed correctly based on place_of_supply, not bill-to.",
))
add(dict(
    id="TC-TAX-005", title="Round-off line and total to nearest paise",
    module="Tax Invoice", priority="P2", type="Compliance",
    pre="Taxable ₹100.155.",
    steps=[
        "Generate invoice.",
    ],
    expected="Rounded HALF_UP to ₹100.16; round-off line populated if total adjusted.",
))
add(dict(
    id="TC-TAX-006", title="Invoice PDF complies with mandatory GST particulars",
    module="Tax Invoice", priority="P1", type="Compliance",
    pre="Invoice generated.",
    steps=[
        "Download PDF.",
    ],
    expected="Contains: supplier name, address, GSTIN; recipient name+GSTIN; invoice no & date; HSN/SAC; description; quantity, unit; taxable value; rate of tax; amount of CGST/SGST/IGST; place of supply; total in words; signature.",
    risk="GAP — verify HSN/SAC code per line.",
))
add(dict(
    id="TC-TAX-007", title="E-invoicing (IRN) for B2B above threshold",
    module="Tax Invoice", priority="P1", type="Compliance",
    pre="Aggregate turnover > ₹5 cr; B2B invoice.",
    steps=[
        "Generate invoice; should call IRP for IRN+QR.",
    ],
    expected="IRN and signed QR embedded in PDF; ack# stored. GAP today.",
    risk="GAP — IRP integration not present.",
))

# ============================== 19. NOTIFICATIONS ==========================
add(dict(
    id="TC-NOT-001", title="Stage certified push reaches FINANCE role users only",
    module="Notifications", priority="P2", type="Functional",
    pre="Multiple roles configured.",
    steps=[
        "Certify a stage.",
    ],
    expected="Only users with STAGE_VIEW + FINANCE perms see in-app; FCM push delivered.",
))
add(dict(
    id="TC-NOT-002", title="Push delivery failure does not lose in-app record",
    module="Notifications", priority="P2", type="Resilience",
    pre="FCM token invalid.",
    steps=[
        "Trigger notification.",
    ],
    expected="DB row persisted; push failure logged; retry policy (or non-blocking).",
))
add(dict(
    id="TC-NOT-003", title="Mark all read endpoint resets badge count",
    module="Notifications", priority="P3", type="Functional",
    pre="10 unread.",
    steps=[
        "PUT /api/portal/notifications/read-all.",
        "GET /unread-count.",
    ],
    expected="unreadCount=0.",
))
add(dict(
    id="TC-NOT-004", title="Daily reminder deduplication per stage/kind",
    module="Notifications", priority="P2", type="Batch",
    pre="Stage due in 3 days.",
    steps=[
        "Run reminder job twice on same day.",
    ],
    expected="Only one row in payment_stage_reminder_sent; UNIQUE constraint enforced.",
))

# ============================== 20. SUPPORT / FEEDBACK =====================
add(dict(
    id="TC-SUP-001", title="Customer raises support ticket, staff replies",
    module="Support", priority="P2", type="Functional",
    pre="Customer portal user.",
    steps=[
        "Create ticket cat=Billing.",
        "Staff assigns to themselves.",
        "Staff replies with attachment.",
        "Customer marks resolved.",
    ],
    expected="States: OPEN → IN_PROGRESS → RESOLVED → CLOSED.",
))
add(dict(
    id="TC-SUP-002", title="Attachment size and type validation",
    module="Support", priority="P2", type="Validation",
    pre="Reply with 50 MB .exe attachment.",
    steps=[
        "Attempt upload.",
    ],
    expected="Rejected — only docs/images, ≤ 10 MB.",
    risk="GAP — verify server-side mime check.",
))
add(dict(
    id="TC-SUP-003", title="SLA breach surfaces escalation flag",
    module="Support", priority="P2", type="Functional",
    pre="Ticket OPEN > 24h.",
    steps=[
        "Run SLA monitor.",
    ],
    expected="Escalation flag visible to supervisor.",
    risk="GAP — SLA fields not modelled today.",
))
add(dict(
    id="TC-FB-001", title="Feedback form dynamic schema renders in customer app",
    module="Feedback", priority="P3", type="Functional",
    pre="Form schema with rating + textarea.",
    steps=[
        "Customer opens form; submits response.",
    ],
    expected="Response stored; analytics tile updates count.",
))

# ============================== 21. WARRANTY / POST-HANDOVER ===============
add(dict(
    id="TC-WAR-001", title="Warranty claim window validated against DLP",
    module="Warranty", priority="P2", type="Functional",
    pre="Handover 2025-01-01; DLP 12 months.",
    steps=[
        "Customer raises claim on 2026-02-15.",
    ],
    expected="System flags out-of-warranty; manual override only with approver.",
))
add(dict(
    id="TC-WAR-002", title="Warranty defect creates linked task and observation",
    module="Warranty", priority="P2", type="Integration",
    pre="In-warranty claim.",
    steps=[
        "Approve claim.",
    ],
    expected="Service Task created; Observation auto-tagged; site visit scheduled.",
))

# ============================== 22. DOCUMENTS ==============================
add(dict(
    id="TC-DOC-001", title="File upload validates size/type and scans",
    module="Documents", priority="P1", type="Security",
    pre="Document upload UI.",
    steps=[
        "Upload 100 MB .pdf, 5 MB .exe, 2 MB image with EICAR test signature.",
    ],
    expected="100 MB rejected (limit); .exe rejected (type); EICAR rejected (AV scan).",
    risk="GAP — verify AV integration.",
))
add(dict(
    id="TC-DOC-002", title="Signed download URLs expire",
    module="Documents", priority="P1", type="Security",
    pre="File download link issued.",
    steps=[
        "Use link after expiry (e.g., 10 min).",
    ],
    expected="403/404 — pre-signed URL expired.",
))
add(dict(
    id="TC-DOC-003", title="Document ACL respects project membership",
    module="Documents", priority="P1", type="Security",
    pre="User not on project.",
    steps=[
        "Try to GET project's BOQ document file URL.",
    ],
    expected="403.",
))

# ============================== 23. DASHBOARDS / EXPORTS ===================
add(dict(
    id="TC-DSH-001", title="Dashboard KPIs accurate to ledger",
    module="Dashboards", priority="P2", type="Functional",
    pre="Multiple projects with mixed state.",
    steps=[
        "Open ADMIN dashboard.",
    ],
    expected="Revenue collected, outstanding, overdue stage count, lead funnel match underlying queries.",
))
add(dict(
    id="TC-DSH-002", title="Export CSV/PDF respects role filter (project-scoped)",
    module="Exports", priority="P2", type="Security",
    pre="PM role; member of 2 projects.",
    steps=[
        "Export payment ledger.",
    ],
    expected="Export contains only PM's projects.",
))

# ============================== 24. CONSTRUCTION-TYPE FLOWS ===============
add(dict(
    id="TC-RES-001",
    title="RESIDENTIAL turnkey end-to-end: lead → handover",
    module="Construction Type — Residential",
    priority="P1", type="End-to-End",
    pre="Marketing lead with 2400 sqft G+2 villa scope.",
    steps=[
        "Convert lead → project (PLANNING).",
        "Create + approve BOQ with foundation/RCC/MEP/finishing.",
        "Issue DPC.",
        "Customer pays advance per Stage-1 schedule.",
        "Move to DESIGN; permit captured.",
        "Move to CONSTRUCTION; daily site reports begin.",
        "Procure cement/steel/sand via Indent→Quotation→PO→GRN.",
        "Daily labour attendance + monthly wagesheets.",
        "Stage-wise certification with GST invoice + retention.",
        "1 minor Change Order during finishes.",
        "Quality checks + handover snag closure.",
        "Final Account → DLP starts.",
    ],
    expected="Each module transitions correctly; financial ledger reconciles; customer signs off.",
    construction="Residential G+2 villa.",
))
add(dict(
    id="TC-COM-001",
    title="COMMERCIAL multi-floor with phased handover",
    module="Construction Type — Commercial",
    priority="P1", type="End-to-End",
    pre="18,000 sqft office shell.",
    steps=[
        "Plan with module structure: Foundation, Structure, MEP, Façade, Finishes.",
        "BOQ with HVAC, fire-fighting, BMS scope.",
        "Procurement includes inter-state PO (IGST).",
        "Subcontracts for MEP (LUMPSUM) and façade (UNIT_RATE).",
        "Phased OC (occupation certificate) and floor-wise handovers.",
        "Stage payments tied to floor handovers.",
    ],
    expected="IGST on inter-state PO; phased Final Account or partial release; retention per floor.",
))
add(dict(
    id="TC-INT-001",
    title="INTERIOR fit-out (no structural)",
    module="Construction Type — Interior",
    priority="P1", type="End-to-End",
    pre="3 BHK fit-out scope.",
    steps=[
        "Lead with projectType='interior_work'.",
        "Estimation uses interior catalog (no foundation/RCC).",
        "DPC heavy on customization (modular kitchen, wardrobes).",
        "Short schedule (8 weeks); 4 stage payments.",
        "Site reports focus on finishing checklists.",
    ],
    expected="Templates exclude structural; DLP shorter (e.g. 6 months).",
))
add(dict(
    id="TC-REN-001",
    title="RENOVATION existing structure with demolition",
    module="Construction Type — Renovation",
    priority="P1", type="End-to-End",
    pre="Existing house; selective demolition planned.",
    steps=[
        "Lead projectType='renovation_remodeling'.",
        "Site survey + photos before scope freeze.",
        "BOQ separates demolition + new work; deductions for salvage credit.",
        "Permit/heritage check if applicable.",
        "Procurement small batches; no e-way bill if local.",
        "Stage payments 25/25/25/25 typical.",
    ],
    expected="Pre-work photos archived in Gallery; salvage value tracked as deduction.",
))
add(dict(
    id="TC-VAS-001",
    title="VASTU consultation-only lead",
    module="Construction Type — Vastu",
    priority="P3", type="Functional",
    pre="Lead projectType='vastu_consultation'.",
    steps=[
        "Capture lead; schedule single visit.",
        "Issue consultation invoice (no project conversion).",
    ],
    expected="Lead status 'lost' acceptable post-consult; tax invoice for service.",
))
add(dict(
    id="TC-SMH-001",
    title="SMART HOME integration scope",
    module="Construction Type — Smart Home",
    priority="P2", type="Functional",
    pre="Smart home add-on to existing project.",
    steps=[
        "Create CO for smart-home scope ₹3.5L.",
        "Customer approves with OTP.",
        "Procurement vendor for IoT kits (specialty).",
        "QC includes commissioning checklist.",
    ],
    expected="CO approved; commissioning QC mandatory; warranty extended for IoT.",
))

# ============================== 25. INDIA COMPLIANCE =======================
add(dict(
    id="TC-IND-001", title="RERA project registration captured",
    module="India Compliance", priority="P1", type="Compliance",
    pre="Residential project >8 units.",
    steps=[
        "Project setup form.",
    ],
    expected="RERA number, authority, phase-wise breakup, registration date stored; PDFs uploaded.",
    risk="GAP — no RERA schema today.",
))
add(dict(
    id="TC-IND-002", title="HSN/SAC code stored per BOQ line for GST",
    module="India Compliance", priority="P1", type="Compliance",
    pre="Each BOQ item should carry HSN/SAC code.",
    steps=[
        "Create item without HSN.",
    ],
    expected="Validation requires HSN for goods or SAC for services. GAP if not enforced.",
    risk="GAP — confirm field exists on BoqItem.",
))
add(dict(
    id="TC-IND-003", title="TDS u/s 194C on contractor payment",
    module="India Compliance", priority="P1", type="Compliance",
    pre="Contractor PO ₹2L total (>₹30k single / ₹1L aggregate).",
    steps=[
        "Record vendor payment.",
    ],
    expected="TDS 1% individual / 2% company auto-deducted; TDS slip with section.",
    risk="GAP — model absent.",
))
add(dict(
    id="TC-IND-004", title="Aadhaar / PAN PII masked in UI and logs",
    module="India Compliance", priority="P1", type="Security",
    pre="Customer/labour with Aadhaar.",
    steps=[
        "View detail screen.",
        "Inspect logs.",
    ],
    expected="UI shows last-4 digits; logs do not contain raw value; encryption-at-rest verified.",
))
add(dict(
    id="TC-IND-005", title="Indian rupee number formatting (lakh/crore)",
    module="India Compliance", priority="P3", type="Localisation",
    pre="Amount ₹12,34,56,789.",
    steps=[
        "Open any financial dashboard.",
    ],
    expected="Displays '12,34,56,789' (Indian grouping), not '123,456,789'.",
))
add(dict(
    id="TC-IND-006", title="DSC/e-sign for high-value approvals",
    module="India Compliance", priority="P2", type="Security",
    pre="CO > ₹10L customer approval.",
    steps=[
        "Approve flow.",
    ],
    expected="DSC/Aadhaar-eSign integration provides legally-binding signature; PDF stamped.",
    risk="GAP — currently OTP only.",
))
add(dict(
    id="TC-IND-007", title="State-specific labour wage compliance (min wages)",
    module="India Compliance", priority="P2", type="Compliance",
    pre="Project in Karnataka.",
    steps=[
        "Set unskilled wage < state minimum.",
    ],
    expected="Validation warns and blocks unless override + reason.",
    risk="GAP — no minimum-wage table.",
))

# ============================== 26. SECURITY / NFR =========================
add(dict(
    id="TC-SEC-001", title="OWASP top-10: SQL injection on search fields",
    module="Security", priority="P1", type="Security",
    pre="Search params accept strings.",
    steps=[
        "Use payload ''; DROP TABLE leads;-- in name filter.",
    ],
    expected="No SQL error; query parameterized; row count unchanged.",
))
add(dict(
    id="TC-SEC-002", title="OWASP: stored XSS in notes",
    module="Security", priority="P1", type="Security",
    pre="Lead notes field.",
    steps=[
        "Save '<script>alert(1)</script>'.",
        "View notes elsewhere.",
    ],
    expected="Output encoded; CSP headers prevent inline scripts.",
))
add(dict(
    id="TC-SEC-003", title="CSRF protection on state-changing endpoints",
    module="Security", priority="P1", type="Security",
    pre="Logged in.",
    steps=[
        "From external origin perform POST.",
    ],
    expected="403 due to CSRF or SameSite cookie protections.",
))
add(dict(
    id="TC-SEC-004", title="IDOR — accessing another project's data by id",
    module="Security", priority="P1", type="Security",
    pre="User in Project A only.",
    steps=[
        "GET /customer-projects/{B-id} and downstream resources.",
    ],
    expected="403 via ProjectAccessGuard for every resource (BOQ, stages, invoices).",
))
add(dict(
    id="TC-SEC-005", title="Password storage hashing strength",
    module="Security", priority="P1", type="Security",
    pre="Code review.",
    steps=[
        "Inspect password column.",
    ],
    expected="bcrypt with cost ≥ 10 (or argon2id). No plaintext, no MD5/SHA1.",
))
add(dict(
    id="TC-SEC-006", title="Audit trail completeness for financial actions",
    module="Security", priority="P1", type="Compliance",
    pre="Various financial actions executed.",
    steps=[
        "Inspect audit tables.",
    ],
    expected="Every state change captured (who, when, before, after, reason).",
    risk="GAP — only BoqAuditLog exists; financial entities mostly unaudited.",
))
add(dict(
    id="TC-SEC-007", title="Secrets rotation — JWT signing keys",
    module="Security", priority="P2", type="Security",
    pre="Key rotation schedule.",
    steps=[
        "Rotate signing key.",
    ],
    expected="kid header in JWT; tokens signed by old key still valid during grace period.",
))
add(dict(
    id="TC-PERF-001", title="Gantt load for 1,000-task project within budget",
    module="Performance", priority="P2", type="Performance",
    pre="Project with 1,000 tasks + 1,500 predecessors.",
    steps=[
        "GET /projects/{id}/schedule/gantt.",
    ],
    expected="< 2s p95; single-pass predecessor lookup; no N+1 (verified via SQL log).",
))
add(dict(
    id="TC-PERF-002", title="Concurrent stage certification under load",
    module="Performance", priority="P2", type="Performance",
    pre="50 concurrent users certify different stages.",
    steps=[
        "Run k6/Gatling scenario.",
    ],
    expected="p95 < 1.5s; no deadlocks; correct row counts.",
))
add(dict(
    id="TC-PERF-003", title="Photo upload over 3G keeps app responsive",
    module="Performance", priority="P2", type="Mobile",
    pre="Throttle network to 3G.",
    steps=[
        "Upload 10 × 3 MB photos.",
    ],
    expected="Background upload; progress per file; resumable on disconnect.",
))
add(dict(
    id="TC-NFR-001", title="Multi-language support — at least English+Hindi+Kannada+Tamil",
    module="Localisation", priority="P3", type="Localisation",
    pre="Language toggle.",
    steps=[
        "Switch language on labour, site engineer, customer apps.",
    ],
    expected="Strings translated; date and currency localised.",
    risk="GAP — verify locale bundles.",
))
add(dict(
    id="TC-NFR-002", title="Accessibility — screen reader & WCAG AA on portal",
    module="Accessibility", priority="P3", type="Accessibility",
    pre="NVDA / TalkBack.",
    steps=[
        "Tab through key screens; verify labels, contrast, focus states.",
    ],
    expected="Critical flows usable with screen reader; contrast ratio ≥ 4.5.",
))
add(dict(
    id="TC-NFR-003", title="Disaster recovery — RPO/RTO",
    module="Reliability", priority="P2", type="Reliability",
    pre="Backup policy defined.",
    steps=[
        "Simulate primary DB loss; restore from backup.",
    ],
    expected="RPO ≤ 15 min; RTO ≤ 4 h documented and met.",
))
add(dict(
    id="TC-NFR-004", title="GDPR-style data export and delete (PII)",
    module="Privacy", priority="P2", type="Privacy",
    pre="Customer requests data export and erasure.",
    steps=[
        "Admin triggers export.",
        "After legal retention, trigger erasure.",
    ],
    expected="Export package emailed; erasure soft-deletes and anonymises records while preserving immutable financial audit (retention by law).",
    risk="GAP — verify policy exists in DPDP-Act era.",
))

# ============================== 27. INTEGRATION FLOWS ======================
add(dict(
    id="TC-INTG-001",
    title="BOQ approval triggers stage initialisation and Gantt baseline check",
    module="Integration", priority="P1", type="Integration",
    pre="Project with draft BOQ and tasks.",
    steps=[
        "Approve BOQ.",
        "Inspect /stages.",
    ],
    expected="Stages auto-created from approved snapshot; PM prompted to approve baseline.",
))
add(dict(
    id="TC-INTG-002",
    title="Stage certification publishes event consumed by downstream services",
    module="Integration", priority="P2", type="Integration",
    pre="Stage certified.",
    steps=[
        "Verify StagePaymentCertifiedEvent listeners executed (notifications, customer push, finance dashboard).",
    ],
    expected="All listeners idempotent; failure of one does not break others.",
))
add(dict(
    id="TC-INTG-003",
    title="Refund / overpayment scenario",
    module="Integration", priority="P2", type="Edge",
    pre="Customer paid ₹50k extra by mistake.",
    steps=[
        "Record receipt; reconcile.",
        "Initiate refund.",
    ],
    expected="Refund entity; ledger balanced; notification to customer.",
    risk="GAP — no refund entity in code.",
))
add(dict(
    id="TC-INTG-004",
    title="Time zone consistency (IST) across server and clients",
    module="Integration", priority="P2", type="Compliance",
    pre="Server UTC; clients IST.",
    steps=[
        "Create site report at 23:55 IST.",
    ],
    expected="Stored as UTC; displayed as IST consistently; DST not applicable (India).",
))
add(dict(
    id="TC-INTG-005",
    title="Multi-tenant isolation (if applicable)",
    module="Integration", priority="P1", type="Security",
    pre="Tenant T1 and T2 (if multi-tenant).",
    steps=[
        "Tenant-T1 user queries T2's project IDs.",
    ],
    expected="404/403 — row-level tenancy enforced.",
    risk="Confirm tenant boundary in DB queries.",
))


# ============================== 28. SELF-CRITIQUE PATCH — additional
# scenarios that emerged on second-pass review of the catalogue.
# ===========================================================================
add(dict(
    id="TC-PAY-EXT-001",
    title="UPI receipt reconciliation against payment schedule",
    module="Payment Reconciliation", priority="P1", type="Integration",
    pre="Customer pays ₹2.5L via UPI; UTR captured on bank statement.",
    steps=[
        "Import bank statement CSV (or webhook from PSP).",
        "Auto-match by UTR, amount, customer.",
        "PM confirms allocation against Stage-2 invoice.",
    ],
    expected="Receipt created; invoice marked PAID; ledger reconciled; "
             "no duplicate allocation; mismatch surfaces in exceptions queue.",
    risk="GAP — automated bank reconciliation not visible in code.",
))
add(dict(
    id="TC-PAY-EXT-002",
    title="NEFT/RTGS payment without UTR — manual allocation",
    module="Payment Reconciliation", priority="P2", type="Functional",
    pre="Customer paid via NEFT; bank UTR missing temporarily.",
    steps=[
        "Record receipt manually with mode='NEFT', reference='pending'.",
        "Later update with UTR.",
    ],
    expected="Receipt allowed in 'PENDING_CONFIRMATION' state until UTR; "
             "audit trail of update; cannot apply to invoice until confirmed.",
))
add(dict(
    id="TC-PAY-EXT-003",
    title="Cheque bounce reversal",
    module="Payment Reconciliation", priority="P1", type="Edge",
    pre="Receipt recorded against cheque ₹5L; cheque returned by bank.",
    steps=[
        "Reverse the receipt; restore invoice outstanding.",
        "Apply bounce-charges (if any).",
        "Notify customer.",
    ],
    expected="Invoice re-opened with original outstanding; bounce fee captured; immutable audit chain.",
    risk="GAP — no reversal entity confirmed.",
))
add(dict(
    id="TC-PAY-EXT-004",
    title="Partial payment allocated across multiple invoices",
    module="Payment Reconciliation", priority="P2", type="Functional",
    pre="₹3L received; outstanding invoices INV-12 ₹1L, INV-13 ₹2.5L.",
    steps=[
        "Allocate ₹1L to INV-12 and ₹2L to INV-13.",
    ],
    expected="Split applied; ₹0.5L remains unallocated; INV-13 still partially due; ledger balanced.",
))
add(dict(
    id="TC-PAY-EXT-005",
    title="Overpayment held as customer credit",
    module="Payment Reconciliation", priority="P2", type="Edge",
    pre="Receipt ₹10L vs invoice ₹9L.",
    steps=[
        "Allocate.",
    ],
    expected="₹1L stored as customer-credit ledger; auto-suggest on next invoice; can be refunded.",
))

# Construction safety / PPE / EHS
add(dict(
    id="TC-EHS-001",
    title="Daily PPE compliance checklist on site report",
    module="Safety / EHS", priority="P2", type="Compliance",
    pre="Daily site report screen.",
    steps=[
        "Submit report; PPE checklist mandatory (helmet, harness, boots, gloves).",
    ],
    expected="Cannot submit without PPE attestation; non-compliance triggers safety observation.",
    risk="GAP — PPE checklist not modelled.",
))
add(dict(
    id="TC-EHS-002",
    title="Safety incident reporting auto-creates investigation task",
    module="Safety / EHS", priority="P1", type="Integration",
    pre="Site report type=SAFETY_INCIDENT.",
    steps=[
        "Submit incident with severity HIGH.",
    ],
    expected="Incident logged; investigation task auto-assigned to QUALITY_SAFETY role; "
             "regulatory reporting clock starts (24h for major incidents).",
))
add(dict(
    id="TC-EHS-003",
    title="Tool-box-talk attendance recorded",
    module="Safety / EHS", priority="P3", type="Functional",
    pre="Morning tool-box talk.",
    steps=[
        "Site supervisor records attendees by Aadhaar/RFID.",
    ],
    expected="Attendance archived; absent workers flagged in attendance system.",
    risk="GAP — no TBT module.",
))

# Bulk operations
add(dict(
    id="TC-BULK-001",
    title="Bulk import of legacy BOQ from Excel",
    module="Bulk Operations", priority="P2", type="Functional",
    pre="Excel with 800 BOQ rows in standard template.",
    steps=[
        "Upload via BOQ Import.",
        "Preview with validation errors.",
        "Confirm import.",
    ],
    expected="Validation surface (missing HSN, bad rates) per row; only valid rows imported; "
             "transactional or row-error report.",
))
add(dict(
    id="TC-BULK-002",
    title="Bulk lead assignment to a sales rep",
    module="Bulk Operations", priority="P3", type="Functional",
    pre="50 unassigned leads.",
    steps=[
        "Select all; click Bulk Assign.",
    ],
    expected="All 50 reassigned; activity feed updated; notifications batched (not 50 individual pushes).",
))
add(dict(
    id="TC-BULK-003",
    title="Bulk export of attendance for payroll",
    module="Bulk Operations", priority="P2", type="Functional",
    pre="200 labourers, 30 days.",
    steps=[
        "Export Excel for date range.",
    ],
    expected="File generated under 10s; correct totals; PII handling per policy.",
))

# Punch list / snag list
add(dict(
    id="TC-PUNCH-001",
    title="Pre-handover snag list creation and closure",
    module="Handover / Punch list", priority="P2", type="Functional",
    pre="Project ready for handover.",
    steps=[
        "Walk-through with customer; record 20 snags with photos.",
        "Assign to contractor / subcontractor.",
        "Verify each; close once resolved.",
    ],
    expected="All snags must be CLOSED before final account moves to AGREED; report exportable.",
    risk="GAP — punch list module not explicitly modelled; uses Observations as proxy.",
))
add(dict(
    id="TC-PUNCH-002",
    title="Customer cannot close snags without acceptance",
    module="Handover / Punch list", priority="P2", type="Functional",
    pre="Snag in RESOLVED state.",
    steps=[
        "Customer reviews and either accepts or re-opens.",
    ],
    expected="Acceptance moves to CLOSED; re-open returns to IN_PROGRESS with comments.",
))

# Vendor blacklist
add(dict(
    id="TC-VEND-001",
    title="Vendor blacklist prevents new POs",
    module="Vendor Management", priority="P1", type="Functional",
    pre="Vendor V1 blacklisted with reason.",
    steps=[
        "Try to create PO with V1.",
    ],
    expected="Blocked with reason 'vendor blacklisted'; admin override possible with note.",
    risk="GAP — blacklist flag/field not seen.",
))
add(dict(
    id="TC-VEND-002",
    title="Vendor performance rating updated on completion",
    module="Vendor Management", priority="P3", type="Functional",
    pre="Vendor completed work order.",
    steps=[
        "PM rates on quality, timeliness, safety.",
    ],
    expected="Rating averaged; visible at vendor selection; >3 ratings required for trends.",
    risk="GAP — performance schema absent.",
))

# Customer self-service
add(dict(
    id="TC-CSELF-001",
    title="Customer changes own password and triggers re-login",
    module="Customer Portal", priority="P2", type="Security",
    pre="Customer logged in.",
    steps=[
        "Change password; confirm new password rules (>=10 chars, mixed).",
    ],
    expected="All other active sessions invalidated; next page forces re-login.",
))
add(dict(
    id="TC-CSELF-002",
    title="Customer downloads invoice PDF after MFA",
    module="Customer Portal", priority="P2", type="Security",
    pre="High-value invoice download.",
    steps=[
        "Click Download; OTP challenge.",
    ],
    expected="OTP gate; download URL pre-signed and short-TTL.",
    risk="GAP — verify OTP gating policy.",
))

# Mobile app lifecycle
add(dict(
    id="TC-MOB-001",
    title="Force-upgrade old Flutter clients on breaking API change",
    module="Mobile Lifecycle", priority="P1", type="Compatibility",
    pre="App version 2.3 installed; API requires ≥3.0.",
    steps=[
        "Open app.",
    ],
    expected="Force-upgrade screen; link to store; user cannot bypass.",
    risk="GAP — versioning headers and minimum-version policy to confirm.",
))
add(dict(
    id="TC-MOB-002",
    title="App handles 401 mid-action gracefully",
    module="Mobile Lifecycle", priority="P2", type="Resilience",
    pre="User mid-payment-receipt entry.",
    steps=[
        "Background app for >1 hour; foreground and submit.",
    ],
    expected="Silent token refresh; draft data preserved locally; no data loss.",
))
add(dict(
    id="TC-MOB-003",
    title="App localisation falls back to English on missing key",
    module="Mobile Lifecycle", priority="P3", type="Localisation",
    pre="Language=Hindi but a new key only present in en.",
    steps=[
        "Render screen.",
    ],
    expected="Falls back to en; logs missing-key telemetry; no blank label.",
))

# Email / SMS deliverability
add(dict(
    id="TC-COMM-001",
    title="Email delivery uses SPF/DKIM/DMARC aligned domain",
    module="Communications", priority="P2", type="Security",
    pre="Send password-reset email.",
    steps=[
        "Inspect email headers (mail-tester).",
    ],
    expected="SPF=pass, DKIM=pass, DMARC=pass; sender domain owned.",
))
add(dict(
    id="TC-COMM-002",
    title="SMS template approval (DLT) compliance for India",
    module="Communications", priority="P1", type="Compliance",
    pre="OTP and notification SMS templates.",
    steps=[
        "Inspect template registry against TRAI DLT records.",
    ],
    expected="Each template registered with sender ID and entity ID; "
             "non-template traffic rejected by gateway.",
    risk="GAP — DLT registration must be verified.",
))
add(dict(
    id="TC-COMM-003",
    title="WhatsApp Business API notification fallback when push fails",
    module="Communications", priority="P3", type="Resilience",
    pre="FCM delivery failed.",
    steps=[
        "After N minutes, fall back to WhatsApp template for critical events (overdue stage).",
    ],
    expected="Single message delivered; dedup with FCM if user comes online.",
    risk="GAP — WhatsApp fallback not in code.",
))

# Photo / EXIF / evidence
add(dict(
    id="TC-EVID-001",
    title="EXIF data preserved on site-report photo for audit",
    module="Evidence", priority="P2", type="Compliance",
    pre="Photo with GPS + timestamp EXIF.",
    steps=[
        "Upload; download via portal.",
    ],
    expected="EXIF preserved (or relevant fields stored separately) for forensic review.",
))
add(dict(
    id="TC-EVID-002",
    title="Reverse-image-search detects re-used stock photo",
    module="Evidence", priority="P3", type="Edge",
    pre="Site engineer attempts to submit a stock photo.",
    steps=[
        "Upload.",
    ],
    expected="Optional ML check (perceptual hash) flags duplicates / known stock; "
             "manual review.",
    risk="Future enhancement.",
))

# WBS template management
add(dict(
    id="TC-WBS-001",
    title="WBS template versioning preserves applied projects",
    module="WBS Templates", priority="P2", type="Functional",
    pre="Template v1 applied to 5 projects.",
    steps=[
        "Edit template to v2.",
    ],
    expected="v2 saved as new version; existing projects unaffected; "
             "creator can re-apply on opt-in.",
))
add(dict(
    id="TC-WBS-002",
    title="Template task with circular predecessor caught at template save",
    module="WBS Templates", priority="P2", type="Validation",
    pre="Template editor.",
    steps=[
        "Create A→B→A.",
    ],
    expected="Save blocked; cycle highlighted.",
))

# Project archive / restore
add(dict(
    id="TC-ARCH-001",
    title="Project archive moves to cold storage with read-only access",
    module="Project Lifecycle", priority="P2", type="Functional",
    pre="Project status COMPLETED, FA CLOSED, DLP expired.",
    steps=[
        "Admin archives.",
    ],
    expected="Project read-only; large assets moved to cold storage; "
             "audit/financial reports remain accessible.",
    risk="GAP — archive workflow not visible.",
))

# Holidays/regional
add(dict(
    id="TC-HOL-001",
    title="State-specific holidays override global calendar",
    module="Scheduling", priority="P3", type="Construction-Type",
    pre="Karnataka has 'Kannada Rajyotsava' on Nov 1.",
    steps=[
        "Project in Karnataka schedules task across Nov 1.",
    ],
    expected="Calendar excludes Nov 1 as non-working; Gantt reflects.",
    risk="Verify state-holiday override.",
))

# Concurrent device limit
add(dict(
    id="TC-SESS-001",
    title="Concurrent session limit for FINANCE role",
    module="Auth", priority="P2", type="Security",
    pre="FINANCE user.",
    steps=[
        "Login on 3 devices; attempt 4th.",
    ],
    expected="4th login refused or oldest invalidated; user notified.",
    risk="Policy decision; verify config.",
))

# API rate limit beyond auth
add(dict(
    id="TC-RATE-001",
    title="Transaction-mutation rate limit per user",
    module="Security", priority="P2", type="Security",
    pre="Single user makes 100 PUT /tasks/{id}/progress calls in 30s.",
    steps=[
        "Burst requests.",
    ],
    expected="429 after threshold; backoff hint header.",
    risk="GAP — global rate-limit policy not confirmed.",
))

# DPDP-Act consent
add(dict(
    id="TC-DPDP-001",
    title="Consent capture for marketing communication (DPDP)",
    module="Privacy", priority="P1", type="Compliance",
    pre="Lead capture form.",
    steps=[
        "Submit without consent checkbox.",
    ],
    expected="Form rejects unless explicit consent ticked; consent timestamp + IP stored.",
    risk="GAP — DPDP-Act 2023 requires explicit, recorded consent.",
))
add(dict(
    id="TC-DPDP-002",
    title="Customer revokes consent and data marketing stops",
    module="Privacy", priority="P1", type="Compliance",
    pre="Customer opts out via portal.",
    steps=[
        "Toggle consent off.",
    ],
    expected="Suppression list updated; subsequent SMS/email skipped; audit row created.",
))


# ---------------------------------------------------------------------------
# Identified gaps summary
# ---------------------------------------------------------------------------
GAPS = [
    ("G-01", "Auth", "No rate limiting on /auth/login and public lead endpoints — DoS/brute-force surface."),
    ("G-02", "Auth", "Refresh-token rotation present but no MFA / TOTP for FINANCE & DIRECTOR roles."),
    ("G-03", "ACL", "No role_assignment_history audit table."),
    ("G-04", "ACL", "Active sessions not invalidated when sensitive permissions are revoked."),
    ("G-05", "Leads", "convertLead() does check-then-create without transactional isolation — duplicate-project risk."),
    ("G-06", "Customers", "GSTIN format only regex-checked; no checksum or live GSTN validation."),
    ("G-07", "Customer Project", "Phase transitions not enforced monotonic; status SUSPENDED does not block all financial writes."),
    ("G-08", "BOQ", "executionQuantity not capped against contracted quantity → overbill risk."),
    ("G-09", "BOQ / DPC", "DPC customization unitRate not validated against catalog rate — audit gap."),
    ("G-10", "Procurement", "validateItemBudget() disabled in PurchaseOrderService — no material-level budget control."),
    ("G-11", "Procurement", "No e-way bill capture or validation for inter-state movement > ₹50,000."),
    ("G-12", "Procurement / Labour", "No TDS section, slip generation, or quarterly return integration."),
    ("G-13", "Labour", "No PF/ESI tracking — Indian labour-law compliance gap."),
    ("G-14", "Labour", "No state minimum-wage validation."),
    ("G-15", "Inventory", "No valuation method (FIFO/LIFO/WAC) or HSN/SAC on materials; no batch/serial tracking."),
    ("G-16", "Subcontract", "No DLP / defect-liability enforcement on retention release."),
    ("G-17", "Subcontract", "GST not modelled on subcontract work order."),
    ("G-18", "Stage Payments / Deductions / FA", "Idempotency keys not honoured on financial mutations."),
    ("G-19", "Final Account", "Optimistic locking (@Version) not confirmed; concurrent edits risk lost updates."),
    ("G-20", "Tax Invoice", "No IRP / IRN / QR e-invoicing integration (mandatory above turnover threshold)."),
    ("G-21", "Tax Invoice", "HSN/SAC code presence per line not enforced."),
    ("G-22", "Site Ops", "No GPS spoof / mock-location detection; no plausibility checks on geotag time-jumps."),
    ("G-23", "CCTV", "Stream URLs stored plaintext; no signed-URL or VPN gating verified."),
    ("G-24", "Documents", "AV scan and strict mime-type validation on uploads not verified."),
    ("G-25", "Notifications", "Push delivery is fire-and-forget without retry / dead-letter queue."),
    ("G-26", "Support", "No SLA tracking, escalation timers, or knowledge base."),
    ("G-27", "Compliance — RERA", "No project RERA registration, phase break-up disclosure schema."),
    ("G-28", "Compliance — DPDP/GDPR", "No data export & erasure (consumer-rights) workflow."),
    ("G-29", "Integration", "No refund / overpayment entity; reconciliation only one-way."),
    ("G-30", "Financial", "Decimal precision inconsistency (NUMERIC(18,6) vs NUMERIC(15,2)) — rounding leakage."),
    ("G-31", "Schedulers", "Overdue-stage transition, DLP retention release, payment reminders rely on jobs whose wiring is not confirmed in deployment."),
    ("G-32", "Change Orders", "ChangeRequestMergeService does not run cycle check on merged WBS edges (verify)."),
    ("G-33", "Localisation", "Hindi / Kannada / Tamil / Marathi UI strings not confirmed for field-ops apps."),
    ("G-34", "Accessibility", "WCAG AA conformance not declared."),
    ("G-35", "Lead Conversion", "Welcome email on conversion uses temp password — password should be one-time reset link only."),
    ("G-36", "Reconciliation", "No automated bank-statement / PSP-webhook reconciliation for UPI / NEFT / RTGS."),
    ("G-37", "Reconciliation", "No reversal entity for cheque bounce or payment chargeback."),
    ("G-38", "Reconciliation", "No customer-credit ledger for overpayment / advance carry-forward."),
    ("G-39", "Safety / EHS", "No PPE checklist on daily site report; safety culture not enforced in flow."),
    ("G-40", "Safety / EHS", "No tool-box-talk module; statutory record under BOCW Act missing."),
    ("G-41", "BOQ Import", "No bulk Excel import path for legacy BOQ data; manual entry only."),
    ("G-42", "Handover", "No first-class punch / snag list; relies on Observations module — limits handover gating."),
    ("G-43", "Vendor", "No vendor blacklist or performance-rating schema."),
    ("G-44", "Customer Portal", "Customer self-service password change does not invalidate other sessions."),
    ("G-45", "Mobile", "No minimum-version policy or force-upgrade gate confirmed in API."),
    ("G-46", "Communications", "TRAI DLT template registration & WhatsApp Business fallback not wired."),
    ("G-47", "Evidence", "EXIF retention strategy unclear — risk of stripping forensic data."),
    ("G-48", "WBS Templates", "Template versioning vs cascading edits to applied projects undefined."),
    ("G-49", "Project Lifecycle", "No archive / cold-storage workflow for completed projects past DLP."),
    ("G-50", "Scheduling", "State-level holiday overrides on top of global calendar not confirmed."),
    ("G-51", "Auth", "No concurrent-session policy (esp. for FINANCE / DIRECTOR roles)."),
    ("G-52", "Security", "Global API rate-limit policy for transactional mutations not confirmed."),
    ("G-53", "Privacy — DPDP", "No explicit consent capture or revocation workflow for marketing communications."),
]


# ---------------------------------------------------------------------------
# PDF rendering
# ---------------------------------------------------------------------------

def make_test_case_table(tc):
    """Return a flowable table for a single test case."""
    construction = tc.get('construction', '—')
    risk = tc.get('risk', '—')
    rows = [
        [P('<b>ID</b>'), P(tc['id']),
         P('<b>Module</b>'), P(tc['module'])],
        [P('<b>Title</b>'), P(tc['title']),
         P('<b>Priority</b>'), P(tc['priority'])],
        [P('<b>Type</b>'), P(tc['type']),
         P('<b>Construction Context</b>'), P(construction)],
        [P('<b>Pre-conditions</b>'),
         Paragraph(tc['pre'], CELL), '', ''],
        [P('<b>Test Steps / Flow</b>'),
         Paragraph(steps_to_html(tc['steps']), CELL), '', ''],
        [P('<b>Expected Result</b>'),
         Paragraph(tc['expected'], CELL), '', ''],
        [P('<b>Defect Risk / Notes</b>'),
         Paragraph(risk, CELL), '', ''],
    ]
    # Span the multi-row entries across 3 cols
    t = Table(rows, colWidths=[32*mm, 70*mm, 35*mm, 50*mm], hAlign='LEFT')
    t.setStyle(TableStyle([
        ('SPAN', (1, 3), (3, 3)),
        ('SPAN', (1, 4), (3, 4)),
        ('SPAN', (1, 5), (3, 5)),
        ('SPAN', (1, 6), (3, 6)),
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (0, -1), colors.HexColor('#EEF1F8')),
        ('BACKGROUND', (2, 0), (2, 2), colors.HexColor('#EEF1F8')),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('FONTSIZE', (0, 0), (-1, -1), 8),
    ]))
    return KeepTogether([Spacer(1, 3), t, Spacer(1, 5)])


def build_cover(story):
    story.append(Spacer(1, 60))
    story.append(Paragraph(
        "WallDot Construction Management Portal", COVER_T))
    story.append(Spacer(1, 8))
    story.append(Paragraph(
        "Production-Grade Test Case Catalog", COVER_T))
    story.append(Spacer(1, 18))
    story.append(Paragraph(
        "End-to-end coverage for residential, commercial, "
        "interior &amp; renovation construction projects in India",
        COVER_S))
    story.append(Spacer(1, 30))
    meta = [
        ["Document", "Test Case Catalogue (Master)"],
        ["Systems", "wd_portal_api (Spring Boot) + wd_portal_app_flutter"],
        ["Test cases", f"{len(TC)}"],
        ["Identified gaps", f"{len(GAPS)}"],
        ["Generated on", date.today().isoformat()],
        ["Author / Reviewer", "Senior QA — Construction Tech"],
        ["Status", "Baseline v1.0 — for review with PM, Engineering, Compliance"],
    ]
    t = Table(meta, colWidths=[55*mm, 100*mm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (0, -1), colors.HexColor('#EEF1F8')),
        ('FONTSIZE', (0, 0), (-1, -1), 10),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t)
    story.append(PageBreak())


def build_intro(story):
    story.append(Paragraph("1. Scope &amp; Strategy", H1))
    story.append(Paragraph(
        "This catalogue tests the construction-management portal across the "
        "complete lead-to-handover lifecycle for the four primary "
        "engagement types serviced in India — <b>Residential</b>, "
        "<b>Commercial</b>, <b>Interior</b> and <b>Renovation</b> — plus the "
        "secondary <b>Vastu</b> consultation and <b>Smart-Home</b> add-on "
        "scopes. Test design follows risk-based prioritisation (P1 / P2 / P3) "
        "with explicit weight on financial integrity, regulatory compliance "
        "(GST, TDS, RERA, DPDP), and field-ops resilience (offline sync, "
        "geo-tagged evidence).",
        BODY))
    story.append(Spacer(1, 6))
    story.append(Paragraph("Coverage dimensions", H2))
    coverage = [
        ["Dimension", "Examples"],
        ["Functional", "Happy-path flows for every screen and API"],
        ["Negative / Validation", "Missing fields, bad GSTIN, status guards"],
        ["Concurrency", "Duplicate conversion, idempotent mutations, locking"],
        ["Security", "OWASP top-10, IDOR, role escalation, secrets handling"],
        ["Compliance", "GST split, TDS, RERA, HSN/SAC, e-invoicing, DPDP"],
        ["Construction-type", "Residential / Commercial / Interior / Renovation flows"],
        ["Integration / E2E", "Lead→Project→BOQ→Stages→Final Account"],
        ["Performance", "Load on Gantt, certification, photo upload"],
        ["Resilience / Offline", "Mobile field-ops with intermittent network"],
        ["Localisation / A11y", "Indian numbering, vernacular UI, screen readers"],
    ]
    t = Table(coverage, colWidths=[55*mm, 110*mm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0B3D91')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t)
    story.append(Spacer(1, 10))

    story.append(Paragraph("2. Test environment &amp; data", H2))
    story.append(Paragraph(
        "<b>Roles seeded:</b> ADMIN, DIRECTOR, COMMERCIAL_MANAGER, "
        "PROJECT_MANAGER, FINANCE, SITE_ENGINEER, SALES, QUALITY_SAFETY, "
        "ESTIMATOR, INTERN, SITE_SUPERVISOR, PROCUREMENT_OFFICER, CUSTOMER, "
        "PARTNER (architect / consultant / contractor).<br/>"
        "<b>Test customers:</b> intra-state (Karnataka 29), inter-state "
        "(Maharashtra 27), unregistered (no GSTIN) → triggers CGST+SGST, "
        "IGST and B2C invoice paths respectively.<br/>"
        "<b>Test projects:</b> one per construction type pre-seeded with "
        "approved BOQ, an active subcontract, and a 12-week schedule.<br/>"
        "<b>Devices:</b> Android 11+, iOS 15+, Chrome / Edge / Safari "
        "current-2 versions. Network: WiFi, 4G, 3G, offline.<br/>"
        "<b>Calendars:</b> Indian public holidays + state holidays + "
        "monsoon window June–September.",
        BODY))

    story.append(Paragraph("3. Self-critique", H2))
    story.append(Paragraph(
        "Where this catalogue stops short and what to plug next:"
        "<br/>• Exploratory testing for Flutter widget edge cases (gesture "
        "conflicts, dynamic font scaling) requires device-cloud time and is "
        "represented only at fixture level."
        "<br/>• Pen-test scenarios (chained CSRF + IDOR, server-side "
        "request-forgery on signed URLs) need a dedicated security engagement."
        "<br/>• Load / soak (24h) testing is scoped at p95 only; full "
        "chaos-engineering (DB failover, FCM outage) is out of scope here."
        "<br/>• Tax-compliance verification ultimately requires CA sign-off "
        "and reconciliation against GSTR-1 filings — this catalogue surfaces "
        "the data needed for that audit, not the audit itself."
        "<br/>• Several gaps in the codebase (see §Gap Register) mean some "
        "test cases will <i>fail today</i>. Those are intentionally retained "
        "because they describe production-grade expectations the system "
        "should meet before customer roll-out.",
        BODY))
    story.append(PageBreak())


def build_section_index(story):
    story.append(Paragraph("4. Test case sections", H1))
    sections = sorted(set(t['module'] for t in TC))
    items = []
    for s in sections:
        count = sum(1 for t in TC if t['module'] == s)
        items.append([P(s), P(str(count))])
    table = Table(
        [[P('<b>Module / Area</b>'), P('<b>Test cases</b>')]] + items,
        colWidths=[125*mm, 35*mm], hAlign='LEFT')
    table.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0B3D91')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
    ]))
    story.append(table)
    story.append(PageBreak())


def build_test_cases(story):
    story.append(Paragraph("5. Test cases", H1))
    # group by module preserving insertion order
    seen = []
    for tc in TC:
        if tc['module'] not in seen:
            seen.append(tc['module'])
    for mod in seen:
        story.append(Paragraph(mod, H2))
        for tc in TC:
            if tc['module'] == mod:
                story.append(make_test_case_table(tc))
        story.append(Spacer(1, 4))


def build_gaps(story):
    story.append(PageBreak())
    story.append(Paragraph("6. Identified gaps &amp; defect risks", H1))
    story.append(Paragraph(
        "Each gap below is referenced from one or more test cases via the "
        "<b>Defect Risk / Notes</b> row. These were discovered during "
        "code-level mapping of controllers, services and Flyway migrations "
        "and should be triaged before production roll-out.", BODY))
    story.append(Spacer(1, 4))
    rows = [[P('<b>Gap ID</b>'), P('<b>Area</b>'), P('<b>Description</b>')]]
    for g in GAPS:
        rows.append([P(g[0]), P(g[1]), P(g[2])])
    t = Table(rows, colWidths=[20*mm, 35*mm, 120*mm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0B3D91')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTSIZE', (0, 0), (-1, -1), 8),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t)


def build_traceability(story):
    story.append(PageBreak())
    story.append(Paragraph("7. Construction-type traceability", H1))
    types = {
        "Residential": ["TC-RES-001", "TC-LEAD-001", "TC-CPRJ-001",
                        "TC-BOQ-001", "TC-DPC-005", "TC-STG-001",
                        "TC-LAB-001", "TC-FA-001"],
        "Commercial": ["TC-COM-001", "TC-CPRJ-001", "TC-BOQ-007",
                       "TC-PROC-008", "TC-SUB-001", "TC-TAX-003"],
        "Interior": ["TC-INT-001", "TC-LEAD-011", "TC-BOQ-010",
                     "TC-DPC-001", "TC-DPC-005"],
        "Renovation": ["TC-REN-001", "TC-GAL-001", "TC-BOQ-001",
                       "TC-PROC-004", "TC-CO-002"],
        "Vastu": ["TC-VAS-001", "TC-LEAD-001", "TC-TAX-002"],
        "Smart Home": ["TC-SMH-001", "TC-CO-001", "TC-WAR-002"],
    }
    rows = [[P('<b>Construction type</b>'), P('<b>Key test cases</b>')]]
    for k, v in types.items():
        rows.append([P(k), P(', '.join(v))])
    t = Table(rows, colWidths=[40*mm, 130*mm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0B3D91')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t)

    story.append(Spacer(1, 10))
    story.append(Paragraph("8. Priority distribution", H2))
    p1 = sum(1 for t in TC if t['priority'] == 'P1')
    p2 = sum(1 for t in TC if t['priority'] == 'P2')
    p3 = sum(1 for t in TC if t['priority'] == 'P3')
    rows = [
        [P('<b>Priority</b>'), P('<b>Count</b>'), P('<b>Definition</b>')],
        [P('P1'), P(str(p1)), P('Blocker — financial, security, compliance or data-loss critical')],
        [P('P2'), P(str(p2)), P('High — major functional or workflow breakage')],
        [P('P3'), P(str(p3)), P('Medium — usability, localisation, peripheral feature')],
    ]
    t = Table(rows, colWidths=[25*mm, 25*mm, 110*mm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0B3D91')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t)


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont('Helvetica', 8)
    canvas.setFillColor(colors.HexColor('#666666'))
    canvas.drawString(15*mm, 10*mm,
                      "WallDot Portal — Test Case Catalog (Confidential)")
    canvas.drawRightString(doc.pagesize[0]-15*mm, 10*mm,
                           f"Page {doc.page}")
    canvas.restoreState()


def build_doc(path):
    doc = SimpleDocTemplate(
        path, pagesize=landscape(A4),
        leftMargin=12*mm, rightMargin=12*mm,
        topMargin=14*mm, bottomMargin=15*mm,
        title="WallDot Portal Test Case Catalog",
        author="Senior QA",
    )
    story = []
    build_cover(story)
    build_intro(story)
    build_section_index(story)
    build_test_cases(story)
    build_gaps(story)
    build_traceability(story)
    doc.build(story, onFirstPage=header_footer, onLaterPages=header_footer)


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, "PORTAL_TEST_CASES.pdf")
    build_doc(out)
    print(f"Wrote {out}  ({len(TC)} test cases, {len(GAPS)} gaps)")
