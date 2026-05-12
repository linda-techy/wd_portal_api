"""
Production-grade Flutter-screen-centric test case catalogue for the
WallDot Construction Portal (lib/screens and lib/features in
wd_portal_app_flutter).

Every test case is anchored to a concrete Flutter screen file and lays out
the user's screen-by-screen flow as a tester would actually click it.

Author : Senior QA (Construction-tech, India)
Run    : python docs/generate_flutter_test_cases_pdf.py
Output : docs/PORTAL_FLUTTER_TEST_CASES.pdf
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
    KeepTogether,
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
CELL = ParagraphStyle('CELL', parent=styles['BodyText'], fontSize=8,
                      leading=10, alignment=TA_LEFT)
COVER_T = ParagraphStyle('COVERT', parent=styles['Title'], fontSize=26,
                         leading=30, alignment=TA_CENTER,
                         textColor=colors.HexColor('#0B3D91'))
COVER_S = ParagraphStyle('COVERS', parent=styles['Title'], fontSize=14,
                         leading=18, alignment=TA_CENTER,
                         textColor=colors.HexColor('#444444'))


def P(text):
    return Paragraph(text.replace('\n', '<br/>'), CELL)


def steps_to_html(steps):
    return '<br/>'.join(f"<b>{i}.</b> {s}" for i, s in enumerate(steps, 1))


# ---------------------------------------------------------------------------
# Test-case dataset
# ---------------------------------------------------------------------------
TC = []  # list of dicts


def add(d):
    TC.append(d)


# ============================== 1. AUTH & SESSION ==========================
add(dict(
    id="F-AUTH-001",
    title="Login with valid credentials on mobile portrait",
    screen="lib/screens/auth/portal_login_screen.dart",
    role="ADMIN / any portal user", priority="P1", type="Functional",
    pre="Active portal user; mobile in portrait; portal_login_screen open.",
    steps=[
        "Tap email field; enter valid email.",
        "Tap password field; enter valid password (>= 6 chars).",
        "Tap 'Sign In'.",
        "Observe button changes to spinner via isLoading.",
    ],
    expected=(
        "PortalAuthProvider authenticates. Router redirects to "
        "MainScreen / DashboardScreen. Bottom-nav appears with Menu / "
        "Projects / Profile (responsive mobile layout)."
    ),
))
add(dict(
    id="F-AUTH-002",
    title="Login email regex rejects malformed input",
    screen="lib/screens/auth/portal_login_screen.dart",
    role="any", priority="P2", type="Validation",
    pre="Login screen open.",
    steps=[
        "Enter 'foo@bar' (no TLD) in email.",
        "Enter any password and tap Sign In.",
    ],
    expected="Form validator (regex `[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}`) "
             "shows inline error. Shake animation triggers. Field focused.",
    flutter_gap="F-G02 — regex pre-2026 RFC; rejects valid TLDs > 4 chars (e.g. .museum).",
))
add(dict(
    id="F-AUTH-003",
    title="Rapid double-tap on Sign In does not double-submit",
    screen="lib/screens/auth/portal_login_screen.dart",
    role="any", priority="P2", type="Race / UI",
    pre="Login screen, valid credentials filled.",
    steps=[
        "Tap Sign In twice in < 100 ms.",
    ],
    expected="Button disables on first tap; second tap is a no-op. Only one "
             "POST /auth/login fires.",
    flutter_gap="F-G03 — disable flag races with setState; verify gating logic.",
))
add(dict(
    id="F-AUTH-004",
    title="Forgot-password generic 200 response prevents account enumeration",
    screen="lib/screens/auth/forgot_password_screen.dart",
    role="any", priority="P1", type="Security",
    pre="Forgot-password screen open.",
    steps=[
        "Submit a known email — observe success view.",
        "Submit a random unknown email — observe response.",
    ],
    expected="Both submits show the same 'Check Your Email' success view. "
             "No timing difference, no error banner, no info-leak.",
))
add(dict(
    id="F-AUTH-005",
    title="Reset-password token-length validation gate",
    screen="lib/screens/auth/reset_password_screen.dart",
    role="any", priority="P2", type="Validation",
    pre="Open /reset-password?token=foo (length <= 20).",
    steps=[
        "Wait for build.",
    ],
    expected="_buildInvalidTokenView() renders 'broken link' state — UI never "
             "shows the password form.",
    flutter_gap="F-G02 — token guard is only `length > 20`, not a UUID/sig check.",
))
add(dict(
    id="F-AUTH-006",
    title="Password mismatch is caught client-side",
    screen="lib/screens/auth/reset_password_screen.dart",
    role="any", priority="P2", type="Validation",
    pre="Valid reset token in URL.",
    steps=[
        "Enter password 'Abc12345' and confirm 'Abc12346'.",
        "Tap Reset.",
    ],
    expected="Confirm-password validator fires 'Passwords do not match'. No API call.",
))
add(dict(
    id="F-AUTH-007",
    title="Profile screen — change password invalidates current session",
    screen="lib/screens/profile/profile_screen.dart",
    role="any logged-in", priority="P1", type="Security",
    pre="User logged in; profile screen open.",
    steps=[
        "Tap 'Change Password'.",
        "Enter current, new (>= 8 chars), confirm; tap Save.",
    ],
    expected=(
        "Snackbar success. Per backend G-44 fix all refresh tokens revoked. "
        "UI should also force re-login on this device — currently does not."
    ),
    flutter_gap="F-G36 — Flutter does not call logout/relogin after password change.",
))
add(dict(
    id="F-AUTH-008",
    title="Logout returns to login and clears notification badge",
    screen="lib/screens/profile/profile_screen.dart",
    role="any", priority="P2", type="Functional",
    pre="Logged in; unread notifications present.",
    steps=[
        "Open Profile → tap Logout.",
    ],
    expected="GoRouter redirects to /login. Notification badge in MainScreen "
             "AppBar disappears. Secure storage cleared.",
))
add(dict(
    id="F-AUTH-009",
    title="Avatar initial does not crash on empty firstName",
    screen="lib/screens/main/components/side_menu.dart",
    role="any", priority="P2", type="Edge",
    pre="Backend returns user with empty firstName.",
    steps=[
        "Login; observe side-menu footer & profile screen header.",
    ],
    expected="Avatar shows '?' / placeholder. No String.charAt(0) exception.",
    flutter_gap="F-G09 — code uses `firstName[0]` without empty guard.",
))

# ============================== 2. MAIN SHELL / SIDE MENU =================
add(dict(
    id="F-NAV-001",
    title="Side menu groups expand/collapse with state retained",
    screen="lib/screens/main/components/side_menu.dart",
    role="any portal user", priority="P2", type="UI",
    pre="Desktop (>=1100px) layout; SideMenu visible.",
    steps=[
        "Collapse the CRM group; navigate to a screen and return.",
    ],
    expected="Group state persists for the session — collapse flag preserved.",
    flutter_gap="F-G17 — verify expansion map is provider-backed, not StatefulWidget local.",
))
add(dict(
    id="F-NAV-002",
    title="Menu items hidden when user lacks permission",
    screen="lib/screens/main/components/side_menu.dart",
    role="SITE_ENGINEER", priority="P1", type="Security / RBAC",
    pre="Site engineer login (no FINANCE perms).",
    steps=[
        "Open side menu.",
    ],
    expected="Finance, Procurement, ACL items hidden via "
             "PermissionProvider.isVisible() callbacks.",
))
add(dict(
    id="F-NAV-003",
    title="Bottom nav (mobile) — exactly three items map correctly",
    screen="lib/screens/main/main_screen.dart",
    role="any", priority="P2", type="Responsive",
    pre="Mobile / tablet (<1100px).",
    steps=[
        "Tap each bottom-nav icon: Menu, Projects, Profile.",
    ],
    expected="Menu opens Drawer with SideMenu. Projects routes /projects "
             "(index 3). Profile routes /profile (index 15). No flicker, no "
             "index drift if items added later.",
    flutter_gap="F-G14 — hard-coded indices fragile to menu reordering.",
))
add(dict(
    id="F-NAV-004",
    title="Notification badge — stale value after long background",
    screen="lib/screens/main/main_screen.dart",
    role="any", priority="P2", type="Resilience",
    pre="App backgrounded > 30 min with notifications arriving.",
    steps=[
        "Bring app to foreground.",
        "Observe badge count.",
    ],
    expected="On AppLifecycleState.resumed, MainScreen should re-fetch "
             "unread-count. Today it only fetches in initState and after "
             "returning from PortalNotificationScreen.",
    flutter_gap="F-G31 — no lifecycle observer for badge refresh.",
))
add(dict(
    id="F-NAV-005",
    title="Offline banner appears and links to /sync/pending",
    screen="lib/widgets/offline_banner.dart (used in MainScreen)",
    role="any", priority="P1", type="Offline",
    pre="Mobile/desktop; outbox has >=1 pending entry; toggle airplane mode.",
    steps=[
        "Turn airplane mode on.",
        "Observe banner above main content.",
        "Tap banner.",
    ],
    expected="Banner: 'Offline — X items queued'. Tap routes to PendingSyncScreen.",
))
add(dict(
    id="F-NAV-006",
    title="Deep link to /reset-password?token=… opens reset screen pre-auth",
    screen="lib/config/router.dart",
    role="anonymous", priority="P1", type="Routing",
    pre="App cold-start.",
    steps=[
        "Open device deep link wdportal://reset-password?token=<uuid>.",
    ],
    expected="ResetPasswordScreen renders directly with token bound; router "
             "redirect logic skips /login for this route.",
))
add(dict(
    id="F-NAV-007",
    title="ProjectDetailScreen has no deep link",
    screen="lib/features/projects/presentation/screens/project_detail_screen.dart",
    role="PM", priority="P3", type="UX gap",
    pre="App running.",
    steps=[
        "Try /projects/42 via browser URL or deep link.",
    ],
    expected=(
        "Today: route not registered — falls back to projects list. Bookmark "
        "or share a single project is impossible (except for DPC builder, "
        "which uses go_router)."
    ),
    flutter_gap="F-G03 — no go_router entry for /projects/:id detail.",
))
add(dict(
    id="F-NAV-008",
    title="Responsive breakpoint switch (rotate device)",
    screen="lib/responsive.dart (used everywhere)",
    role="any", priority="P2", type="Responsive",
    pre="Tablet at 900px width portrait.",
    steps=[
        "Rotate to landscape (>=1100px).",
    ],
    expected="MainScreen switches from Drawer mode to side-by-side SideMenu + "
             "content. AppBar hamburger disappears.",
))

# ============================== 3. DASHBOARD ==============================
add(dict(
    id="F-DSH-001",
    title="Dashboard skeleton vs. data render",
    screen="lib/screens/dashboard/crm_dashboard_modern.dart",
    role="ADMIN / PM", priority="P2", type="Functional",
    pre="Authenticated.",
    steps=[
        "Open dashboard cold.",
    ],
    expected="Shimmer skeleton (4 cards × 120px) shows then resolves to KPI "
             "tiles. 'Updated X mins ago' label appears bottom.",
    flutter_gap="F-G18 — skeleton hard-coded to 4 cards regardless of API payload.",
))
add(dict(
    id="F-DSH-002",
    title="Pull-to-refresh updates timestamps",
    screen="lib/screens/dashboard/crm_dashboard_modern.dart",
    role="any", priority="P3", type="UX",
    pre="Dashboard open for >5 mins.",
    steps=[
        "Pull down on the scrollable area.",
    ],
    expected="RefreshIndicator runs _loadData(); _lastUpdated resets to now.",
))
add(dict(
    id="F-DSH-003",
    title="Granular error per chart not supported",
    screen="lib/screens/dashboard/crm_dashboard_modern.dart",
    role="any", priority="P3", type="Resilience gap",
    pre="One chart endpoint returns 500.",
    steps=[
        "Reload dashboard.",
    ],
    expected="Entire dashboard shows a single error widget. Chart-level "
             "fallback (other charts succeed) not implemented.",
    flutter_gap="F-G18 — single error state for compound dashboard.",
))

# ============================== 4. LEADS ==================================
add(dict(
    id="F-LEAD-001",
    title="Lead list pagination via scroll-to-load",
    screen="lib/features/leads/presentation/screens/leads_screen.dart",
    role="SALES / ADMIN", priority="P1", type="Functional",
    pre="50+ leads in backend.",
    steps=[
        "Open Leads screen.",
        "Scroll list; observe 'Loading more…' indicator at 200 px from end.",
    ],
    expected="Pages fetched at threshold; no duplicate fetch (loadingMore "
             "flag set). Scroll position preserved when items appended.",
    flutter_gap="F-G19 — no debounce guard; rapid scroll can re-trigger.",
))
add(dict(
    id="F-LEAD-002",
    title="Filter chips combine with search across pagination",
    screen="lib/features/leads/presentation/screens/leads_screen.dart",
    role="SALES", priority="P1", type="Functional",
    pre="Mixed-status leads exist.",
    steps=[
        "Type 'Anita' in search.",
        "Select status='qualified'.",
        "Select projectType='residential_construction'.",
    ],
    expected="Search + filters compose into a single PaginationParams; result "
             "set drops to matching leads only; page resets to 0.",
))
add(dict(
    id="F-LEAD-003",
    title="Add Lead — public/walk-in (no email)",
    screen="lib/features/leads/presentation/screens/add_lead_screen.dart",
    role="SALES", priority="P1", type="Functional",
    pre="Add Lead form open.",
    steps=[
        "Fill name, phone, projectType='residential_construction', "
        "state='Karnataka', district='Bengaluru', budget=4500000.",
        "Leave email blank.",
        "Set source='directWalkin'.",
        "Save.",
    ],
    expected="Lead created; row appears in list with COLD/WARM/HOT badge "
             "computed from scoring factors. Welcome email NOT triggered "
             "(no email).",
    construction="Residential walk-in customer.",
))
add(dict(
    id="F-LEAD-004",
    title="Lead score recompute on budget edit",
    screen="lib/features/leads/presentation/screens/edit_lead_screen.dart",
    role="SALES", priority="P2", type="Integration",
    pre="Lead with budget < 10L (LOW factor).",
    steps=[
        "Edit budget to 60,00,000.",
        "Save.",
        "Open Lead Score History tab.",
    ],
    expected="Score category transitions COLD/WARM → HOT; LeadScoreHistory "
             "row visible with previous/new + JSON factors.",
))
add(dict(
    id="F-LEAD-005",
    title="Architect referral banner shows on edit",
    screen="lib/features/leads/presentation/screens/edit_lead_screen.dart",
    role="SALES", priority="P2", type="UX",
    pre="Lead.leadSource = 'referralArchitect' with linked partner.",
    steps=[
        "Open Edit Lead.",
    ],
    expected="Purple banner with partner firm + phone fetched via "
             "PartnershipAdminService. 'Loading partner info…' shows briefly.",
    flutter_gap="F-G18 — partner fetched only on initial build; not refreshed "
                "when user changes source dropdown mid-edit.",
))
add(dict(
    id="F-LEAD-006",
    title="Lead Activity timeline merges interactions + system events",
    screen="lib/features/leads/presentation/screens/lead_activity_screen.dart",
    role="SALES / PM", priority="P2", type="Functional",
    pre="Lead with 3 interactions, 1 score change, 1 doc upload.",
    steps=[
        "Open Activity tab.",
    ],
    expected="Timeline shows mixed events in reverse chronological order; "
             "icons differ by event type.",
))
add(dict(
    id="F-LEAD-007",
    title="Quick-log interaction schedules next follow-up",
    screen="lib/features/leads/presentation/screens/lead_interactions_screen.dart",
    role="SALES", priority="P1", type="Functional",
    pre="Lead in 'qualified'.",
    steps=[
        "Tap '+'; choose 'Quick Log'.",
        "Type='CALL', outcome='SCHEDULED_FOLLOWUP', nextActionDate=+3d.",
        "Save.",
    ],
    expected="Card appears top of list; nextFollowUp persisted; "
             "FollowUpsScreen 'Overdue' tab will surface on D+3.",
))
add(dict(
    id="F-LEAD-008",
    title="Document upload restricts to whitelisted extensions",
    screen="lib/features/leads/presentation/screens/lead_documents_screen.dart",
    role="SALES", priority="P1", type="Validation",
    pre="Lead documents tab open.",
    steps=[
        "Upload a .exe file via file picker.",
        "Upload a 2 MB JPG.",
    ],
    expected=".exe rejected with helpful message (whitelist: pdf, jpg, jpeg, "
             "png, webp, doc, docx, xls, xlsx, ppt, pptx, csv, txt). JPG "
             "succeeds.",
    flutter_gap="F-G34 — no upload progress shown; no virus-scan client-side.",
))
add(dict(
    id="F-LEAD-009",
    title="Estimation wizard — budgetary mode with confidence",
    screen="lib/features/lead_estimation/presentation/screens/lead_estimation_wizard_screen.dart",
    role="ESTIMATOR / SALES", priority="P1", type="Construction-Type",
    pre="Lead with projectType='interior_work'.",
    steps=[
        "Open 'Generate Estimation' from Lead Quotations.",
        "Step 1: choose package 'Interior Premium'.",
        "Step 2: switch to budgetary mode; enter estimatedArea=1200, "
        "confidenceLevel='Medium'.",
        "Step 3-4: skip (catalog endpoints stub).",
        "Step 5: enter discount 5%, GST 18%, validUntil=+30d. Save.",
    ],
    expected=(
        "WizardDraft sent to LeadEstimationsProvider; preview JSON computed; "
        "estimation row created and visible in lead's quotations list."
    ),
    construction="Interior — design-only catalog, no structural items.",
))
add(dict(
    id="F-LEAD-010",
    title="Estimation detail — revise opens wizard with pre-fill",
    screen="lib/features/lead_estimation/presentation/screens/estimation_detail_screen.dart",
    role="ESTIMATOR", priority="P2", type="Functional",
    pre="Estimation in DRAFT or SENT status.",
    steps=[
        "Open estimation detail.",
        "Tap 'Revise'.",
    ],
    expected="LeadEstimationWizardScreen opens with existing values bound "
             "to WizardDraft. Save creates a new revision.",
))
add(dict(
    id="F-LEAD-011",
    title="Estimation PDF download shows action spinner",
    screen="lib/features/lead_estimation/presentation/screens/estimation_detail_screen.dart",
    role="any", priority="P3", type="UX",
    pre="Estimation in ISSUED.",
    steps=[
        "Tap PDF download icon.",
    ],
    expected="AppBar action shows CircularProgressIndicator until "
             "FileDownloadHelper completes.",
))
add(dict(
    id="F-LEAD-012",
    title="Follow-ups screen — overdue badge accuracy",
    screen="lib/screens/follow_ups/follow_ups_screen.dart",
    role="SALES", priority="P2", type="Functional",
    pre="Two leads with nextFollowUp = yesterday.",
    steps=[
        "Open Follow-ups → Overdue tab.",
    ],
    expected="Both leads listed; overdue chip red; tap routes to lead detail.",
    flutter_gap="F-G19 — type counts computed from current page only.",
))
add(dict(
    id="F-LEAD-013",
    title="Communication screen — global interaction search debounce",
    screen="lib/screens/communication/communication_screen.dart",
    role="any", priority="P3", type="Performance",
    pre="1000+ interactions in DB.",
    steps=[
        "Type 'anita' rapidly in search.",
    ],
    expected=(
        "Today: every keystroke triggers a reload (no debounce). Goal: "
        "300ms debounce; cancel in-flight request."
    ),
    flutter_gap="F-G19 — search not debounced.",
))
add(dict(
    id="F-LEAD-014",
    title="Lead tasks tab loading + delete confirm",
    screen="lib/features/leads/presentation/screens/lead_tasks_screen.dart",
    role="PM", priority="P2", type="Functional",
    pre="Lead with 5 tasks.",
    steps=[
        "Open Tasks tab.",
        "Tap delete on a task.",
    ],
    expected="Loading spinner → tasks sorted by dueDate asc. Delete shows "
             "confirmation dialog; on confirm list reloads (no optimistic UI).",
    flutter_gap="F-G18 — no optimistic UI on update/delete.",
))
add(dict(
    id="F-LEAD-015",
    title="Document viewer renders PDF + image with auth header",
    screen="lib/features/leads/presentation/screens/document_viewer_screen.dart",
    role="any", priority="P2", type="Functional",
    pre="Lead with 1 PDF + 1 JPG document.",
    steps=[
        "Open PDF; open JPG.",
    ],
    expected="SfPdfViewer (memory/network) renders PDF. Image opens in "
             "InteractiveViewer (pinch zoom 0.5x–4x).",
    flutter_gap="F-G24 — no print/share/annotate; no video preview.",
))

# ============================== 5. CUSTOMERS ==============================
add(dict(
    id="F-CUST-001",
    title="Customer list filter chips and search",
    screen="lib/features/customers/presentation/screens/customers_screen.dart",
    role="SALES / PM", priority="P2", type="Functional",
    pre="Mixed individual/business customers seeded.",
    steps=[
        "Tap 'Business' chip.",
        "Type 'Acme'.",
    ],
    expected="List filtered to business customers whose name contains 'Acme'. "
             "Pagination footer updates totalPages.",
))
add(dict(
    id="F-CUST-002",
    title="Add Customer — GST number validation",
    screen="lib/features/customers/presentation/screens/add_customer_screen.dart",
    role="SALES", priority="P1", type="Validation / India",
    pre="Add Customer form open.",
    steps=[
        "Enter GSTIN '29ABCDE1234F1Z5'.",
        "Enter GSTIN '29ABCDE1234F1Z' (14 chars).",
        "Enter GSTIN '00ABCDE1234F1Z5' (invalid state 00).",
    ],
    expected="First accepted. Second rejected (length). Third rejected "
             "(state-code check).",
    flutter_gap="F-G06 — GSTIN format only regex-checked; no checksum.",
))
add(dict(
    id="F-CUST-003",
    title="Edit Customer — password blank means 'keep current'",
    screen="lib/features/customers/presentation/screens/edit_customer_screen.dart",
    role="ADMIN", priority="P2", type="UX",
    pre="Customer exists with password.",
    steps=[
        "Open Edit; leave password blank; change phone.",
        "Save.",
    ],
    expected="Phone updated; customer can still login with previous password.",
    flutter_gap="F-G18 — placeholder text could mislead user; needs explicit "
                "'leave blank to keep current' helper text.",
))
add(dict(
    id="F-CUST-004",
    title="Customer detail — soft-deactivate vs strict delete distinguishable",
    screen="lib/features/customers/presentation/screens/customer_detail_screen.dart",
    role="ADMIN", priority="P1", type="Functional",
    pre="Customer linked to active project.",
    steps=[
        "Open detail.",
        "Tap 'Deactivate' (enabled toggle off).",
        "Then tap 'Delete'.",
    ],
    expected="Deactivate succeeds (PATCH /enabled). Delete refused with 409 "
             "listing active references; UI shows reason.",
))
add(dict(
    id="F-CUST-005",
    title="Reset password email — single-click guard",
    screen="lib/features/customers/presentation/screens/customer_detail_screen.dart",
    role="ADMIN", priority="P2", type="Race",
    pre="Customer with email.",
    steps=[
        "Tap 'Send Password Reset' twice rapidly.",
    ],
    expected="_isSendingReset disables button; only one email sent.",
))
add(dict(
    id="F-CUST-006",
    title="Customer project documents — category filter",
    screen="lib/screens/customer_projects/project_documents_screen.dart",
    role="PM", priority="P2", type="Functional",
    pre="Project with docs in 3 categories.",
    steps=[
        "Open project documents.",
        "Tap 'Permits' chip.",
    ],
    expected="List filters to permit docs; chip horizontal scroller does "
             "NOT auto-scroll to selected (UX issue).",
    flutter_gap="F-G17 — selected chip not auto-scrolled into view.",
))
add(dict(
    id="F-CUST-007",
    title="Project payments — design payment 'no record' state",
    screen="lib/screens/customer_projects/project_payments_screen.dart",
    role="PM", priority="P2", type="Functional",
    pre="Brand-new project with no design payment yet.",
    steps=[
        "Open project payments.",
    ],
    expected="Empty state icon + 'No payment records' (backend returns "
             "200 + data:null per recent change).",
))
add(dict(
    id="F-CUST-008",
    title="Customer detail — initial-customer fast render",
    screen="lib/features/customers/presentation/screens/customer_detail_screen.dart",
    role="any", priority="P3", type="Performance / UX",
    pre="Coming from list with cached row.",
    steps=[
        "Tap a customer row.",
    ],
    expected="Detail renders immediately from initialCustomer; background "
             "fetch refreshes; refresh failure silent.",
    flutter_gap="F-G18 — silent failure on background refresh; user unaware "
                "if data is stale.",
))
add(dict(
    id="F-CUST-009",
    title="Customer project — phase chip edit dialog",
    screen="lib/features/projects/presentation/screens/project_detail_screen.dart",
    role="PM / ADMIN", priority="P2", type="Functional",
    pre="Project in PLANNING.",
    steps=[
        "Tap the phase chip.",
        "Pick DESIGN.",
        "Confirm.",
    ],
    expected="SimpleDialog with 5 options. Save calls "
             "CustomerProjectService.updateProject. Header re-renders DESIGN "
             "chip.",
    flutter_gap="F-G15 — no monotonic-transition guard client-side; user "
                "could pick PLANNING from CONSTRUCTION (backend should reject "
                "per backend G-07 gap).",
))
add(dict(
    id="F-CUST-010",
    title="GPS lock card — admin-only override visible",
    screen="lib/features/projects/presentation/screens/project_detail_screen.dart",
    role="ADMIN vs PM", priority="P2", type="Security / RBAC",
    pre="Project with locked GPS.",
    steps=[
        "Open project as PM — note no override button.",
        "Logout, login as ADMIN — observe.",
    ],
    expected="canOverride bound to roleCode == 'ADMIN'. PM sees read-only GPS.",
))

# ============================== 6. PROJECT DETAIL / GANTT / TASK PROGRESS =
add(dict(
    id="F-PRJ-001",
    title="Project module grid renders 20+ tiles responsively",
    screen="lib/features/projects/presentation/screens/project_detail_screen.dart",
    role="PM", priority="P2", type="Responsive",
    pre="Tablet 850px width.",
    steps=[
        "Open project detail.",
    ],
    expected="GridView with 2-4 columns (responsive). Tile tap routes to "
             "module screen; DPC tile uses context.go('/dpc/builder/{id}'); "
             "others use Navigator.push.",
    flutter_gap="F-G03 — mixed routing approach inconsistent.",
))
add(dict(
    id="F-PRJ-002",
    title="Schedule-config tab hidden silently without perms",
    screen="lib/features/projects/presentation/screens/project_detail_screen.dart",
    role="SITE_ENGINEER", priority="P2", type="UX gap",
    pre="Site engineer (no HOLIDAY_VIEW, no PROJECT_SCHEDULE_CONFIG_EDIT).",
    steps=[
        "Open project detail.",
    ],
    expected=(
        "Today: schedule-config tab simply not rendered — user has no idea it "
        "exists. Better: show greyed-out tab with 'Requires permission X'."
    ),
    flutter_gap="F-G10 — silent gating; no Access Denied affordance.",
))
add(dict(
    id="F-GNT-001",
    title="Gantt loads with critical path highlighted red",
    screen="lib/features/projects/presentation/screens/gantt_screen.dart",
    role="PM", priority="P1", type="Construction-Domain",
    pre="Project with 30 tasks, predecessors, CPM computed.",
    steps=[
        "Open Project → Gantt.",
    ],
    expected="GanttCpmProvider loads; critical tasks rendered in red; float "
             "tasks show amber float bar; today line dashed orange.",
))
add(dict(
    id="F-GNT-002",
    title="Gantt — monsoon-sensitive chip surfaces in monsoon window",
    screen="lib/features/projects/presentation/screens/gantt_screen.dart",
    role="PM / Site engineer", priority="P2", type="Construction-Domain",
    pre="Project in Karnataka; monsoon Jun–Sep configured; task scheduled "
        "in July.",
    steps=[
        "Open Gantt in current monsoon window.",
    ],
    expected="Monsoon icon + warning chip on task label row.",
    flutter_gap="F-G12 — flag visible only in Gantt; absent from task list / "
                "create screens.",
))
add(dict(
    id="F-GNT-003",
    title="Gantt 100-task performance",
    screen="lib/features/projects/presentation/screens/gantt_screen.dart",
    role="PM", priority="P2", type="Performance",
    pre="Project with 150 tasks.",
    steps=[
        "Open Gantt on mid-range Android (4GB RAM).",
        "Scroll the chart laterally and vertically.",
    ],
    expected="< 16 ms frame time. Today: ListView.separated may jank > 100 "
             "rows.",
    flutter_gap="F-G04 — non-virtualised chart row builder.",
))
add(dict(
    id="F-GNT-004",
    title="Inline task date edit dialog — progress slider clamps 0..100",
    screen="lib/features/projects/presentation/screens/gantt_screen.dart",
    role="PM", priority="P2", type="Validation",
    pre="Gantt open.",
    steps=[
        "Tap a bar; pick start/end dates; drag progress to 100%; Save.",
    ],
    expected="PUT /tasks/{id}/schedule succeeds; bar reflows; CPM "
             "recomputes; status auto-derives COMPLETED.",
))
add(dict(
    id="F-GNT-005",
    title="Predecessor cycle introduced via Gantt edit",
    screen="lib/features/projects/presentation/screens/gantt_screen.dart",
    role="PM", priority="P1", type="Construction-Domain",
    pre="A→B→C exists.",
    steps=[
        "Add predecessor C→A via predecessor dialog.",
    ],
    expected="Server rejects with CycleDetectedException. UI shows snackbar.",
    flutter_gap="F-G05 — no client-side BFS check before submit.",
))
add(dict(
    id="F-TSK-PROG-001",
    title="Task progress entry — offline-aware mark-complete",
    screen="lib/features/projects/presentation/screens/task_progress_entry_screen.dart",
    role="SITE_ENGINEER (mobile)", priority="P1", type="Offline",
    pre="Mobile in airplane mode; project assigned.",
    steps=[
        "Open Project → Tasks → Update Progress.",
        "Filter='Active'; tap a task.",
        "Slide progress to 100 %.",
        "Tap 'Mark Complete'.",
        "Grant camera + location.",
    ],
    expected=(
        "States cycle: idle → capturing → queued. Banner 'Queued — syncs "
        "when online'. OutboxService stores SiteReportCreate+TaskMarkComplete."
    ),
    construction="Residential field-ops day-to-day.",
))
add(dict(
    id="F-TSK-PROG-002",
    title="Progress entry without photo allowed but logged",
    screen="lib/features/projects/presentation/screens/task_progress_entry_screen.dart",
    role="SITE_ENGINEER", priority="P2", type="Functional",
    pre="Online.",
    steps=[
        "Set progress 60 %; add note; Save.",
    ],
    expected="Progress saved without photo. (Photo only mandatory on mark-"
             "complete.)",
    flutter_gap="F-G39 — distinction between 'update %' (no photo) and "
                "'mark complete' (photo required) easy to miss; needs clearer UX.",
))
add(dict(
    id="F-TSK-PROG-003",
    title="GPS denied surfaces actionable error state",
    screen="lib/features/projects/presentation/screens/task_progress_entry_screen.dart",
    role="SITE_ENGINEER", priority="P1", type="Negative",
    pre="Location permission revoked.",
    steps=[
        "Try Mark Complete.",
    ],
    expected="State 'errorGpsUnavailable' shown with explanatory text + "
             "Retry button.",
))
add(dict(
    id="F-PM-APP-001",
    title="PM approval inbox — photo thumbnail, accept, reject reason dialog",
    screen="lib/features/projects/presentation/screens/pm_approval_inbox_screen.dart",
    role="PM (TASK_COMPLETION_APPROVE)", priority="P1", type="Functional",
    pre="Two queued completions from site engineer (with photos).",
    steps=[
        "Open Approval Inbox.",
        "Tap thumbnail to expand (fullscreen).",
        "Approve first; Reject second with reason 'Plaster not finished'.",
    ],
    expected="Approve removes row, completes task. Reject opens "
             "RejectCompletionDialog; reason recorded; task reverts to "
             "IN_PROGRESS with rejectionReason.",
    flutter_gap="F-G13 — only 56 px thumbnail; no fullscreen preview; no "
                "bulk approve.",
))
add(dict(
    id="F-PM-APP-002",
    title="Approval inbox blocked for non-PM",
    screen="lib/features/projects/presentation/screens/pm_approval_inbox_screen.dart",
    role="SITE_ENGINEER", priority="P1", type="RBAC",
    pre="Site engineer (no approval perm).",
    steps=[
        "Navigate to /tasks/approval-inbox.",
    ],
    expected="Screen renders 'You are not authorised…' without fetching data.",
))
add(dict(
    id="F-PRJ-MEM-001",
    title="Project members — remove secondary signatory with confirm",
    screen="lib/features/projects/presentation/screens/project_members_screen.dart",
    role="PM", priority="P2", type="Functional",
    pre="Project with primary owner + 2 secondary.",
    steps=[
        "Tap delete on a secondary member; confirm.",
    ],
    expected="DELETE call; member removed; owner remains; customer portal "
             "access revoked.",
))
add(dict(
    id="F-CCTV-001",
    title="Add CCTV camera form requires name; validates port",
    screen="lib/screens/projects/cctv_camera_form_screen.dart",
    role="ADMIN", priority="P2", type="Validation",
    pre="Add Camera open.",
    steps=[
        "Save with empty name → expect inline error.",
        "Enter name; set port='8080a' → expect parse error.",
        "Fix port=8080; Save.",
    ],
    expected="First save blocked. Second blocked by int.tryParse null. Third "
             "succeeds.",
    flutter_gap="F-G09 — password field stored plaintext in form; no auth "
                "test or encryption pre-save.",
))
add(dict(
    id="F-CCTV-002",
    title="CCTV management — enable/disable toggle",
    screen="lib/screens/projects/cctv_management_screen.dart",
    role="ADMIN", priority="P3", type="Functional",
    pre="Camera 'Site-Gate' ACTIVE.",
    steps=[
        "Open camera; PopupMenu → Disable.",
    ],
    expected="PATCH /cctv-cameras/{id}/toggle; row color changes to grey.",
))
add(dict(
    id="F-SUB-001",
    title="Subcontract list — status filter chips",
    screen="lib/screens/projects/subcontracts_screen.dart",
    role="PM / Procurement", priority="P2", type="Functional",
    pre="Subcontracts in DRAFT/ACTIVE/COMPLETED.",
    steps=[
        "Open Subcontracts.",
        "Filter='Active'.",
    ],
    expected="Only active rows. Tapping a row shows snackbar 'View "
             "details…' — detail screen not implemented.",
    flutter_gap="F-G11 — detail screen stub; F-G38 — retention not shown "
                "per row.",
))
add(dict(
    id="F-360-001",
    title="View 360 player loads panorama + info sheet",
    screen="lib/screens/projects/view_360_player_screen.dart",
    role="any project member", priority="P3", type="Functional",
    pre="Project with uploaded 360 image.",
    steps=[
        "Open 360 list; tap a tour.",
    ],
    expected="Panorama widget; drag-to-explore hint visible 3 s then fades.",
    flutter_gap="F-G25 — no VR mode; pinch-zoom dependent on `panorama` "
                "package internal handling.",
))

# ============================== 7. TASKS LIST / CREATE / ALERTS ===========
add(dict(
    id="F-TLST-001",
    title="Task list status + priority multi-filter",
    screen="lib/screens/tasks/task_list_screen.dart",
    role="PM", priority="P2", type="Functional",
    pre="Mixed tasks seeded.",
    steps=[
        "Tap status='IN_PROGRESS' + priority='HIGH'.",
    ],
    expected="List intersect; pagination footer updates.",
))
add(dict(
    id="F-TCR-001",
    title="Task create requires dueDate",
    screen="lib/screens/tasks/task_create_screen.dart",
    role="PM", priority="P1", type="Validation",
    pre="Create form open.",
    steps=[
        "Fill title; leave dueDate empty.",
        "Submit.",
    ],
    expected="Form validation blocks. Pick a date; submit succeeds.",
))
add(dict(
    id="F-TCR-002",
    title="Date picker honours Indian DD/MM/YYYY",
    screen="lib/screens/tasks/task_create_screen.dart",
    role="any", priority="P3", type="Localisation",
    pre="Device locale en_IN.",
    steps=[
        "Open due date picker.",
    ],
    expected=(
        "Picker order is DD/MM/YYYY. Code uses `{day}/{month}/{year}` "
        "literal which works in India but not locale-aware."
    ),
    flutter_gap="F-G16 — use DateFormat('dd/MM/yyyy', 'en_IN').",
))
add(dict(
    id="F-TCR-003",
    title="Predecessor selection NOT available on create",
    screen="lib/screens/tasks/task_create_screen.dart",
    role="PM", priority="P2", type="Gap",
    pre="Create form.",
    steps=[
        "Scan available fields.",
    ],
    expected=(
        "Today: predecessors not editable on task create — must be added "
        "later via WBS template editor or Gantt. Worth surfacing on create."
    ),
    flutter_gap="F-G14 — predecessor editor missing on create screen.",
))
add(dict(
    id="F-TALR-001",
    title="Task alert dashboard — admin manual trigger",
    screen="lib/screens/tasks/task_alert_dashboard_screen.dart",
    role="ADMIN", priority="P3", type="Functional",
    pre="Admin logged in.",
    steps=[
        "Open Alerts.",
        "Tap 'Trigger Manual Check' icon.",
    ],
    expected="POST /tasks/alerts/trigger; recent alerts list refreshes.",
))
add(dict(
    id="F-TALR-002",
    title="Severity pie chart renders zero state",
    screen="lib/screens/tasks/task_alert_dashboard_screen.dart",
    role="any", priority="P3", type="UX",
    pre="No alerts.",
    steps=[
        "Open Alerts.",
    ],
    expected="Pie chart degraded to placeholder; stat cards show 0/0/0.",
))

# ============================== 8. WBS / SCHEDULING =======================
add(dict(
    id="F-WBS-001",
    title="WBS template editor — add phase, then task",
    screen="lib/features/scheduling/presentation/screens/wbs_template_editor_screen.dart",
    role="ADMIN (canManageWbsTemplates)", priority="P1", type="Functional",
    pre="Empty template.",
    steps=[
        "Add phase 'Foundation' via FAB.",
        "Add task 'Excavation', duration 5 days, weight=null, "
        "monsoonSensitive=true.",
        "Save.",
    ],
    expected="ReorderableListView on phase pane; task appears in DataTable; "
             "save returns new template version.",
    construction="Residential foundation phase.",
))
add(dict(
    id="F-WBS-002",
    title="Predecessor editor missing — only count shown",
    screen="lib/features/scheduling/presentation/screens/wbs_template_editor_screen.dart",
    role="ADMIN", priority="P2", type="Gap",
    pre="Task added.",
    steps=[
        "Try to edit predecessors from the task row.",
    ],
    expected="Today: 'Preds' column shows count only; no editor dialog.",
    flutter_gap="F-G14 — predecessor editor not implemented in screen.",
))
add(dict(
    id="F-WBS-003",
    title="Apply WBS template to a project",
    screen="lib/features/scheduling/presentation/screens/wbs_template_list_screen.dart",
    role="ADMIN / PM", priority="P1", type="Construction-Type",
    pre="Templates seeded per project type.",
    steps=[
        "Pick template 'Commercial-Standard-G+4'.",
        "Apply to a fresh commercial project.",
    ],
    expected="Tasks + predecessors cloned. Dates shifted from project start. "
             "Gantt populated.",
    construction="Commercial G+4.",
))
add(dict(
    id="F-HOL-001",
    title="Holiday calendar — scope dropdown required",
    screen="lib/features/scheduling/presentation/screens/holiday_calendar_screen.dart",
    role="ADMIN", priority="P2", type="Functional",
    pre="Calendar open.",
    steps=[
        "Try to create a holiday without selecting scope.",
    ],
    expected="Backend rejects (scope required). UI surfaces error toast.",
))
add(dict(
    id="F-HOL-002",
    title="State-level holiday overrides global on project Gantt",
    screen="lib/features/scheduling/presentation/screens/holiday_calendar_screen.dart",
    role="ADMIN", priority="P3", type="Construction-Domain",
    pre="Project in Karnataka; 'Kannada Rajyotsava' on Nov 1 state-scope.",
    steps=[
        "Schedule a task across Nov 1.",
        "Open Gantt.",
    ],
    expected="Nov 1 marked non-working; bar reflows skipping Nov 1.",
    flutter_gap="F-G15 — no project-specific override field; cannot disable a "
                "state holiday for one project.",
))

# ============================== 9. SITE REPORTS ===========================
add(dict(
    id="F-SR-001",
    title="Site report — at-least-one-photo enforced",
    screen="lib/screens/reports/add_site_report_screen.dart",
    role="SITE_ENGINEER", priority="P1", type="Validation",
    pre="Add Site Report open.",
    steps=[
        "Fill title + description; no photos.",
        "Submit.",
    ],
    expected="Toast 'At least one photo required'. Form does not submit.",
))
add(dict(
    id="F-SR-002",
    title="Photo capture preserves geotag + accuracy",
    screen="lib/features/site_reports/presentation/screens/site_reports_screen.dart",
    role="SITE_ENGINEER", priority="P1", type="Construction-Domain",
    pre="On-site; GPS enabled.",
    steps=[
        "Take 3 photos via camera; observe lat/lng/accuracy chip.",
    ],
    expected="ImagePicker captures with imageQuality=70; lat/lng/accuracy "
             "attached to each PhotoCapture; distanceFromProject computed.",
))
add(dict(
    id="F-SR-003",
    title="Offline submit queues to outbox with idempotency key",
    screen="lib/features/site_reports/presentation/screens/site_reports_screen.dart",
    role="SITE_ENGINEER (mobile)", priority="P1", type="Offline",
    pre="Mobile in airplane mode.",
    steps=[
        "Fill report; submit.",
    ],
    expected="OutboxService.enqueue stores SITE_REPORT_CREATE row with "
             "photos. Toast: 'Queued — will upload when online.'",
))
add(dict(
    id="F-SR-004",
    title="Web variant has no offline queue",
    screen="lib/features/site_reports/presentation/screens/site_reports_screen.dart",
    role="SITE_ENGINEER (web)", priority="P1", type="Offline Gap",
    pre="Browser; airplane mode (DevTools network=offline).",
    steps=[
        "Submit report.",
    ],
    expected="createReportDirect throws; toast: 'Network error — please try "
             "again'. No queue.",
    flutter_gap="F-G30 — web cannot use OutboxService (drift_flutter "
                "unavailable).",
))
add(dict(
    id="F-SR-005",
    title="Sync of queued report on reconnect",
    screen="lib/screens/sync/pending_sync_screen.dart + SyncService",
    role="SITE_ENGINEER", priority="P1", type="Offline",
    pre="2 queued reports, network restored.",
    steps=[
        "Open Pending Sync screen.",
        "Tap 'Sync Now'.",
    ],
    expected="Both rows drain to 'done'; toast 'All synced'. PendingSync "
             "screen empty.",
))
add(dict(
    id="F-SR-006",
    title="Permanent failure surfaces in Issues tab",
    screen="lib/screens/sync/pending_sync_screen.dart",
    role="SITE_ENGINEER", priority="P2", type="Resilience",
    pre="Photo blob corrupted (5 failed attempts).",
    steps=[
        "Open Issues tab.",
    ],
    expected="Row with error message + Retry + Discard buttons.",
    flutter_gap="F-G31 — no bulk discard; one-by-one only.",
))
add(dict(
    id="F-SR-007",
    title="Cross-project lazy summary on empty list",
    screen="lib/features/site_reports/presentation/screens/site_reports_screen.dart",
    role="SITE_ENGINEER", priority="P3", type="UX",
    pre="No reports for selected project; user has submitted to another "
        "project today.",
    steps=[
        "Open Site Reports for empty project.",
    ],
    expected="Empty card + footer hint 'You submitted 2 reports today to "
             "<Project Y> — wrong project?'",
))
add(dict(
    id="F-SR-008",
    title="Multi-photo upload caps individual size",
    screen="lib/features/site_reports/presentation/screens/site_reports_screen.dart",
    role="SITE_ENGINEER", priority="P2", type="Validation",
    pre="Mobile.",
    steps=[
        "Pick 12 photos including one 40 MB raw camera shot.",
    ],
    expected="imageQuality=70 compresses; per-file size limit (e.g., 10 MB) "
             "enforced before queueing.",
    flutter_gap="F-G34 — no explicit file size cap surfaced in UI.",
))
add(dict(
    id="F-SR-009",
    title="GPS spoofing detection",
    screen="lib/features/site_reports/...",
    role="SITE_ENGINEER", priority="P1", type="Security Gap",
    pre="User runs mock-location app.",
    steps=[
        "Set device to mock coordinates near project.",
        "Submit report.",
    ],
    expected=(
        "Today: not detected. Goal: check Position.isMocked (mobile "
        "geolocator) and flag for review."
    ),
    flutter_gap="F-G39 — no mock-location detection.",
))
add(dict(
    id="F-SR-010",
    title="Photo carousel shows 4 thumbs + '+N' count",
    screen="lib/features/site_reports/presentation/screens/site_reports_screen.dart",
    role="any", priority="P3", type="UI",
    pre="Report with 7 photos.",
    steps=[
        "Open list.",
    ],
    expected="Card shows 4 thumbnails and '+3' badge.",
))
add(dict(
    id="F-SR-011",
    title="Approve report fans webhook + push",
    screen="Site report detail",
    role="PM", priority="P2", type="Integration",
    pre="Report SUBMITTED.",
    steps=[
        "Approve.",
    ],
    expected="Status APPROVED; ActivityFeedService logs; FCM push to customer "
             "(if subscribed).",
))
add(dict(
    id="F-SR-012",
    title="Background sync respects exponential backoff",
    screen="lib/services/sync_service.dart",
    role="SITE_ENGINEER", priority="P2", type="Resilience",
    pre="3 transient API failures.",
    steps=[
        "Cause server 500 for 3 attempts.",
    ],
    expected="Backoff: 2 → 4 → 8 → 16 → 30 min jittered; max 5 attempts "
             "before permanent failure.",
))

# ============================== 10. SITE VISITS ===========================
add(dict(
    id="F-SV-001",
    title="Check-in dialog requires project + GPS + visit type",
    screen="lib/screens/site_visits/site_visit_check_in_dialog.dart",
    role="PM / Site Engineer", priority="P1", type="Validation",
    pre="At project gate.",
    steps=[
        "Tap '+ Check In' on Site Visits.",
        "Skip GPS capture; tap Submit.",
    ],
    expected="Validation: 'Capture location' required. Set GPS; submit "
             "succeeds; status PENDING → CHECKED_IN.",
))
add(dict(
    id="F-SV-002",
    title="Active visit banner appears across screens",
    screen="lib/screens/site_visits/site_visits_screen.dart",
    role="PM", priority="P2", type="UX",
    pre="Active CHECKED_IN visit.",
    steps=[
        "Navigate to dashboard, leads, projects.",
    ],
    expected="Banner sticky at top of Site Visits screen; but doesn't appear "
             "on unrelated screens (could be a Provider-driven global banner).",
    flutter_gap="F-G17 — visit banner not globally surfaced.",
))
add(dict(
    id="F-SV-003",
    title="Check-out distance from project — high value flag",
    screen="lib/screens/site_visits/site_visit_check_out_dialog.dart",
    role="PM", priority="P2", type="Construction-Domain",
    pre="Active visit; check-out from 5km away.",
    steps=[
        "Tap Check Out; capture GPS at far location.",
    ],
    expected="Server records distanceFromProjectCheckOut; UI shows '5.0 km "
             "from site' chip in red.",
))
add(dict(
    id="F-SV-004",
    title="Visit duration computed client-side and persisted",
    screen="lib/screens/site_visits/site_visit_check_out_dialog.dart",
    role="any", priority="P3", type="Functional",
    pre="Visit checked-in 90 min ago.",
    steps=[
        "Open Check-Out dialog.",
    ],
    expected="Header shows '1h 30m'; durationMinutes=90 sent in payload.",
))
add(dict(
    id="F-SV-005",
    title="Visit detail screen shows full GPS lat/lng",
    screen="lib/screens/site_visits/site_visit_detail_screen.dart",
    role="any", priority="P2", type="UI",
    pre="Completed visit.",
    steps=[
        "Open detail.",
    ],
    expected="Two timeline cards (check-in / check-out) with 6-decimal "
             "coords in monospace.",
))
add(dict(
    id="F-SV-006",
    title="GPS spoofing for fake on-site visit",
    screen="lib/screens/site_visits/...",
    role="SITE_ENGINEER", priority="P1", type="Security Gap",
    pre="Mock location app.",
    steps=[
        "Check in from 100 km away with mocked coords near project.",
    ],
    expected="Today: not detected. Goal: flag visit, alert PM.",
    flutter_gap="F-G39 — no mock-location check.",
))

# ============================== 11. OBSERVATIONS / QUALITY / GALLERY ======
add(dict(
    id="F-OBS-001",
    title="Observations tabs — Active vs Resolved",
    screen="lib/features/observations/presentation/screens/observations_screen.dart",
    role="PM", priority="P2", type="Functional",
    pre="3 active, 2 resolved observations.",
    steps=[
        "Switch tabs.",
    ],
    expected="Active tab lists 3; Resolved lists 2. PDF export from app bar.",
))
add(dict(
    id="F-OBS-002",
    title="Critical observation blocks task completion (cross-screen)",
    screen="lib/features/observations/... + task progress screen",
    role="SITE_ENGINEER", priority="P2", type="Integration",
    pre="Critical OPEN observation on Task T.",
    steps=[
        "Try to Mark Complete T.",
    ],
    expected="Backend rejects; UI shows 'Critical observation #N must be "
             "resolved first'. Action link to observation.",
    flutter_gap="F-G18 — confirm cross-screen link works.",
))
add(dict(
    id="F-QC-001",
    title="Quality check FAIL auto-creates observation",
    screen="lib/features/quality/presentation/screens/quality_checks_screen.dart",
    role="QS", priority="P2", type="Integration",
    pre="QC checklist for slab pour.",
    steps=[
        "Record check with result=FAIL.",
    ],
    expected="Backend creates linked observation; UI navigates to it or "
             "shows toast 'Observation #N created'.",
))
add(dict(
    id="F-GAL-001",
    title="Gallery grouped by date; auth-aware image loading",
    screen="lib/features/gallery/presentation/screens/gallery_screen.dart",
    role="any", priority="P3", type="Functional",
    pre="Project with 50 gallery images across 5 days.",
    steps=[
        "Open Gallery; toggle grid ↔ list view.",
    ],
    expected="Date group headers; images load via authenticated requests "
             "(Authorization header). Pull-to-refresh refreshes.",
))
add(dict(
    id="F-DEL-001",
    title="Log delay with customer-visible flag",
    screen="lib/features/delays/presentation/screens/delay_logs_screen.dart",
    role="PM", priority="P2", type="Functional",
    pre="Project in CONSTRUCTION phase.",
    steps=[
        "Tap 'Log Delay'.",
        "Category=WEATHER, fromDate=today, durationDays=3.",
        "Toggle customerVisible=true; leave customer summary blank.",
        "Submit.",
    ],
    expected="Validation: customer summary required when visible flag on. "
             "Fill summary; submit. Outbox-queued on mobile.",
    construction="Monsoon weather delay scenario.",
))
add(dict(
    id="F-DEL-002",
    title="Close delay with toDate after fromDate",
    screen="lib/features/delays/presentation/screens/delay_logs_screen.dart",
    role="PM", priority="P2", type="Validation",
    pre="Open delay starting 5 days ago.",
    steps=[
        "Tap Close; pick toDate=yesterday.",
    ],
    expected="Date picker prevents toDate < fromDate. Submit succeeds; "
             "delay row shows duration.",
))

# ============================== 12. LABOUR ================================
add(dict(
    id="F-LAB-001",
    title="Bulk attendance — 50 labourers in 30 s",
    screen="lib/screens/labour/attendance_screen.dart",
    role="SITE_SUPERVISOR", priority="P1", type="Functional",
    pre="50 labour records active on project.",
    steps=[
        "Select project + today.",
        "Mark 45 PRESENT, 3 HALF_DAY, 2 ABSENT via chips.",
        "Submit.",
    ],
    expected="50 attendance rows persisted; success toast.",
    flutter_gap="F-G19 — no RFID/Aadhaar capture; chip taps only.",
))
add(dict(
    id="F-LAB-002",
    title="Duplicate attendance entry handled",
    screen="lib/screens/labour/attendance_screen.dart",
    role="SITE_SUPERVISOR", priority="P2", type="Negative",
    pre="Attendance for today already exists.",
    steps=[
        "Try to re-submit.",
    ],
    expected="Backend 409; UI shows 'Already recorded today'.",
))
add(dict(
    id="F-LAB-003",
    title="Add Labour — wage decimal + ID proof type",
    screen="lib/screens/labour/add_labour_screen.dart",
    role="HR / SUPERVISOR", priority="P2", type="Validation",
    pre="Form open.",
    steps=[
        "Fill name, phone, trade=MASON, dailyWage=850.50, idProofType=AADHAAR, "
        "idProofNumber='1234 5678 9012'.",
        "Save.",
    ],
    expected="Stored; Aadhaar masked in UI to '**** **** 9012'.",
    flutter_gap="F-G29 — no client-side Aadhaar verification/masking before "
                "upload (relies on backend).",
))
add(dict(
    id="F-LAB-004",
    title="MB entry auto-calculates qty from L×B×D",
    screen="lib/screens/labour/mb_entry_screen.dart",
    role="SUPERVISOR", priority="P2", type="Functional",
    pre="MB form open.",
    steps=[
        "Enter L=4, B=2, D=0 (footing).",
        "Observe Quantity field.",
    ],
    expected="Qty = 4 × 2 × max(0,1) = 8 SFT. (Code multiplies 0 → 1 to "
             "avoid zeroing.)",
    construction="Foundation footing measurement.",
))
add(dict(
    id="F-LAB-005",
    title="MB list filters by project",
    screen="lib/screens/labour/mb_list_screen.dart",
    role="SUPERVISOR", priority="P3", type="Functional",
    pre="MB entries across 2 projects.",
    steps=[
        "Change project dropdown.",
    ],
    expected="List reloads; totals update.",
))
add(dict(
    id="F-LAB-006",
    title="Wage sheet generation respects date range",
    screen="lib/features/labour/presentation/screens/wage_sheet_screen.dart",
    role="HR / Finance", priority="P1", type="Functional",
    pre="Attendance for 26 working days.",
    steps=[
        "Pick start/end dates; tap Generate.",
    ],
    expected="WageSheet DRAFT; one row per labourer; daysWorked, wage, "
             "advances, net payable.",
    flutter_gap="F-G18 — no PF/ESI deductions visible (gap from backend).",
))

# ============================== 13. INVENTORY =============================
add(dict(
    id="F-INV-001",
    title="Materials list — add with category + unit",
    screen="lib/screens/inventory/materials_screen.dart",
    role="ADMIN", priority="P2", type="Functional",
    pre="Materials screen open.",
    steps=[
        "Add 'TMT Steel Fe500', category=METAL, unit=KG.",
    ],
    expected="Persists; new row visible.",
    flutter_gap="F-G08 — no HSN field on material form.",
))
add(dict(
    id="F-INV-002",
    title="Stock adjustment requires reason",
    screen="lib/screens/inventory/add_stock_adjustment_screen.dart",
    role="STORE_KEEPER", priority="P2", type="Validation",
    pre="Stock for Cement = 100 bags.",
    steps=[
        "Adjust -10 with reason blank; Save.",
    ],
    expected="Form validation: reason required.",
    flutter_gap="F-G18 — no approver workflow gate.",
))
add(dict(
    id="F-INV-003",
    title="Material consumption report rows",
    screen="lib/screens/inventory/material_consumption_screen.dart",
    role="STORE_KEEPER", priority="P3", type="Functional",
    pre="Material with inward 200, current 50, wastage 5.",
    steps=[
        "Open consumption report.",
    ],
    expected="Row: 200 / 50 / 5 across columns.",
))
add(dict(
    id="F-INV-004",
    title="Stock report empty state",
    screen="lib/screens/inventory/stock_report_screen.dart",
    role="any", priority="P3", type="UX",
    pre="No data.",
    steps=[
        "Open report.",
    ],
    expected="Centered icon + 'No stock data' message.",
))

# ============================== 14. PROCUREMENT ===========================
add(dict(
    id="F-PRC-001",
    title="Add vendor — GSTIN length",
    screen="lib/screens/procurement/add_vendor_screen.dart",
    role="PROCUREMENT", priority="P1", type="Validation / India",
    pre="Form open.",
    steps=[
        "Type GSTIN '29ABC' (5 chars); Save.",
    ],
    expected="Validator: 'GSTIN must be 15 chars'.",
    flutter_gap="F-G06 — no checksum / live GSTN validation.",
))
add(dict(
    id="F-PRC-002",
    title="PO list status filter + pagination",
    screen="lib/screens/procurement/po_list_screen.dart",
    role="PROCUREMENT", priority="P2", type="Functional",
    pre="POs in DRAFT/ISSUED/RECEIVED.",
    steps=[
        "Filter='ISSUED'.",
    ],
    expected="Only issued POs; pagination updates.",
))
add(dict(
    id="F-PRC-003",
    title="Add PO — Indian numbering on total",
    screen="lib/screens/procurement/add_purchase_order_screen.dart",
    role="PROCUREMENT", priority="P3", type="Localisation",
    pre="Form open.",
    steps=[
        "Enter line items totalling ₹12,34,56,789.",
    ],
    expected="Total formatted as Indian grouping (lakh/crore). Today: raw "
             "number.",
    flutter_gap="F-G28 — no Indian numbering helper.",
))
add(dict(
    id="F-PRC-004",
    title="Quotation management — select winner; others auto-reject",
    screen="lib/features/procurement/presentation/screens/quotation_management_screen.dart",
    role="PROCUREMENT", priority="P1", type="Functional",
    pre="3 quotations on one indent.",
    steps=[
        "Tap 'Select' on Q2.",
    ],
    expected="Q2 SELECTED; Q1, Q3 REJECTED; selectedAt stored.",
))
add(dict(
    id="F-PRC-005",
    title="Vendor compliance — phone uniqueness",
    screen="lib/screens/procurement/add_vendor_screen.dart",
    role="PROCUREMENT", priority="P2", type="Negative",
    pre="Vendor V1 has phone 9999.",
    steps=[
        "Create V2 with same phone.",
    ],
    expected="Backend 409; UI surfaces 'Vendor with this phone exists'.",
))

# ============================== 15. BOQ ===================================
add(dict(
    id="F-BOQ-001",
    title="BOQ list — currency formatting & summary card",
    screen="lib/features/boq/presentation/screens/boq_screen.dart",
    role="PM / Estimator", priority="P2", type="Functional",
    pre="BOQ items present.",
    steps=[
        "Open BOQ.",
    ],
    expected="Items grouped by category; totals shown as ₹ with 2 decimals; "
             "summary card toggleable.",
    flutter_gap="F-G37 — no GST split / HSN visible per row.",
))
add(dict(
    id="F-BOQ-002",
    title="BOQ doc — submit → approve → state transitions",
    screen="lib/features/boq/presentation/screens/boq_document_management_screen.dart",
    role="PM (BOQ_APPROVE)", priority="P1", type="Functional",
    pre="BoQ DRAFT with items.",
    steps=[
        "Tap Submit; observe status PENDING_APPROVAL.",
        "Tap Internal Approve.",
    ],
    expected=(
        "Status APPROVED; timestamps row updates; further item edits "
        "blocked. No confirmation dialog today (gap)."
    ),
    flutter_gap="F-G21 — no confirm dialog on irreversible approve.",
))
add(dict(
    id="F-BOQ-003",
    title="BOQ reject requires reason",
    screen="lib/features/boq/presentation/screens/boq_document_management_screen.dart",
    role="PM", priority="P2", type="Validation",
    pre="BoQ in PENDING_APPROVAL.",
    steps=[
        "Tap Reject; submit with blank reason.",
    ],
    expected="Dialog validation refuses; rejection reason required.",
))
add(dict(
    id="F-BOQ-004",
    title="BOQ invoice — confirm payment requires reference",
    screen="lib/features/boq/presentation/screens/boq_invoice_screen.dart",
    role="FINANCE", priority="P1", type="Validation",
    pre="Invoice SENT.",
    steps=[
        "Tap 'Confirm Payment Received'; leave ref blank.",
    ],
    expected="Dialog: payment reference required.",
))
add(dict(
    id="F-BOQ-005",
    title="Change order — addition triggers advance invoice",
    screen="lib/features/boq/presentation/screens/co_management_screen.dart",
    role="PM (VO_APPROVE)", priority="P1", type="Functional",
    pre="CO addition ₹2,50,000 approved by customer.",
    steps=[
        "Tap 'Start Work'.",
    ],
    expected="Status IN_PROGRESS; advance invoice auto-generated; visible "
             "in Invoices screen.",
))
add(dict(
    id="F-BOQ-006",
    title="Payment schedule — raise invoice on DUE stage",
    screen="lib/features/boq/presentation/screens/payment_schedule_screen.dart",
    role="FINANCE", priority="P1", type="Functional",
    pre="Stage 2 in DUE.",
    steps=[
        "Tap 'Raise Invoice'; pick today as invoice date.",
    ],
    expected="Tax invoice created with stored GST rate; status moves to "
             "INVOICED.",
    flutter_gap="F-G22 — no idempotency token on tap (network retry could "
                "duplicate).",
))
add(dict(
    id="F-BOQ-007",
    title="Finance summary card reconciles after CO",
    screen="lib/features/boq/presentation/screens/payment_schedule_screen.dart",
    role="FINANCE", priority="P2", type="Integration",
    pre="Project with 1 addition CO ₹1.2L approved.",
    steps=[
        "Open Payment Schedule.",
    ],
    expected="Net project value = original + addition; collected/outstanding "
             "computed from invoices + receipts.",
))
add(dict(
    id="F-BOQ-008",
    title="CGST/SGST/IGST split UI missing",
    screen="lib/features/boq/...",
    role="FINANCE", priority="P1", type="India Compliance Gap",
    pre="Intra- vs inter-state invoice.",
    steps=[
        "Open invoice screen for KA→KA project.",
    ],
    expected=(
        "Should show CGST 9% + SGST 9% split. Today: only single 'GST 18%' "
        "line. Filing GSTR-1 with this data impossible without backend "
        "splitting."
    ),
    flutter_gap="F-G06 — no CGST/SGST/IGST split anywhere in UI.",
))

# ============================== 16. DPC ===================================
add(dict(
    id="F-DPC-001",
    title="DPC builder requires approved BOQ",
    screen="lib/features/dpc/presentation/screens/dpc_builder_screen.dart",
    role="PM / Estimator", priority="P1", type="Functional",
    pre="Project with DRAFT BOQ.",
    steps=[
        "Open DPC builder.",
    ],
    expected="Empty state 'No approved BoQ — approve the BOQ first' with "
             "link to BOQ doc screen.",
))
add(dict(
    id="F-DPC-002",
    title="Split-screen PDF preview on wide screens",
    screen="lib/features/dpc/presentation/screens/dpc_builder_screen.dart",
    role="any", priority="P2", type="Responsive",
    pre="Approved BOQ; window > 1200px.",
    steps=[
        "Edit a scope rationale.",
    ],
    expected="Right pane re-renders PDF preview after debounce. Below "
             "1200px preview stacks below content.",
))
add(dict(
    id="F-DPC-003",
    title="Issue DPC freezes document",
    screen="lib/features/dpc/presentation/screens/dpc_builder_screen.dart",
    role="ADMIN", priority="P1", type="Functional",
    pre="DRAFT DPC.",
    steps=[
        "Edit content; tap 'Issue'.",
    ],
    expected="Status DRAFT → ISSUED; PDF persisted; further edits blocked "
             "until 'Revise'.",
    flutter_gap="F-G21 — no confirm dialog before irreversible Issue.",
))
add(dict(
    id="F-DPC-004",
    title="Revisions screen — download prior PDF",
    screen="lib/features/dpc/presentation/screens/dpc_revisions_screen.dart",
    role="any", priority="P2", type="Functional",
    pre="2 revisions issued.",
    steps=[
        "Tap 'Download' on revision 1.",
    ],
    expected="PDF fetched and saved/opened.",
    flutter_gap="F-G24 — no in-app PDF preview before download.",
))
add(dict(
    id="F-DPC-005",
    title="Customization catalog admin — search debounce",
    screen="lib/features/dpc_customization_catalog/presentation/screens/dpc_customization_catalog_admin_screen.dart",
    role="ADMIN", priority="P3", type="Performance",
    pre="500 catalog items.",
    steps=[
        "Type 'kitchen' rapidly.",
    ],
    expected="Debounced search; page resets to 0; results filter.",
))
add(dict(
    id="F-DPC-006",
    title="Catalog rate override on DPC line — audit gap",
    screen="lib/features/dpc/presentation/screens/dpc_builder_screen.dart",
    role="ADMIN", priority="P2", type="Compliance Gap",
    pre="Catalog item rate ₹1200.",
    steps=[
        "Add customisation line; override unit rate to ₹600.",
        "Save.",
    ],
    expected="Today: accepted silently. Goal: audit log entry + reviewer "
             "approval required.",
    flutter_gap="F-G18 — no override audit; ties to backend G-09.",
))

# ============================== 17. ESTIMATION SETTINGS ===================
add(dict(
    id="F-ESET-001",
    title="Packages list — show/hide inactive",
    screen="lib/features/estimation_settings/presentation/screens/packages_list_screen.dart",
    role="ADMIN", priority="P3", type="Functional",
    pre="Mixed active/inactive packages.",
    steps=[
        "Toggle 'Show inactive'.",
    ],
    expected="List re-fetches; inactive shown.",
))
add(dict(
    id="F-ESET-002",
    title="Market index — publish new snapshot",
    screen="lib/features/estimation_settings/presentation/screens/market_index_screen.dart",
    role="ADMIN (canPublishMarketIndex)", priority="P2", type="Functional",
    pre="Last published 6 months ago.",
    steps=[
        "Tap 'New Snapshot'.",
        "Fill rates for steel, cement, etc.",
        "Submit.",
    ],
    expected="Composite index computed; row inserted; Active chip on it.",
))
add(dict(
    id="F-ESET-003",
    title="Rate card version creation",
    screen="lib/features/estimation_settings/presentation/screens/rate_card_screen.dart",
    role="ADMIN", priority="P3", type="Functional",
    pre="Package + project type selected.",
    steps=[
        "Tap 'New Version'.",
        "Submit material/labour/overhead rates with valid dates.",
    ],
    expected="Version appears with Active chip if effective-today.",
))

# ============================== 18. FINANCE / PAYMENTS ====================
add(dict(
    id="F-FIN-001",
    title="Finance dashboard tabs",
    screen="lib/screens/finance/finance_dashboard_screen.dart",
    role="FINANCE", priority="P2", type="Functional",
    pre="Mixed invoices, bills, labour payments.",
    steps=[
        "Switch tabs.",
    ],
    expected="Each tab loads independently; counts visible.",
    flutter_gap="F-G37 — no summary totals; no status filtering visible.",
))
add(dict(
    id="F-FIN-002",
    title="Add project invoice — line items with GST",
    screen="lib/screens/finance/add_project_invoice_screen.dart",
    role="FINANCE", priority="P1", type="India Compliance",
    pre="Form open.",
    steps=[
        "Add lines with taxable values; choose GST 18%.",
    ],
    expected="Total + GST + grand total computed. Should split CGST+SGST or "
             "IGST based on customer state — today not done.",
    flutter_gap="F-G06 — no CGST/SGST/IGST split.",
))
add(dict(
    id="F-PAY-001",
    title="Payments dashboard — pending vs all chips",
    screen="lib/screens/payments/payments_dashboard_screen.dart",
    role="FINANCE", priority="P2", type="Functional",
    pre="Mixed pending + paid.",
    steps=[
        "Tap 'Pending'.",
    ],
    expected="List filters to non-PAID; pagination resets.",
))
add(dict(
    id="F-PAY-002",
    title="Challan management — auto numbering per FY",
    screen="lib/screens/payments/challan_management_screen.dart",
    role="FINANCE", priority="P1", type="India Compliance",
    pre="3 challans created today (FY 2026-27).",
    steps=[
        "Create one more.",
    ],
    expected="Number is WAL/CH/2026-27/00004; backend pessimistic lock.",
))
add(dict(
    id="F-PAY-003",
    title="Payment history — date range filter",
    screen="lib/screens/payments/payment_history_screen.dart",
    role="FINANCE", priority="P2", type="Functional",
    pre="Year of receipts.",
    steps=[
        "Pick Apr 1–Sep 30.",
    ],
    expected="Receipts filtered; total at footer.",
))

# ============================== 19. STAGE PAYMENTS / DEDUCTIONS / FA ======
add(dict(
    id="F-STG-001",
    title="Stage timeline status pills",
    screen="lib/features/stage_payments/presentation/screens/stage_payment_screen.dart",
    role="PM / FINANCE", priority="P2", type="UI",
    pre="Stages UPCOMING/DUE/INVOICED/PAID/OVERDUE.",
    steps=[
        "Open timeline.",
    ],
    expected="Pills colour-coded grey/orange/blue/green/red.",
))
add(dict(
    id="F-STG-002",
    title="Certify stage requires 'certified by'",
    screen="lib/features/stage_payments/presentation/screens/stage_payment_screen.dart",
    role="PM / DIRECTOR", priority="P1", type="Validation",
    pre="Stage UPCOMING.",
    steps=[
        "Tap Certify; leave certified-by blank.",
    ],
    expected="Dialog validation requires field. Provide name; default "
             "retention 5%; submit.",
    flutter_gap="F-G21 — no confirmation dialog before final certification.",
))
add(dict(
    id="F-STG-003",
    title="Certify stage idempotency on rapid double-tap",
    screen="lib/features/stage_payments/presentation/screens/stage_payment_screen.dart",
    role="PM", priority="P1", type="Race",
    pre="Stage DUE.",
    steps=[
        "Tap Certify twice rapidly.",
    ],
    expected="Today: backend may create duplicate event. Goal: button "
             "disable + idempotency key.",
    flutter_gap="F-G22 — no idempotency token; ties to backend G-18.",
))
add(dict(
    id="F-DED-001",
    title="Deduction PARTIALLY_ACCEPTABLE requires amount <= requested",
    screen="lib/features/deductions/presentation/screens/deduction_register_screen.dart",
    role="DIRECTOR", priority="P1", type="Validation",
    pre="Deduction requested 60k.",
    steps=[
        "Decide PARTIAL with acceptedAmount=70k.",
    ],
    expected="UI rejects; show error message.",
))
add(dict(
    id="F-DED-002",
    title="Decision dialog requires 'approved by'",
    screen="lib/features/deductions/presentation/screens/deduction_register_screen.dart",
    role="DIRECTOR", priority="P2", type="Validation",
    pre="Pending deduction.",
    steps=[
        "Decide ACCEPT; submit without approver name.",
    ],
    expected="Dialog validation blocks.",
))
add(dict(
    id="F-FA-001",
    title="Final account create requires base + preparedBy",
    screen="lib/features/final_account/presentation/screens/final_account_screen.dart",
    role="COMMERCIAL_MANAGER", priority="P1", type="Validation",
    pre="No FA yet.",
    steps=[
        "Tap 'Create'.",
        "Leave base contract value blank; submit.",
    ],
    expected="Validation refuses. Fill values + preparedBy; submit; status "
             "DRAFT.",
))
add(dict(
    id="F-FA-002",
    title="Transition to AGREED requires agreedBy",
    screen="lib/features/final_account/presentation/screens/final_account_screen.dart",
    role="DIRECTOR", priority="P1", type="Validation",
    pre="FA SUBMITTED.",
    steps=[
        "Tap 'Agree'; submit blank.",
    ],
    expected="Dialog requires agreedBy. Submit succeeds; status AGREED; DLP "
             "starts.",
    flutter_gap="F-G21 — no confirmation that AGREED is essentially "
                "irreversible.",
))
add(dict(
    id="F-FA-003",
    title="Optimistic-lock conflict surfaces 409 in UI",
    screen="lib/features/final_account/presentation/screens/final_account_screen.dart",
    role="FINANCE", priority="P2", type="Concurrency",
    pre="Two users editing same FA.",
    steps=[
        "Both edit + save.",
    ],
    expected="Second save receives 409 (backend G-19 fix); UI shows 'This "
             "final account was modified by another user. Please refresh and "
             "reapply your changes.'",
    flutter_gap="F-G23 — UI must consume the 409 and offer refetch.",
))

# ============================== 20. VARIATION ORDERS ======================
add(dict(
    id="F-VO-001",
    title="VO list — filter by status; ± amount colour",
    screen="lib/features/variation_orders/presentation/screens/vo_list_screen.dart",
    role="PM", priority="P2", type="UI",
    pre="Mixed COs.",
    steps=[
        "Filter='APPROVED'.",
    ],
    expected="Only approved; addition amounts green, reductions orange.",
))
add(dict(
    id="F-VO-002",
    title="VO create — required fields and GST not auto",
    screen="lib/features/variation_orders/presentation/screens/vo_create_screen.dart",
    role="PM", priority="P2", type="Validation / Gap",
    pre="Form open.",
    steps=[
        "Fill title; submit.",
    ],
    expected="Required: title, type, category, net amount, description, "
             "justification. GST is NOT auto-calculated from net amount.",
    flutter_gap="F-G18 — no GST auto-calc; user must compute mentally.",
))
add(dict(
    id="F-VO-003",
    title="VO detail — approve/reject tabs flow",
    screen="lib/features/variation_orders/presentation/screens/vo_detail_screen.dart",
    role="DIRECTOR", priority="P1", type="Functional",
    pre="VO SUBMITTED.",
    steps=[
        "Tap Approve; comment 'OK to proceed'.",
    ],
    expected="Backend transitions to INTERNALLY_APPROVED; UI refreshes.",
))
add(dict(
    id="F-VO-004",
    title="VO PDF generation",
    screen="lib/features/variation_orders/presentation/screens/vo_detail_screen.dart",
    role="any", priority="P3", type="Gap",
    pre="VO in any status.",
    steps=[
        "Look for PDF / Share buttons.",
    ],
    expected="Today: no PDF generation visible. Customer must print "
             "screenshot.",
    flutter_gap="F-G24 — VO PDF export missing.",
))

# ============================== 21. NOTIFICATIONS =========================
add(dict(
    id="F-NOTI-001",
    title="Mark all read clears badge",
    screen="lib/screens/notifications/portal_notification_screen.dart",
    role="any", priority="P2", type="Functional",
    pre="10 unread.",
    steps=[
        "Open notifications.",
        "Tap 'Mark all read'.",
    ],
    expected="Badge clears; PUT /read-all called; list opacity changes.",
))
add(dict(
    id="F-NOTI-002",
    title="Pull-to-refresh and infinite scroll",
    screen="lib/screens/notifications/portal_notification_screen.dart",
    role="any", priority="P3", type="Functional",
    pre="100 notifications.",
    steps=[
        "Pull down; scroll to bottom.",
    ],
    expected="Refresh fetches latest; _hasMore loads next page.",
    flutter_gap="F-G31 — no type filter; no delete/archive.",
))

# ============================== 22. SUPPORT / FEEDBACK ====================
add(dict(
    id="F-SUP-001",
    title="Ticket list status + category filters",
    screen="lib/screens/support/support_tickets_screen.dart",
    role="SUPPORT_AGENT", priority="P2", type="Functional",
    pre="Mixed tickets.",
    steps=[
        "Filter='OPEN', category='BILLING'.",
    ],
    expected="List filtered; pagination updates.",
    flutter_gap="F-G32 — no SLA badge in list.",
))
add(dict(
    id="F-SUP-002",
    title="Ticket detail — assignee dropdown + reply",
    screen="lib/screens/support/support_ticket_detail_screen.dart",
    role="SUPPORT_AGENT", priority="P2", type="Functional",
    pre="Ticket OPEN.",
    steps=[
        "Pick assignee.",
        "Type reply; Send.",
    ],
    expected="Reply appears in thread; assignee updated.",
    flutter_gap="F-G32 — no attachment in reply UI.",
))
add(dict(
    id="F-FB-001",
    title="Feedback forms tab vs responses",
    screen="lib/features/feedback/presentation/screens/feedback_screen.dart",
    role="PM", priority="P3", type="Functional",
    pre="Project with form + responses.",
    steps=[
        "Switch tabs.",
    ],
    expected="Forms tab shows form summaries; Responses tab shows submitted "
             "data.",
))

# ============================== 23. PARTNERSHIPS ==========================
add(dict(
    id="F-PRT-001",
    title="Partner admin list — tab + type filter + search",
    screen="lib/features/partnerships/presentation/screens/partnerships_admin_screen.dart",
    role="ADMIN", priority="P2", type="Functional",
    pre="Partners across statuses.",
    steps=[
        "Tab='pending'; type='architect'; search='Anand'.",
    ],
    expected="Composed filter list.",
))
add(dict(
    id="F-PRT-002",
    title="Partner detail — approve workflow",
    screen="lib/features/partnerships/presentation/screens/partner_admin_detail_screen.dart",
    role="ADMIN", priority="P2", type="Functional",
    pre="Partner pending.",
    steps=[
        "Tap Approve; confirm.",
    ],
    expected="Status active; tab badge counts update.",
    flutter_gap="F-G18 — no document verify / KYC checklist UI.",
))

# ============================== 24. WARRANTIES ============================
add(dict(
    id="F-WAR-001",
    title="Warranty list shows DLP timer",
    screen="lib/features/warranties/presentation/screens/warranties_screen.dart",
    role="PM / Customer", priority="P2", type="Construction-Domain Gap",
    pre="Warranty start = handover; period 12 months.",
    steps=[
        "Open warranties.",
    ],
    expected="Each row shows 'Expires in N days' chip; alert if < 30 days.",
    flutter_gap="F-G33 — DLP countdown not visible; no expiry alert.",
))
add(dict(
    id="F-WAR-002",
    title="Add warranty (PM)",
    screen="lib/features/warranties/presentation/screens/warranties_screen.dart",
    role="PM", priority="P3", type="Functional",
    pre="Project COMPLETED.",
    steps=[
        "Tap '+ Warranty'.",
        "Pick component, vendor, period.",
        "Save.",
    ],
    expected="Row inserted.",
))

# ============================== 25. DOCUMENTS / FILE VIEWER ===============
add(dict(
    id="F-DOC-001",
    title="Project document upload + categorisation",
    screen="lib/screens/documents/project_document_list_screen.dart",
    role="PM", priority="P2", type="Functional",
    pre="Project documents tab.",
    steps=[
        "Tap upload FAB; pick file; choose category; submit.",
    ],
    expected="Document appears in list; category chip applied.",
    flutter_gap="F-G34 — no upload progress indicator.",
))
add(dict(
    id="F-DOC-002",
    title="Approval centre — filter + pagination",
    screen="lib/screens/documents/approval_center_screen.dart",
    role="DIRECTOR", priority="P2", type="Functional",
    pre="Approvals across statuses.",
    steps=[
        "Filter='PENDING'.",
    ],
    expected="List filters; detail navigation today shows snackbar stub.",
    flutter_gap="F-G11 — approval detail screen not implemented.",
))
add(dict(
    id="F-DOC-003",
    title="Universal file viewer — PDF + image with auth",
    screen="lib/features/shared/universal_file_viewer_screen.dart",
    role="any", priority="P2", type="Functional",
    pre="Signed URL.",
    steps=[
        "Open PDF; open JPG.",
    ],
    expected="SfPdfViewer renders; image in InteractiveViewer. "
             "Authorization header attached via StorageService.",
    flutter_gap="F-G24 — no Office doc (.docx/.xlsx) preview; no video.",
))
add(dict(
    id="F-DOC-004",
    title="Create document — design agreement form",
    screen="lib/screens/documents/design_agreement_screen.dart",
    role="ADMIN / PM", priority="P3", type="Functional",
    pre="Form open.",
    steps=[
        "Fill client name, area, fee/sqft.",
    ],
    expected="Form validates GlobalKey<FormState>; save generates "
             "document.",
))

# ============================== 26. ACL EDITOR ============================
add(dict(
    id="F-ACL-001",
    title="ADMIN role immutable",
    screen="lib/screens/acl/acl_screen.dart",
    role="ADMIN", priority="P1", type="Security",
    pre="ACL screen.",
    steps=[
        "Select ADMIN role.",
    ],
    expected="All permission toggles disabled; Save bar hidden; explanation "
             "banner shown.",
))
add(dict(
    id="F-ACL-002",
    title="Permission edit — discard vs save",
    screen="lib/screens/acl/acl_screen.dart",
    role="ADMIN", priority="P2", type="Functional",
    pre="PM role selected.",
    steps=[
        "Toggle 2 permissions; tap Discard.",
        "Toggle again; tap Save.",
    ],
    expected="Discard reverts; Save persists; 'unsaved changes' badge "
             "disappears after Save.",
))
add(dict(
    id="F-ACL-003",
    title="Apply role template confirms",
    screen="lib/screens/acl/acl_screen.dart",
    role="ADMIN", priority="P2", type="Functional",
    pre="Custom role open.",
    steps=[
        "Tap a template chip.",
    ],
    expected="Confirmation dialog: 'Replace current permissions with "
             "<template>?'. Confirm applies preset.",
))
add(dict(
    id="F-ACL-004",
    title="Access denied for non-admin",
    screen="lib/screens/acl/acl_screen.dart",
    role="SALES", priority="P1", type="RBAC",
    pre="SALES user.",
    steps=[
        "Try to open /acl.",
    ],
    expected="_buildAccessDenied() renders; menu item also hidden.",
))

# ============================== 27. PORTAL USERS / TEAM ===================
add(dict(
    id="F-PU-001",
    title="Add portal user — role + enabled toggle",
    screen="lib/screens/portal_users/add_portal_user_screen.dart",
    role="ADMIN", priority="P1", type="Functional",
    pre="Add screen.",
    steps=[
        "Fill name/email/password; pick role 'PROJECT_MANAGER'; Save.",
    ],
    expected="User created; appears in list.",
    flutter_gap="F-G35 — role picker not project-scoped.",
))
add(dict(
    id="F-PU-002",
    title="Portal users list search resets pagination",
    screen="lib/screens/portal_users/portal_users_screen.dart",
    role="ADMIN", priority="P3", type="Functional",
    pre="Many users; page=3.",
    steps=[
        "Type 'rao' in search.",
    ],
    expected="page resets to 0; matching users shown.",
))
add(dict(
    id="F-TM-001",
    title="Team members — client-side filter",
    screen="lib/screens/team_members/team_members_screen.dart",
    role="ADMIN / HR", priority="P2", type="Functional",
    pre="100 team members.",
    steps=[
        "Search 'eng'; filter dept='Engineering'.",
    ],
    expected="Client-side filter shows only Engineering matches.",
    flutter_gap="F-G18 — no server pagination; all loaded — scale risk.",
))

# ============================== 28. PENDING SYNC ==========================
add(dict(
    id="F-SYNC-001",
    title="Queued tab shows pending entries with next-retry time",
    screen="lib/screens/sync/pending_sync_screen.dart",
    role="SITE_ENGINEER", priority="P1", type="Offline",
    pre="2 queued, network up.",
    steps=[
        "Open Pending Sync.",
    ],
    expected="Both rows visible with attempt count + nextRetryAt timestamp.",
))
add(dict(
    id="F-SYNC-002",
    title="Sync Now button triggers immediate drain",
    screen="lib/screens/sync/pending_sync_screen.dart",
    role="SITE_ENGINEER", priority="P1", type="Offline",
    pre="3 queued; network up.",
    steps=[
        "Tap 'Sync Now'.",
    ],
    expected="SyncService.triggerSyncNow runs; rows transition to done; "
             "list empties.",
))
add(dict(
    id="F-SYNC-003",
    title="Per-row Retry/Discard on permanent failure",
    screen="lib/screens/sync/pending_sync_screen.dart",
    role="SITE_ENGINEER", priority="P2", type="Resilience",
    pre="1 permanent failure.",
    steps=[
        "Tap Discard.",
    ],
    expected="Confirmation dialog; on confirm row removed; photo file "
             "deleted from local storage.",
))
add(dict(
    id="F-SYNC-004",
    title="Outbox not initialised on web",
    screen="lib/main.dart",
    role="any (web)", priority="P2", type="Offline Gap",
    pre="Browser.",
    steps=[
        "Open Pending Sync route.",
    ],
    expected="Route hidden / placeholder. Today: route shown but reads "
             "empty.",
    flutter_gap="F-G30 — web variant has no Drift backend.",
))

# ============================== 29. CONSTRUCTION-TYPE FLOWS ==============
add(dict(
    id="F-RES-001",
    title="RESIDENTIAL — full mobile field-ops day-flow",
    screen="multiple",
    role="SITE_ENGINEER (mobile)", priority="P1", type="End-to-End",
    pre="Residential project assigned.",
    steps=[
        "Login on mobile.",
        "Drawer → My Tasks → filter Active.",
        "Open task; capture progress 60% with note; Save.",
        "Open Site Reports → New; take 3 geotagged photos; submit.",
        "Switch network off; capture and submit a second report (queued).",
        "Switch network on; observe Pending Sync drain.",
    ],
    expected="All actions complete; queued report syncs without duplicate; "
             "PM inbox shows mark-completes for approval.",
    construction="Residential G+2 villa.",
))
add(dict(
    id="F-COM-001",
    title="COMMERCIAL — multi-phase Gantt & inter-state PO",
    screen="multiple",
    role="PM (desktop)", priority="P1", type="End-to-End",
    pre="Commercial project; vendor in another state.",
    steps=[
        "Apply 'Commercial-Standard-G+4' WBS template.",
        "Add inter-state PO (Maharashtra vendor).",
        "Issue invoice from a stage in Karnataka project.",
    ],
    expected="WBS cloned; PO uses IGST (per backend). Invoice should show "
             "IGST split — today only single GST line.",
    construction="Commercial G+4.",
    flutter_gap="F-G06 — IGST split UI missing.",
))
add(dict(
    id="F-INT-001",
    title="INTERIOR — DPC heavy on customisations",
    screen="multiple",
    role="ESTIMATOR / PM", priority="P1", type="End-to-End",
    pre="Lead 'interior_work'.",
    steps=[
        "Generate estimation in interior mode.",
        "Convert to project; approve BOQ (no structural items).",
        "Issue DPC with 6 customisations (modular kitchen, wardrobes).",
    ],
    expected="Interior template excludes structural scopes; DPC PDF lists "
             "customisations in dedicated section.",
    construction="Interior 3BHK fit-out.",
))
add(dict(
    id="F-REN-001",
    title="RENOVATION — pre-work photos + demolition deduction",
    screen="multiple",
    role="PM", priority="P1", type="End-to-End",
    pre="Existing house; demolition planned.",
    steps=[
        "Gallery → upload 20 pre-work photos.",
        "BOQ → mark salvage as DEDUCTION item (negative impact).",
        "Issue stage payment schedule 25/25/25/25.",
    ],
    expected="Pre-work photos archived; deductions reflected in net contract "
             "value.",
    construction="Renovation with selective demolition.",
))
add(dict(
    id="F-SMH-001",
    title="SMART-HOME — add-on CO with OTP customer approval",
    screen="multiple",
    role="PM", priority="P2", type="End-to-End",
    pre="Existing project.",
    steps=[
        "Create CO 'Smart home IoT kit' ₹3.5L.",
        "Submit → Internal Approve → Send to customer.",
        "Customer enters OTP; approves.",
        "Start work; commissioning QC check.",
    ],
    expected="Status → IN_PROGRESS; advance invoice generated; warranty row "
             "added on completion.",
    construction="Smart-home retrofit.",
))

# ============================== 30. INDIA UI COMPLIANCE ==================
add(dict(
    id="F-IND-001",
    title="Indian number formatting in financial dashboard",
    screen="lib/features/boq/.../payment_schedule_screen.dart",
    role="FINANCE", priority="P3", type="Localisation",
    pre="Total = ₹12,34,56,789.",
    steps=[
        "Open payment schedule.",
    ],
    expected="Displayed '12,34,56,789' (Indian grouping). Today: many "
             "screens raw or US grouping.",
    flutter_gap="F-G28 — no shared lakh/crore formatter.",
))
add(dict(
    id="F-IND-002",
    title="GST split UI on invoice — gap",
    screen="lib/features/boq/.../boq_invoice_screen.dart",
    role="FINANCE", priority="P1", type="India Compliance Gap",
    pre="Intra-state invoice.",
    steps=[
        "Open invoice detail.",
    ],
    expected="CGST 9% + SGST 9% lines. Today: single GST line.",
    flutter_gap="F-G06 — CGST/SGST/IGST split absent.",
))
add(dict(
    id="F-IND-003",
    title="TDS field on vendor payment — gap",
    screen="Finance / vendor payment dialog",
    role="FINANCE", priority="P1", type="India Compliance Gap",
    pre="Vendor invoice.",
    steps=[
        "Record payment.",
    ],
    expected="TDS section selector (194C/194J), rate, slip download. "
             "Today: absent.",
    flutter_gap="F-G07 — no TDS UI.",
))
add(dict(
    id="F-IND-004",
    title="HSN/SAC code input on material",
    screen="lib/screens/inventory/add_material_screen.dart",
    role="ADMIN", priority="P1", type="India Compliance Gap",
    pre="Add material.",
    steps=[
        "Fill name/unit/rate; look for HSN/SAC.",
    ],
    expected="HSN/SAC required input (4-8 digits per backend G-21).",
    flutter_gap="F-G08 — HSN/SAC field missing on material form.",
))
add(dict(
    id="F-IND-005",
    title="Aadhaar masked in labour list",
    screen="lib/screens/labour/labour_list_screen.dart",
    role="any", priority="P1", type="Privacy",
    pre="Labour with idProofType=AADHAAR.",
    steps=[
        "Open list / detail.",
    ],
    expected="Aadhaar shown as '**** **** 9012' (last 4 only).",
    flutter_gap="F-G29 — verify masking in labour detail.",
))
add(dict(
    id="F-IND-006",
    title="Date format DD/MM/YYYY across screens",
    screen="multiple",
    role="any", priority="P3", type="Localisation",
    pre="Device en_IN.",
    steps=[
        "Open Task Create, Site Report list, BOQ doc.",
    ],
    expected="Dates consistently DD MMM YYYY or DD/MM/YYYY.",
    flutter_gap="F-G16 — some date strings use literal `{day}/{month}/"
                "{year}` not locale-aware.",
))
add(dict(
    id="F-IND-007",
    title="RERA number on residential project (>8 units)",
    screen="Project create / customer projects",
    role="PM", priority="P1", type="India Compliance Gap",
    pre="Residential project >8 units.",
    steps=[
        "Open project create form.",
    ],
    expected="Should show RERA registration number field. Today: missing.",
    flutter_gap="F-G06 — no RERA capture (ties to backend G-27).",
))

# ============================== 31. RESPONSIVE / A11Y / THEME =============
add(dict(
    id="F-RSP-001",
    title="Side menu vs drawer transitions at 1100px",
    screen="lib/screens/main/main_screen.dart",
    role="any", priority="P2", type="Responsive",
    pre="Tablet at 1099px width.",
    steps=[
        "Resize window to 1100px.",
    ],
    expected="Layout flips from drawer + bottom nav to sidebar + content. No "
             "flicker. Selected menu item preserved.",
))
add(dict(
    id="F-RSP-002",
    title="DPC builder stacks below 1200px",
    screen="lib/features/dpc/presentation/screens/dpc_builder_screen.dart",
    role="any", priority="P3", type="Responsive",
    pre="Builder open at 1199px.",
    steps=[
        "Increase width past 1200px.",
    ],
    expected="PDF preview pane attaches to the right.",
))
add(dict(
    id="F-A11Y-001",
    title="Screen reader on main login flow",
    screen="lib/screens/auth/portal_login_screen.dart",
    role="any (NVDA / TalkBack)", priority="P3", type="Accessibility",
    pre="Screen reader on.",
    steps=[
        "Navigate through email / password / sign in.",
    ],
    expected="Each field announces label + state. Today: limited Semantics.",
    flutter_gap="F-G29 — no Semantics widgets added.",
))
add(dict(
    id="F-A11Y-002",
    title="Contrast on coral red brand colour",
    screen="theme / lib/theme/app_theme.dart",
    role="any", priority="P3", type="Accessibility",
    pre="High-contrast device.",
    steps=[
        "Measure contrast of coralRed buttons on white.",
    ],
    expected="WCAG AA >= 4.5; coralRed (#F36F72) on white likely fails — "
             "needs darker variant for text-on-white.",
    flutter_gap="F-G29 — verify WCAG conformance.",
))
add(dict(
    id="F-THM-001",
    title="Dark mode toggle absent",
    screen="lib/theme/app_theme.dart",
    role="any", priority="P3", type="Theme Gap",
    pre="Anywhere.",
    steps=[
        "Look for dark mode toggle in profile / settings.",
    ],
    expected="Today: not implemented.",
    flutter_gap="F-G27 — light theme only.",
))
add(dict(
    id="F-LOC-001",
    title="Hindi / Kannada strings — not present",
    screen="all",
    role="any", priority="P3", type="Localisation Gap",
    pre="Device locale hi_IN or kn_IN.",
    steps=[
        "Open any screen.",
    ],
    expected="All strings English. Today: no `AppLocalizations` or .arb "
             "files.",
    flutter_gap="F-G28 — no i18n.",
))

# ============================== 32. OFFLINE-SYNC EDGES ====================
add(dict(
    id="F-OFF-001",
    title="App killed mid-photo-upload — resume on relaunch",
    screen="lib/services/sync_service.dart",
    role="SITE_ENGINEER", priority="P1", type="Offline",
    pre="2 photos queued; photo 1 mid-upload.",
    steps=[
        "Force-kill app.",
        "Relaunch.",
    ],
    expected="OutboxService rehydrates; SyncService re-attempts. No "
             "duplicate from photo 1 (idempotency key).",
))
add(dict(
    id="F-OFF-002",
    title="Storage full mid-photo capture",
    screen="lib/features/site_reports/...",
    role="SITE_ENGINEER", priority="P2", type="Resilience",
    pre="Device storage 99% full.",
    steps=[
        "Try to capture a photo.",
    ],
    expected="ImagePicker fails gracefully; UI shows 'Storage full — free up "
             "space'.",
    flutter_gap="F-G18 — verify error handling on PickerException.",
))
add(dict(
    id="F-OFF-003",
    title="Outbox photo file deleted by OS cleanup",
    screen="lib/services/outbox_service.dart",
    role="SITE_ENGINEER", priority="P2", type="Resilience",
    pre="Photo cached in tmp; OS deletes due to space pressure.",
    steps=[
        "Open Pending Sync; retry.",
    ],
    expected="Service detects missing file; row moves to permanent failure "
             "with clear reason.",
))
add(dict(
    id="F-OFF-004",
    title="Time-zone drift between device and server",
    screen="multiple",
    role="any", priority="P2", type="Edge",
    pre="Device clock set 10 mins ahead.",
    steps=[
        "Submit a site report.",
    ],
    expected="Server stores UTC of actual receipt; UI later displays in "
             "device local (IST). No date display jump.",
))
add(dict(
    id="F-OFF-005",
    title="Sync conflict — same task progress edited offline by 2 devices",
    screen="lib/services/sync_service.dart",
    role="2 SITE_ENGINEERS", priority="P2", type="Concurrency Gap",
    pre="Device A offline, Device B offline. Both update progress for same "
        "task.",
    steps=[
        "Bring both online; allow sync.",
    ],
    expected="Today: last writer wins. Goal: conflict UI in Pending Sync.",
    flutter_gap="F-G23 — no conflict-resolution UI.",
))

# ============================== 33. ERROR / RESILIENCE ===================
add(dict(
    id="F-RES-001",
    title="Global error handler catches uncaught async errors",
    screen="lib/main.dart",
    role="any", priority="P2", type="Resilience",
    pre="Force a deliberate throw in a screen build.",
    steps=[
        "Trigger.",
    ],
    expected="FlutterError.onError + PlatformDispatcher.onError logged; "
             "screen shows red Material error box (debug) or generic "
             "fallback (release).",
    flutter_gap="F-G26 — no per-route ErrorBoundary widget.",
))
add(dict(
    id="F-RES-002",
    title="No internet — list shows actionable error state",
    screen="lib/features/leads/presentation/screens/leads_screen.dart",
    role="any", priority="P2", type="Resilience",
    pre="Airplane mode.",
    steps=[
        "Open Leads.",
    ],
    expected="ErrorStateWidget with retry button; offline banner above.",
))
add(dict(
    id="F-RES-003",
    title="401 mid-action triggers silent refresh",
    screen="lib/services/api_service.dart",
    role="any", priority="P1", type="Resilience",
    pre="Access token expired during a form submit.",
    steps=[
        "Submit form.",
    ],
    expected="ApiService interceptor calls /auth/refresh-token; retries "
             "original request once; user sees nothing.",
))

# ============================== 34. SECURITY UI ==========================
add(dict(
    id="F-SEC-001",
    title="CCTV password masked while typing",
    screen="lib/screens/projects/cctv_camera_form_screen.dart",
    role="ADMIN", priority="P1", type="Security",
    pre="Form open.",
    steps=[
        "Type password.",
    ],
    expected="obscureText = true; toggle eye to reveal.",
    flutter_gap="F-G09 — verify masked; payload not logged in console.",
))
add(dict(
    id="F-SEC-002",
    title="Logs do not contain auth token",
    screen="all",
    role="any", priority="P1", type="Security",
    pre="Profile screen.",
    steps=[
        "Inspect console / logcat during navigation.",
    ],
    expected="No 'Bearer <jwt>' string appears in logs.",
))
add(dict(
    id="F-SEC-003",
    title="Storage service writes secure on mobile",
    screen="lib/services/storage_service.dart",
    role="any", priority="P1", type="Security",
    pre="Mobile.",
    steps=[
        "Login; inspect.",
    ],
    expected="Tokens via flutter_secure_storage (Keychain / Keystore). "
             "Plain SharedPreferences only on web (web has limitations).",
))


# ============================== 35. SELF-CRITIQUE PATCH ==================
# Second-pass additions: push notification routing, force upgrade, customer
# portal, keyboard occlusion, image-permission flows, app-lifecycle edges,
# and missed construction screens.
# ===========================================================================
add(dict(
    id="F-PUSH-001",
    title="Tap FCM push deep-links to the right screen",
    screen="lib/main.dart + lib/services/push_notification_service.dart",
    role="any", priority="P1", type="Functional",
    pre="App backgrounded; server sends STAGE_CERTIFIED push with "
        "{type:'stage', stageId:42, projectId:7}.",
    steps=[
        "Tap the notification on the lock screen.",
    ],
    expected="App resumes and navigates to PaymentScheduleScreen of project "
             "7 with stage 42 scrolled into view. Today: routing may stop "
             "at default landing.",
    flutter_gap="F-G31 — verify FCM payload → GoRouter resolution.",
))
add(dict(
    id="F-PUSH-002",
    title="Foreground push surfaces in-app banner not OS notification",
    screen="lib/services/push_notification_service.dart",
    role="any", priority="P2", type="UX",
    pre="App open on Leads screen.",
    steps=[
        "Receive LEAD_ASSIGNED push.",
    ],
    expected="In-app banner / Snackbar 'New lead assigned'; tap routes to "
             "lead. No duplicate OS notif.",
))
add(dict(
    id="F-PUSH-003",
    title="FCM token deregistered on logout",
    screen="lib/services/portal_auth_service.dart",
    role="any", priority="P1", type="Privacy",
    pre="Logged in with FCM token.",
    steps=[
        "Logout.",
    ],
    expected="POST /auth/fcm-token with empty token or DELETE call clears "
             "server-side token so subsequent pushes don't reach this device.",
    flutter_gap="F-G18 — confirm token clear on logout.",
))
add(dict(
    id="F-UPG-001",
    title="Forced-upgrade screen blocks navigation",
    screen="lib/main.dart + version check",
    role="any", priority="P1", type="Compatibility",
    pre="Installed app v2.3; minimum supported v3.0.",
    steps=[
        "Open app; observe.",
    ],
    expected="Modal 'Update required' with store link. No way to dismiss; "
             "cannot reach login.",
    flutter_gap="F-G18 — confirm min-version policy is wired.",
))
add(dict(
    id="F-UPG-002",
    title="Non-blocking upgrade prompt allows skip",
    screen="lib/main.dart",
    role="any", priority="P2", type="Compatibility",
    pre="Installed v3.0; latest v3.1 (non-breaking).",
    steps=[
        "Open app.",
    ],
    expected="Dismissable banner with 'Update available'. Skippable.",
))
add(dict(
    id="F-CST-001",
    title="Customer portal — project list scoped to user",
    screen="customer-facing routes (TBD)",
    role="CUSTOMER", priority="P1", type="RBAC",
    pre="Customer with 2 projects.",
    steps=[
        "Login on /customer.",
    ],
    expected="Only the 2 projects visible; navigation rail / drawer "
             "restricted to Documents, BOQ Acknowledge, Payments, Support.",
    flutter_gap="F-G06 — customer portal coverage minimal in this codebase "
                "(majority is internal staff app).",
))
add(dict(
    id="F-CST-002",
    title="Customer acknowledges BOQ from portal",
    screen="customer BOQ document view",
    role="CUSTOMER", priority="P1", type="Functional",
    pre="BOQ APPROVED internally.",
    steps=[
        "Customer opens BOQ on portal; taps Acknowledge.",
    ],
    expected="customerAcknowledgedAt/By stored; UI shows 'Acknowledged on "
             "<date>' chip; no status change.",
))
add(dict(
    id="F-CST-003",
    title="Customer pays stage via UPI (deep link)",
    screen="customer payment screen",
    role="CUSTOMER", priority="P2", type="India / Payment",
    pre="Stage DUE.",
    steps=[
        "Customer taps 'Pay via UPI'.",
    ],
    expected="upi:// intent fires with payee VPA + amount. After payment, "
             "user returns; receipt awaits manual confirmation by Finance.",
    flutter_gap="F-G06 — UPI deep-link integration not visible in repo.",
))
add(dict(
    id="F-KBD-001",
    title="Soft keyboard does not occlude submit button on long form",
    screen="lib/features/leads/presentation/screens/add_lead_screen.dart",
    role="any", priority="P2", type="UX",
    pre="Mobile portrait.",
    steps=[
        "Tap on Notes (last field) — keyboard rises.",
        "Scroll to Save button.",
    ],
    expected="Scaffold resizes; Save button reachable above keyboard.",
    flutter_gap="F-G18 — confirm resizeToAvoidBottomInset / SingleChildScrollView.",
))
add(dict(
    id="F-KBD-002",
    title="Done action on phone keyboard advances to next field",
    screen="forms across app",
    role="any", priority="P3", type="UX",
    pre="Form open.",
    steps=[
        "Tap 'Next' on numeric keyboard.",
    ],
    expected="Focus moves to next TextFormField rather than dismissing "
             "keyboard prematurely.",
    flutter_gap="F-G18 — many forms omit textInputAction = TextInputAction.next.",
))
add(dict(
    id="F-PERM-001",
    title="Camera permission denied — actionable recovery",
    screen="lib/features/site_reports/...",
    role="SITE_ENGINEER", priority="P1", type="Permission",
    pre="Camera permission previously denied 'Don't ask again'.",
    steps=[
        "Tap 'Capture photo'.",
    ],
    expected="Dialog 'Camera disabled — Open Settings'; tap launches OS "
             "settings deep link. Returning enables capture.",
    flutter_gap="F-G18 — verify permission_handler hook.",
))
add(dict(
    id="F-PERM-002",
    title="Storage / Photos permission for gallery picker (Android 13+)",
    screen="lib/features/site_reports/...",
    role="SITE_ENGINEER", priority="P1", type="Permission",
    pre="Android 13 device; READ_MEDIA_IMAGES not granted.",
    steps=[
        "Tap 'Pick from gallery'.",
    ],
    expected="Granular media permission prompt (not legacy "
             "READ_EXTERNAL_STORAGE). Picker opens after grant.",
))
add(dict(
    id="F-LIFE-001",
    title="App backgrounded during photo upload — upload continues",
    screen="lib/services/sync_service.dart",
    role="SITE_ENGINEER", priority="P2", type="Resilience",
    pre="Multi-photo upload in progress.",
    steps=[
        "Press Home; wait 30s; reopen app.",
    ],
    expected="Foreground service (Android) or background URLSession (iOS) "
             "completes the upload. Sync screen shows progress on resume.",
    flutter_gap="F-G18 — confirm WorkManager / BGTaskScheduler integration.",
))
add(dict(
    id="F-LIFE-002",
    title="Phone rotation mid-form preserves field values",
    screen="lib/features/leads/presentation/screens/add_lead_screen.dart",
    role="any", priority="P2", type="Resilience",
    pre="Form partially filled.",
    steps=[
        "Rotate device to landscape.",
    ],
    expected="Field values preserved (Form state retained); no data loss.",
))
add(dict(
    id="F-LIFE-003",
    title="Hot reload retains route but not transient state",
    screen="lib/main.dart",
    role="dev", priority="P3", type="Resilience",
    pre="Dev build.",
    steps=[
        "Trigger hot reload while on Gantt.",
    ],
    expected="Gantt re-renders on same route; CPM provider may re-fetch.",
))
add(dict(
    id="F-EVID-001",
    title="EXIF stripped from photo before upload (privacy)",
    screen="lib/features/site_reports/...",
    role="SITE_ENGINEER", priority="P2", type="Privacy",
    pre="Photo captured with personal metadata (device model, geotag).",
    steps=[
        "Submit; intercept request body.",
    ],
    expected=(
        "Today: EXIF preserved (per code review) to support forensic audit. "
        "Option needed: strip personal metadata while preserving project "
        "geotag (which is captured server-side too). Decision required."
    ),
    flutter_gap="F-G40 — EXIF retention strategy needs product decision.",
))
add(dict(
    id="F-LONG-001",
    title="Specifications field 5000-char limit on BOQ",
    screen="lib/features/boq/presentation/screens/boq_screen.dart",
    role="ESTIMATOR", priority="P3", type="Validation",
    pre="Create BOQ item.",
    steps=[
        "Paste 6000-char text into specifications.",
    ],
    expected="Server validation rejects; UI should show counter '5000/5000'.",
    flutter_gap="F-G18 — counter widget not visible on screen.",
))
add(dict(
    id="F-LONG-002",
    title="Notes field accepts multiline (newlines preserved)",
    screen="any notes field",
    role="any", priority="P3", type="Functional",
    pre="Form open.",
    steps=[
        "Type 'Line1\\nLine2' (Shift+Enter on web).",
    ],
    expected="Newlines preserved in display.",
))
add(dict(
    id="F-PROV-001",
    title="Provider disposed on screen pop avoids leaks",
    screen="multiple",
    role="dev / QA", priority="P2", type="Memory",
    pre="Open / close Gantt 50 times.",
    steps=[
        "Trigger heap snapshot.",
    ],
    expected="GanttCpmProvider, CpmTaskResult lists garbage-collected; no "
             "ever-growing retain.",
    flutter_gap="F-G18 — audit Provider lifecycle in heavy screens.",
))
add(dict(
    id="F-WEB-001",
    title="Web build PWA install / service worker",
    screen="web build",
    role="any", priority="P3", type="Web",
    pre="Chrome 120+.",
    steps=[
        "Open portal in browser.",
    ],
    expected="Install prompt available (PWA manifest); service worker "
             "caches static assets.",
    flutter_gap="F-G30 — service worker offline strategy missing.",
))
add(dict(
    id="F-BRO-001",
    title="Brochure download from lead page",
    screen="lead documents / brochure (BrochureController API)",
    role="SALES", priority="P3", type="Functional",
    pre="Brochures uploaded by admin.",
    steps=[
        "Open lead; select brochure to share.",
    ],
    expected="PDF downloads or share-sheet opens (WhatsApp, email).",
    flutter_gap="F-G24 — brochure download UI presence not verified.",
))
add(dict(
    id="F-PROC-RFQ-001",
    title="Vendor quotation comparison side-by-side",
    screen="lib/features/procurement/presentation/screens/quotation_management_screen.dart",
    role="PROCUREMENT", priority="P2", type="Functional",
    pre="3 quotations on indent.",
    steps=[
        "Open quotation comparison.",
    ],
    expected="Table with 3 columns: amount, delivery, taxes, validity. "
             "Highlight lowest amount. Select winner button.",
))
add(dict(
    id="F-RET-001",
    title="Subcontract retention release detail visible",
    screen="lib/screens/projects/subcontract_work_order_detail_screen.dart",
    role="PM / Finance", priority="P2", type="Construction-Domain",
    pre="Subcontract with totalRetentionAccumulated = ₹50,000.",
    steps=[
        "Open work order detail.",
    ],
    expected="Retention card: accumulated, released, balance. Release "
             "button gated by DLP end date.",
    flutter_gap="F-G38 — retention breakdown only in detail, not list.",
))
add(dict(
    id="F-MOD-001",
    title="Module grid 'coming soon' affordance",
    screen="lib/features/projects/presentation/screens/project_detail_screen.dart",
    role="any", priority="P3", type="UX",
    pre="Tile maps to unimplemented module.",
    steps=[
        "Tap that tile.",
    ],
    expected="Snackbar 'Coming soon'. Today: silent or generic error.",
    flutter_gap="F-G18 — better disable styling for inactive tiles.",
))
add(dict(
    id="F-SUB-RFQ-001",
    title="Approval centre — bulk approve with same reason",
    screen="lib/screens/documents/approval_center_screen.dart",
    role="DIRECTOR", priority="P2", type="UX gap",
    pre="6 similar approvals pending.",
    steps=[
        "Select multiple rows; tap Approve.",
    ],
    expected="Today: no bulk action. Goal: checkbox + bulk approve dialog.",
    flutter_gap="F-G18 — bulk operations missing.",
))


# ============================== 37. HSN/SAC, AGREED, 409 CONFLICT (FA/BOQ) =
add(dict(
    id="F-HSN-001",
    title="HSN/SAC visible + editable on BOQ line row",
    screen="lib/screens/boq/boq_line_editor.dart",
    role="ESTIMATOR", priority="P1", type="Functional",
    pre="BOQ line list view.",
    steps=[
        "Open a line; inspect form.",
    ],
    expected="HSN/SAC text field with input formatter restricting to 4–8 digits. "
             "Pre-filled from selected material's hsn_sac_code (V137). "
             "Save fails with 'invalid HSN' if regex violates.",
    flutter_gap="F-G37 — HSN now shown on row chip too.",
))
add(dict(
    id="F-AGREED-001",
    title="AGREED BOQ line shows lock icon; rate field disabled",
    screen="lib/screens/boq/boq_line_editor.dart",
    role="ESTIMATOR", priority="P1", type="Business Rule",
    pre="Line state=AGREED; stage-1 invoice raised.",
    steps=[
        "Try to edit unit rate.",
    ],
    expected="Rate field disabled with tooltip 'Locked after invoice — raise a "
             "Change Order'. CO button appears.",
))
add(dict(
    id="F-409-001",
    title="Final Account stale edit shows 409 conflict UX",
    screen="lib/screens/final_account/final_account_editor.dart",
    role="PROJECT_MANAGER", priority="P1", type="Concurrency",
    pre="User A and B both have FA open; B saves first.",
    steps=[
        "User A taps Save.",
    ],
    expected="Banner 'Final Account changed by another user' with two buttons: "
             "'Discard and reload' (refetch + replace local form) and 'Keep my "
             "edits' (download fresh + show inline diff). No silent overwrite.",
    flutter_gap="F-G23 — wires backend 409 to a usable client flow.",
))

# ============================== 38. PROJECT DELETE / CUSTOMER LIFECYCLE ===
add(dict(
    id="F-PRJ-DEL-001",
    title="Admin-only Delete Project — typed-confirmation dialog",
    screen="lib/screens/projects/project_detail_screen.dart",
    role="ADMIN", priority="P1", type="Security",
    pre="Project P-77 detail open; viewer is ADMIN.",
    steps=[
        "Tap kebab → Delete Project.",
    ],
    expected="Dialog: 'Type project code WD-77 to confirm'. Delete disabled until "
             "exact match. After delete, route pops to project list; toast "
             "confirms; project hidden from list.",
))
add(dict(
    id="F-PRJ-DEL-002",
    title="Non-admin does not see Delete Project entry",
    screen="lib/screens/projects/project_detail_screen.dart",
    role="PROJECT_MANAGER", priority="P1", type="RBAC",
    pre="Same project, role=PM.",
    steps=[
        "Open kebab.",
    ],
    expected="Delete Project not rendered (not just disabled). UI does not leak "
             "the option's existence.",
))
add(dict(
    id="F-CST-LC-001",
    title="Customer kebab shows Activate/Deactivate, no Delete (regress 1aa1fb46)",
    screen="lib/screens/customers/customer_list_screen.dart",
    role="ADMIN", priority="P1", type="Refactor regression",
    pre="Customer list.",
    steps=[
        "Open row kebab.",
    ],
    expected="Only Activate or Deactivate is shown (mutex on state). Delete is "
             "absent from kebab. Delete reachable only from a separate confirm "
             "flow (7e04b70d).",
))
add(dict(
    id="F-CST-LC-002",
    title="Server outcome surfaced — Deleted vs Deactivated toast (regress ad2632d9)",
    screen="lib/screens/customers/customer_list_screen.dart",
    role="ADMIN", priority="P1", type="UX",
    pre="Customer has linked lead.",
    steps=[
        "Perform delete on a deletable customer.",
        "Perform delete on a customer with linked lead.",
    ],
    expected="Toast text differs: 'Customer deleted' vs 'Customer deactivated "
             "(has linked records)'. List row removed or recoloured accordingly.",
))

# ============================== 39. IDEMPOTENCY KEYS IN OUTBOX =============
add(dict(
    id="F-IDM-001",
    title="Outbox attaches X-Idempotency-Key on financial mutations",
    screen="lib/services/outbox_service.dart",
    role="any", priority="P1", type="Functional",
    pre="Offline; user marks stage certified; outbox enqueues.",
    steps=[
        "Reconnect; outbox drains.",
        "Force a retry of the same entry.",
    ],
    expected="Each outbox row carries a stable UUID v4 idempotency key generated "
             "at enqueue time. Retry uses same key. Backend dedupes; ledger shows "
             "exactly one effect.",
    flutter_gap="F-G22 — must cover stage certify, payment confirm, invoice raise.",
))
add(dict(
    id="F-IDM-002",
    title="Idempotency key persists across app kill / device reboot",
    screen="lib/services/outbox_service.dart + Drift schema",
    role="any", priority="P1", type="Reliability",
    pre="Outbox row with key 'k1' pending.",
    steps=[
        "Kill app; reboot device; open app.",
    ],
    expected="Row still has 'k1'; drain uses same key. Drift schema persists "
             "the column; key never regenerated on resume.",
))

# ============================== 40. FCM PUSH ROUTING / DEDUP ===============
add(dict(
    id="F-FCM-DL-001",
    title="STAGE_DUE push deep-links to PaymentSchedule with stage scrolled-into-view",
    screen="lib/services/push_notification_service.dart",
    role="any", priority="P1", type="Functional",
    pre="Backgrounded; server emits payload {type:'STAGE_DUE', stageId:42, projectId:7}.",
    steps=[
        "Tap notification.",
    ],
    expected="App resumes; navigates to PaymentScheduleScreen of project 7; "
             "stage 42 row is scrolled into view and visually highlighted "
             "for 2s. If deep-link fails, falls back to landing with toast.",
    flutter_gap="F-G31 — confirm route table covers STAGE_DUE, INVOICE_RAISED, "
                "LEAD_ASSIGNED, FA_AGREED.",
))
add(dict(
    id="F-FCM-DL-002",
    title="Two pushes within window — only one in-app banner",
    screen="lib/services/push_notification_service.dart",
    role="any", priority="P2", type="UX",
    pre="App foregrounded.",
    steps=[
        "Server emits two STAGE_DUE for same stage within 1 min.",
    ],
    expected="Client de-dupes by stageId+kind+yyyymmdd; only one banner; "
             "subsequent suppressed (mirrors server INSERT-ON-CONFLICT dedup).",
))

# ============================== 41. MONEY FORMATTING / I18N ===============
add(dict(
    id="F-MONEY-001",
    title="Money fields render as Indian lakh/crore with 2dp",
    screen="lib/utils/money_format.dart (or shared)",
    role="any", priority="P1", type="Localisation",
    pre="Amount 12,345,678.5 paise.",
    steps=[
        "Render in invoice, BOQ summary, dashboard tile.",
    ],
    expected="Display '₹1,23,45,678.51' (Indian grouping). HALF_UP at 2dp matches "
             "server MoneyMath. Negative values prefixed with '-' not '()'.",
    flutter_gap="F-G28 — shared Indian numeral formatter still missing in places.",
))
add(dict(
    id="F-MONEY-002",
    title="Cumulative GST roll-up matches server within 1 paise",
    screen="lib/screens/invoice/invoice_preview_screen.dart",
    role="FINANCE", priority="P1", type="Regression",
    pre="50-line invoice in preview.",
    steps=[
        "Compare on-screen total vs server-issued PDF.",
    ],
    expected="Difference ≤ ₹0.01 on both subtotal and GST.",
))

# ============================== 42. LEAD INTERACTIONS UI (V133) ===========
add(dict(
    id="F-LEAD-INT-001",
    title="Lead detail shows interactions timeline + next-follow-up chip",
    screen="lib/screens/leads/lead_detail_screen.dart",
    role="SALES", priority="P1", type="Functional",
    pre="Lead with 3 interactions: CALL, WHATSAPP, EMAIL.",
    steps=[
        "Open lead.",
    ],
    expected="Timeline ordered desc by occurred_at; channel icon + outcome + "
             "notes preview. Next-follow-up chip turns red when overdue. Add "
             "Interaction CTA visible.",
))
add(dict(
    id="F-LEAD-INT-002",
    title="Quick-log Interaction sheet — minimal taps to record CALL outcome",
    screen="lib/screens/leads/quick_log_interaction_sheet.dart",
    role="SALES", priority="P2", type="UX",
    pre="Lead detail open.",
    steps=[
        "Tap Quick-log → CALL → No answer → Save.",
    ],
    expected="Bottom sheet with channel chips, outcome chips, optional notes, "
             "next-follow-up date picker. Total ≤ 3 taps for the common case.",
))

# ============================== 43. RESPONSIVE / OFFLINE ON WEB ============
add(dict(
    id="F-WEB-001",
    title="Web build hides offline-only entry points",
    screen="lib/app_shell.dart + offline gates",
    role="any", priority="P2", type="UX / Web",
    pre="Build kIsWeb=true.",
    steps=[
        "Inspect drawer + dashboard.",
    ],
    expected="Pending Sync, My Tasks (offline mode), Outbox screen are hidden "
             "on web (F-G30). A small disclaimer 'Offline mode unavailable on "
             "web — use mobile app for field work' shown in profile.",
))
add(dict(
    id="F-RESP-001",
    title="BOQ table → cards transition at < 600px",
    screen="lib/screens/boq/boq_list_screen.dart",
    role="any", priority="P2", type="Responsive",
    pre="DevTools at 1200px then 360px.",
    steps=[
        "Resize.",
    ],
    expected="DataTable layout on wide. Card list with horizontal-scroll chips "
             "on narrow. No clipped totals; no horizontal overflow indicator.",
))


# ---------------------------------------------------------------------------
# Flutter implementation gaps register
# ---------------------------------------------------------------------------
GAPS = [
    ("F-G01", "Auth", "Login regex outdated (TLD <= 4 chars); rejects valid emails like .museum."),
    ("F-G02", "Auth", "Reset-token guard is only length>20, not UUID/sig format check."),
    ("F-G03", "Routing", "Mixed Navigator.push and go_router; no deep-link for /projects/:id."),
    ("F-G04", "Gantt", "Non-virtualised chart list; performance risk at 100+ tasks on mid-range devices."),
    ("F-G05", "Tasks", "No client-side predecessor cycle detection; relies on backend response."),
    ("F-G06", "India / GST", "No CGST/SGST/IGST split UI; no RERA capture; no place-of-supply selector."),
    ("F-G07", "India / TDS", "No TDS section selector, rate input, or TDS slip download anywhere."),
    ("F-G08", "Materials / HSN", "No HSN/SAC input on material form (backend G-21 added column; UI not wired)."),
    ("F-G09", "CCTV", "Camera password field stored plaintext in form; no auth test pre-save."),
    ("F-G10", "ProjectDetail", "Permission-gated tabs (schedule-config) hidden silently without 'Access Denied' affordance."),
    ("F-G11", "Subcontract / Approval", "Detail screens stubbed — show snackbar TODO."),
    ("F-G12", "Tasks / Monsoon", "Monsoon-sensitive flag stored but absent from task list / create / detail; only Gantt warning chip."),
    ("F-G13", "PM Approval Inbox", "Photo only 56 px thumbnail; no fullscreen preview; no bulk approve."),
    ("F-G14", "WBS Templates", "Predecessor editor UI missing (only count shown); duration always 'days'; no hours/weeks selector."),
    ("F-G15", "Holiday Calendar", "No project-specific override flag; cannot disable a state holiday for a single project."),
    ("F-G16", "Localisation / Dates", "Some screens use literal `{day}/{month}/{year}`; not locale-aware."),
    ("F-G17", "State Management", "No PageStorageKey on long lists; scroll position lost; menu expansion state ad-hoc; active visit banner not global."),
    ("F-G18", "UX general", "Stale caches on re-entry (role list, partner lookup); no draft auto-save; silent background-refresh failures; partial doc upload progress; no GST auto-calc on VO; no approver workflow on stock adjust; no PF/ESI in wage sheet."),
    ("F-G19", "Search / Lists", "No debounce on search bars; type counts computed only from current page; no infinite-scroll guard against duplicate fetch; chip list not auto-scrolled to selection; bulk attendance has no RFID/Aadhaar capture."),
    ("F-G20", "Forms", "Multi-step wizards have no progress indicator and no auto-save."),
    ("F-G21", "Confirmations", "Missing on irreversible actions: BOQ approve, DPC Issue, stage certify, FA agree, retention release."),
    ("F-G22", "Idempotency", "No X-Idempotency-Key on stage certify, payment confirm, invoice raise; network retry could duplicate."),
    ("F-G23", "Concurrency", "No optimistic-lock 409 conflict UI for FinalAccount (backend G-19 fix needs client follow-through)."),
    ("F-G24", "PDF Preview / Export", "No in-app PDF preview before invoice/certificate issue; no PDF for VO; no print/share/annotate in viewer."),
    ("F-G25", "View 360", "No VR mode, pinch-zoom dependent on `panorama` pkg internals."),
    ("F-G26", "Error handling", "No per-route ErrorBoundary widget; reliance on global handlers only."),
    ("F-G27", "Theme", "Light theme only; no dark-mode toggle, no font-scale setting."),
    ("F-G28", "Localisation", "No i18n (`AppLocalizations` / .arb absent); no shared Indian lakh/crore formatter."),
    ("F-G29", "Accessibility / Privacy", "Few Semantics widgets; coral brand colour likely fails WCAG AA contrast for buttons; no client-side Aadhaar masking helper."),
    ("F-G30", "Offline / Web", "OutboxService + drift skipped on web — web users have no offline capability for site reports / task mark-complete."),
    ("F-G31", "Notifications", "No type filter; no delete / archive; badge not refreshed on app resume."),
    ("F-G32", "Support / SLA", "No SLA badge in list; no attachments in reply UI; no bulk actions."),
    ("F-G33", "Warranties / DLP", "No DLP countdown chip; no expiry alert notifications."),
    ("F-G34", "Documents", "Legacy documents screen has delete-only actions; no upload progress indicator; no Office-doc preview."),
    ("F-G35", "Portal Users", "Role picker shows all roles; not project-scoped."),
    ("F-G36", "Password change", "After /reset-password no client-side relogin; user can still call APIs until token refresh fails."),
    ("F-G37", "BOQ list", "No GST split per item; no HSN/SAC display; subcontract retention not visible per row."),
    ("F-G38", "Subcontract list", "Retention only in detail; missing from card row in list view."),
    ("F-G39", "GPS", "No mock-location detection client-side for site reports / visits."),
    ("F-G40", "Evidence", "No client-side EXIF inspection / time-jump plausibility check."),
    ("F-G44", "BOQ Editor", "HSN/SAC inherit-on-material-select not wired in editor; estimator has to retype."),
    ("F-G45", "Concurrency UX", "Backend 409 on Final Account, AGREED rate, and BOQ approve lacks dedicated client banner / diff view in places."),
    ("F-G46", "Project Lifecycle", "Admin delete typed-confirmation dialog not enforced; risk of accidental tap-through on sensitive action."),
    ("F-G47", "Customer Lifecycle", "Toast text not differentiated for Delete vs Deactivate outcomes when server returns 200 in both cases."),
    ("F-G48", "Idempotency", "Outbox idempotency-key persistence across reboot relies on Drift column not yet confirmed for every mutation type."),
    ("F-G49", "Push routing", "STAGE_DUE / INVOICE_RAISED / FA_AGREED / LEAD_ASSIGNED deep-link table not exhaustive; fallback may surface as no-op."),
    ("F-G50", "Indian numerals", "[CLOSED] All NumberFormat.currency(symbol:'₹') call sites now pass locale:'en_IN' — lakh/crore grouping consistent across 11 screens (BOQ, FA, deductions, payment schedule, VO, stage payments, project detail, etc.)."),
]


# ---------------------------------------------------------------------------
# Rendering
# ---------------------------------------------------------------------------

def make_test_case_table(tc):
    construction = tc.get('construction', '—')
    gap = tc.get('flutter_gap', '—')
    rows = [
        [P('<b>ID</b>'),         P(tc['id']),
         P('<b>Screen</b>'),     P(tc['screen'])],
        [P('<b>Title</b>'),      P(tc['title']),
         P('<b>Role</b>'),       P(tc['role'])],
        [P('<b>Priority</b>'),   P(tc['priority']),
         P('<b>Type</b>'),       P(tc['type'])],
        [P('<b>Pre-conditions</b>'),
         Paragraph(tc['pre'], CELL), '', ''],
        [P('<b>Screen flow / Steps</b>'),
         Paragraph(steps_to_html(tc['steps']), CELL), '', ''],
        [P('<b>Expected UI behaviour</b>'),
         Paragraph(tc['expected'], CELL), '', ''],
        [P('<b>Construction context</b>'),
         P(construction),
         P('<b>Flutter gap</b>'),
         P(gap)],
    ]
    t = Table(rows, colWidths=[32*mm, 70*mm, 28*mm, 57*mm], hAlign='LEFT')
    t.setStyle(TableStyle([
        ('SPAN', (1, 3), (3, 3)),
        ('SPAN', (1, 4), (3, 4)),
        ('SPAN', (1, 5), (3, 5)),
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (0, -1), colors.HexColor('#EEF1F8')),
        ('BACKGROUND', (2, 0), (2, 2), colors.HexColor('#EEF1F8')),
        ('BACKGROUND', (2, 6), (2, 6), colors.HexColor('#EEF1F8')),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('FONTSIZE', (0, 0), (-1, -1), 8),
    ]))
    return KeepTogether([Spacer(1, 3), t, Spacer(1, 5)])


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont('Helvetica', 8)
    canvas.setFillColor(colors.HexColor('#666666'))
    canvas.drawString(15*mm, 10*mm,
                      "WallDot Flutter App — Screen Test Case Catalog (Confidential)")
    canvas.drawRightString(doc.pagesize[0]-15*mm, 10*mm,
                           f"Page {doc.page}")
    canvas.restoreState()


def build_cover(story):
    story.append(Spacer(1, 60))
    story.append(Paragraph(
        "WallDot Portal — Flutter Screen Test Catalog", COVER_T))
    story.append(Spacer(1, 8))
    story.append(Paragraph(
        "Production-grade, screen-by-screen test cases for the "
        "wd_portal_app_flutter application", COVER_S))
    story.append(Spacer(1, 18))
    story.append(Paragraph(
        "Residential · Commercial · Interior · Renovation construction "
        "scenarios in India, with explicit Flutter implementation gaps",
        COVER_S))
    story.append(Spacer(1, 30))
    meta = [
        ["Document", "Flutter Screen Test Case Catalog"],
        ["Target app", "wd_portal_app_flutter (Flutter, Dart, GoRouter)"],
        ["Companion", "PORTAL_TEST_CASES.pdf (API-side)"],
        ["Test cases", f"{len(TC)}"],
        ["Flutter gaps documented", f"{len(GAPS)}"],
        ["Generated on", date.today().isoformat()],
        ["Author / Reviewer", "Senior QA — Construction Tech (top 0.1%)"],
        ["Status", "Baseline v1.0 — for review with Flutter / QA / PM"],
    ]
    t = Table(meta, colWidths=[55*mm, 110*mm])
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
        "This catalogue is the Flutter-screen-centric companion to the API "
        "test suite. Every case is anchored to a concrete <i>.dart</i> file "
        "under <i>lib/screens</i> or <i>lib/features</i>, and the steps are "
        "written as a tester actually clicks them — tap this button, swipe "
        "this card, observe this widget. Coverage spans all four primary "
        "engagement types (Residential, Commercial, Interior, Renovation) "
        "plus the secondary Vastu and Smart-Home scopes, with explicit "
        "field-ops resilience (Outbox, geotagged photos, offline sync) "
        "and India-compliance UI checks.",
        BODY))
    story.append(Spacer(1, 4))
    story.append(Paragraph("Roles modelled", H2))
    roles = [
        ["Role", "Typical screens covered"],
        ["ADMIN", "ACL editor, portal users, partnerships, holidays, WBS templates"],
        ["DIRECTOR", "VO approvals, deduction decisions, Final Account agree"],
        ["COMMERCIAL_MANAGER / PM", "Project detail, Gantt, BOQ doc, CO, stage payments, FA"],
        ["FINANCE", "Invoices, payments, challan, payment schedule"],
        ["ESTIMATOR / SALES", "Leads, lead-estimation wizard, BOQ list"],
        ["SITE_ENGINEER / SUPERVISOR", "Site reports, site visits, mark-complete, attendance"],
        ["QUALITY_SAFETY (QS)", "Quality checks, observations"],
        ["PROCUREMENT", "Vendors, POs, quotations, GRN"],
        ["CUSTOMER (portal)", "Project view, BOQ acknowledge, support tickets"],
        ["PARTNER (Architect/Consultant)", "Referrals, partner dashboard"],
    ]
    t = Table(roles, colWidths=[55*mm, 110*mm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0B3D91')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t)
    story.append(Spacer(1, 6))

    story.append(Paragraph("2. Devices &amp; layouts", H2))
    story.append(Paragraph(
        "<b>Mobile</b> &lt; 850 px — bottom nav (Menu, Projects, Profile) + "
        "drawer for SideMenu.<br/>"
        "<b>Tablet</b> 850–1100 px — same drawer-based layout in portrait, "
        "side-menu in landscape.<br/>"
        "<b>Desktop / Web</b> ≥ 1100 px — SideMenu (250 px) + content "
        "(flex 5). DPC builder gets a split PDF preview at ≥ 1200 px.<br/>"
        "Test devices: Android 11+ (4 GB RAM mid-range plus a flagship), "
        "iOS 15+, Chromium / Safari current-2 versions. Network throttling: "
        "WiFi, 4G, 3G, offline.", BODY))

    story.append(Paragraph("3. Offline-sync model", H2))
    story.append(Paragraph(
        "Mobile and desktop builds use a Drift-backed OutboxService + "
        "SyncService stack (S5 PR1/PR2). Submissions of site reports, "
        "task mark-complete and delay logs are queued and drained when "
        "online. <b>Web builds skip the entire stack</b> (F-G30) — they "
        "fall back to direct API calls and have no offline capability. "
        "Most other screens (labour, inventory, procurement, finance, "
        "BOQ, payments) are API-only even on mobile.", BODY))

    story.append(Paragraph("4. Self-critique", H2))
    story.append(Paragraph(
        "Where this catalogue stops short:<br/>"
        "• Pixel-accurate visual regression (e.g. golden tests) is "
        "scoped separately — these cases are functional, not visual.<br/>"
        "• Deep Flutter framework concerns (widget rebuild profiling, "
        "key uniqueness, RepaintBoundary placement) need devtools work, "
        "not a tester click-through.<br/>"
        "• Native-platform edge cases (Android scoped storage, iOS "
        "background fetch, Web service worker) are listed at high "
        "level only; they need platform-specific test rigs.<br/>"
        "• Gaps marked F-G## are observed in code today. Several test "
        "cases <i>will fail</i> because they describe the production-"
        "grade UX the app should reach, not what ships now.",
        BODY))
    story.append(PageBreak())


def build_section_index(story):
    story.append(Paragraph("5. Screens covered", H1))
    seen = []
    for tc in TC:
        if tc['screen'] not in seen:
            seen.append(tc['screen'])
    rows = [[P('<b>Screen path</b>'), P('<b>Test cases</b>')]]
    for s in seen:
        count = sum(1 for t in TC if t['screen'] == s)
        rows.append([P(s), P(str(count))])
    table = Table(rows, colWidths=[140*mm, 25*mm], hAlign='LEFT')
    table.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0B3D91')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTSIZE', (0, 0), (-1, -1), 8),
    ]))
    story.append(table)
    story.append(PageBreak())


def build_test_cases(story):
    story.append(Paragraph("6. Test cases (by screen cluster)", H1))
    # Group by leading path segment
    clusters = []
    cluster_map = {}
    for tc in TC:
        p = tc['screen']
        if '/' in p:
            seg = '/'.join(p.split('/')[:3])
        else:
            seg = p
        cluster_map.setdefault(seg, []).append(tc)
        if seg not in clusters:
            clusters.append(seg)
    for c in clusters:
        story.append(Paragraph(c, H2))
        for tc in cluster_map[c]:
            story.append(make_test_case_table(tc))
        story.append(Spacer(1, 3))


def build_gaps(story):
    story.append(PageBreak())
    story.append(Paragraph("7. Flutter implementation gaps register", H1))
    story.append(Paragraph(
        "Each gap below was found while mapping the Flutter codebase for "
        "this catalogue and is referenced from the matching test cases via "
        "the <b>Flutter gap</b> cell. Backend-side gaps remain documented "
        "in the companion <i>PORTAL_TEST_CASES.pdf</i>.",
        BODY))
    story.append(Spacer(1, 4))
    rows = [[P('<b>Gap ID</b>'), P('<b>Area</b>'), P('<b>Description</b>')]]
    for g in GAPS:
        rows.append([P(g[0]), P(g[1]), P(g[2])])
    t = Table(rows, colWidths=[22*mm, 38*mm, 115*mm])
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
    story.append(Paragraph("8. Construction-type traceability", H1))
    types = {
        "Residential":  ["F-RES-001", "F-LEAD-003", "F-TSK-PROG-001", "F-SR-001", "F-LAB-001", "F-STG-002"],
        "Commercial":   ["F-COM-001", "F-WBS-003", "F-PRC-001", "F-BOQ-006", "F-IND-002"],
        "Interior":     ["F-INT-001", "F-LEAD-009", "F-DPC-001", "F-DPC-003"],
        "Renovation":   ["F-REN-001", "F-GAL-001", "F-DEL-001"],
        "Vastu":        ["F-LEAD-003"],
        "Smart Home":   ["F-SMH-001", "F-BOQ-005", "F-WAR-001"],
    }
    rows = [[P('<b>Construction type</b>'), P('<b>Key Flutter test cases</b>')]]
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
    story.append(Paragraph("9. Priority distribution", H2))
    p1 = sum(1 for t in TC if t['priority'] == 'P1')
    p2 = sum(1 for t in TC if t['priority'] == 'P2')
    p3 = sum(1 for t in TC if t['priority'] == 'P3')
    rows = [
        [P('<b>Priority</b>'), P('<b>Count</b>'), P('<b>Definition</b>')],
        [P('P1'), P(str(p1)), P('Blocker — financial, security, compliance or data-loss critical')],
        [P('P2'), P(str(p2)), P('High — major functional / workflow / construction-domain breakage')],
        [P('P3'), P(str(p3)), P('Medium — UX, localisation, peripheral feature')],
    ]
    t = Table(rows, colWidths=[25*mm, 25*mm, 115*mm])
    t.setStyle(TableStyle([
        ('GRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#888888')),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0B3D91')),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
    ]))
    story.append(t)


def build_doc(path):
    doc = SimpleDocTemplate(
        path, pagesize=landscape(A4),
        leftMargin=12*mm, rightMargin=12*mm,
        topMargin=14*mm, bottomMargin=15*mm,
        title="WallDot Flutter Test Case Catalog",
        author="Senior QA — Construction Tech",
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
    out = os.path.join(here, "PORTAL_FLUTTER_TEST_CASES.pdf")
    build_doc(out)
    print(f"Wrote {out}  ({len(TC)} test cases, {len(GAPS)} gaps)")
