# Lead Module - Manual Testing Execution Guide

**Tester**: Manual Execution Required  
**Date**: January 10, 2026  
**Browser**: Chrome/Firefox/Edge  
**Application**: http://localhost:8082

---

## PRE-TEST SETUP

### 1. Verify Services Running:
```powershell
# Check API (should return 401 Unauthorized - means it's running)
curl http://localhost:8081/api/leads

# Check Flutter (should load login page)
# Open browser: http://localhost:8082
```

### 2. Login Credentials:
- **Email**: admin@gmail.com
- **Password**: Test123$

---

## TEST CASE 1: Create New Lead (Happy Path) ✅

### Execution Steps:

1. **Navigate to Leads Module**
   - [ ] Open http://localhost:8082
   - [ ] Login with admin@gmail.com / Test123$
   - [ ] Click "Leads" in sidebar
   - [ ] Take screenshot (name: `01_leads_list_initial.png`)

2. **Open Add Lead Form**
   - [ ] Click "Add New Lead" button
   - [ ] Verify form loads with sections:
     - Basic Information
     - Location Details  
     - Project Information
   - [ ] Take screenshot (name: `02_add_lead_form.png`)

3. **Fill Form - Basic Information**
   ```
   Full Name: Rajesh Kumar
   Email: rajesh.kumar.test@example.com
   Phone: 9876543210
   WhatsApp: 9876543210
   Customer Type: Individual / Homeowner (or first option)
   Lead Source: Website
   Priority: High
   Status: New Inquiry (default)
   ```

4. **Fill Form - Location Details**
   ```
   State: Kerala
   District: Kottayam
   Location: Pala
   Address: 123 Main Street, Pala, Kerala
   ```

5. **Fill Form - Project Information**
   ```
   Project Type: Turnkey Project (or first option)
   Project Description: "New 3BHK residential house"
   Budget: 5000000
   Project Sqft Area: 2500
   Date of Enquiry: Select today's date
   ```

6. **Submit Form**
   - [ ] Click "Save" or "Submit" button
   - [ ] Wait for response
   - [ ] Verify success message appears
   - [ ] Take screenshot (name: `03_lead_created_success.png`)

7. **Verify in List**
   - [ ] Return to Leads list
   - [ ] Search for "Rajesh"
   - [ ] Verify new lead appears
   - [ ] Take screenshot (name: `04_lead_in_list.png`)

### Database Verification:
```sql
-- Run in PostgreSQL
SELECT 
    lead_id, name, email, phone, customer_type, 
    lead_source, lead_status, priority, budget,
    score, score_category, created_at
FROM leads 
WHERE email = 'rajesh.kumar.test@example.com';
```

### Expected Results:
- ✅ Form validates and accepts all data
- ✅ Success message: "Lead created successfully"
- ✅ Lead appears in list with correct details
- ✅ Database record created
- ✅ Score calculated (should be ~50-70 for this lead)
- ✅ Score category: "WARM" or "HOT" (due to high budget)

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
Issues: None / [List any issues]
Screenshot: [Confirm screenshots taken]
Database Check: [Confirm record exists]
```

---

## TEST CASE 2: Create Lead with Missing Required Field ❌

### Execution Steps:

1. **Open Add Lead Form**
   - [ ] Click "Add New Lead"

2. **Leave Name Field Empty**
   - [ ] Enter email: empty.test@example.com
   - [ ] Enter phone: 1234567890
   - [ ] Fill other fields
   - [ ] Leave "Full Name" empty

3. **Attempt Submit**
   - [ ] Click Save
   - [ ] Take screenshot (name: `05_validation_error.png`)

### Expected Results:
- ❌ Validation error displayed: "Name is required"
- ❌ Form does not submit
- ✅ User stays on form screen
- ❌ No database record created

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## TEST CASE 3: Edit Existing Lead ✏️

### Execution Steps:

1. **Find Lead**
   - [ ] Go to Leads list
   - [ ] Find "Rajesh Kumar" lead
   - [ ] Click Edit button/icon
   - [ ] Take screenshot (name: `06_edit_lead_form.png`)

2. **Modify Fields**
   - [ ] Change Phone: 9876543210 → 9999888877
   - [ ] Change Priority: High → Medium
   - [ ] Change Status: New Inquiry → Contacted
   - [ ] Add note: "Follow-up completed on [date]"

3. **Save Changes**
   - [ ] Click Save
   - [ ] Verify success message
   - [ ] Take screenshot (name: `07_lead_updated.png`)

### Database Verification:
```sql
SELECT phone, priority, lead_status, notes, updated_at 
FROM leads 
WHERE email = 'rajesh.kumar.test@example.com';
```

### Expected Results:
- ✅ Form pre-populates with existing data
- ✅ Changes save successfully
- ✅ Success message appears
- ✅ List reflects updated data
- ✅ `updated_at` timestamp changes

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## TEST CASE 4: Search Functionality 🔍

### Execution Steps:

1. **Search by Name**
   - [ ] In Leads list, enter "Rajesh" in search box
   - [ ] Press Enter or click Search
   - [ ] Take screenshot (name: `08_search_by_name.png`)

2. **Search by Email**
   - [ ] Clear search
   - [ ] Enter "rajesh.kumar.test"
   - [ ] Verify results

3. **Search by Phone**
   - [ ] Clear search
   - [ ] Enter "9999888877"
   - [ ] Verify results

### Expected Results:
- ✅ Search is case-insensitive
- ✅ Searches across name, email, phone
- ✅ Results update in real-time or on Enter
- ✅ Only matching leads shown

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## TEST CASE 5: Filter by Status 🎯

### Execution Steps:

1. **Create Test Leads with Different Statuses**
   - [ ] Create "Test Lead 1" with Status: New Inquiry
   - [ ] Create "Test Lead 2" with Status: Contacted
   - [ ] Create "Test Lead 3" with Status: Qualified

2. **Test Status Filter**
   - [ ] Select "New Inquiry" from status dropdown
   - [ ] Verify only "New Inquiry" leads shown
   - [ ] Take screenshot (name: `09_filter_new_inquiry.png`)
   
   - [ ] Select "Contacted"
   - [ ] Verify only "Contacted" leads shown
   
   - [ ] Select "All" or clear filter
   - [ ] Verify all leads shown

### Expected Results:
- ✅ Filter dropdown populated with statuses
- ✅ Filtering works correctly
- ✅ Results update immediately

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## TEST CASE 6: Lead Assignment 👤

### Execution Steps:

1. **Edit Lead**
   - [ ] Select "Rajesh Kumar" lead
   - [ ] Click Edit

2. **Assign to User**
   - [ ] Find "Assigned To" dropdown
   - [ ] Select a Portal User (if available)
   - [ ] Save

3. **Verify Assignment**
   - [ ] Check lead list shows assigned user
   - [ ] Take screenshot (name: `10_lead_assigned.png`)

### Database Verification:
```sql
SELECT 
    l.name, l.assigned_team, l.assigned_to_id,
    pu.first_name, pu.last_name
FROM leads l
LEFT JOIN portal_users pu ON l.assigned_to_id = pu.user_id
WHERE l.email = 'rajesh.kumar.test@example.com';
```

### Expected Results:
- ✅ Dropdown shows available users
- ✅ Assignment saves successfully
- ✅ `assigned_team` field updated
- ✅ Activity log shows assignment

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## TEST CASE 7: Lead Scoring Verification 📊

### Create Leads with Different Budgets:

1. **High Value Lead**
   ```
   Name: High Value Client
   Email: high.value@example.com
   Budget: 10000000 (10M)
   Source: Referral
   ```
   **Expected Score Category**: HOT (60+ points)

2. **Medium Lead**
   ```
   Name: Medium Client  
   Email: medium.value@example.com
   Budget: 3000000 (3M)
   Source: Website
   ```
   **Expected Score Category**: WARM (31-60 points)

3. **Low Lead**
   ```
   Name: Low Budget Client
   Email: low.budget@example.com
   Budget: 500000 (500K)
   Source: Cold Call
   ```
   **Expected Score Category**: COLD (0-30 points)

### Verification:
```sql
SELECT name, budget, lead_source, score, score_category 
FROM leads 
WHERE email IN ('high.value@example.com', 'medium.value@example.com', 'low.budget@example.com')
ORDER BY score DESC;
```

### Expected Results:
- ✅ High value lead: Score 60+, Category HOT
- ✅ Medium lead: Score 31-60, Category WARM  
- ✅ Low lead: Score 0-30, Category COLD
- ✅ Scores calculated automatically

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## TEST CASE 8: Lead Conversion to Project 🚀

### Prerequisites:
- At least one qualified lead exists
- User has PROJECT_MANAGER or ADMIN role

### Execution Steps:

1. **Select Lead**
   - [ ] Find "Rajesh Kumar" lead
   - [ ] Click "Convert to Project" button (if visible)
   OR navigate to lead details and find conversion option

2. **Fill Conversion Form**
   ```
   Project Name: Kumar Residence  
   Project Manager: [Select from dropdown]
   Start Date: [Today's date]
   Location: Pala, Kottayam
   ```

3. **Submit Conversion**
   - [ ] Click Convert/Submit
   - [ ] Wait for response
   - [ ] Take screenshot (name: `11_conversion_success.png`)

### Database Verification:
```sql
-- Check project created
SELECT * FROM customer_projects 
WHERE lead_id = (SELECT lead_id FROM leads WHERE email = 'rajesh.kumar.test@example.com');

-- Check customer user created
SELECT * FROM customer_users 
WHERE email = 'rajesh.kumar.test@example.com';

-- Check lead status updated
SELECT lead_status, converted_at, converted_by_id 
FROM leads 
WHERE email = 'rajesh.kumar.test@example.com';
```

### Expected Results:
- ✅ New project created with project code (e.g., PRJ-2026-0001)
- ✅ Customer user account created
- ✅ Lead status changed to "WON"
- ✅ Success message with project details
- ✅ Documents migrated (if any existed)
- ✅ Activity log shows conversion

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
Project Created: YES / NO
Project Code: _______
Customer Account: YES / NO
```

---

## TEST CASE 9: Pagination 📄

### Prerequisites:
- At least 15-20 leads exist (create more test leads if needed)

### Execution Steps:

1. **Check Pagination Controls**
   - [ ] Go to Leads list
   - [ ] Verify pagination controls at bottom
   - [ ] Note total count and current page

2. **Navigate Pages**
   - [ ] Click "Next" or page 2
   - [ ] Verify different leads shown
   - [ ] Click "Previous"
   - [ ] Verify returns to page 1
   - [ ] Take screenshot (name: `12_pagination.png`)

### Expected Results:
- ✅ Page size limit respected (typically 10-20 per page)
- ✅ Navigation works (Next, Previous, Page numbers)
- ✅ Total count accurate
- ✅ Current page indicator correct

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
Total Leads: _____
Leads Per Page: _____
```

---

## TEST CASE 10: Sort Functionality ⬆️⬇️

### Execution Steps:

1. **Sort by Name**
   - [ ] Click "Name" column header
   - [ ] Verify ascending order (A-Z)
   - [ ] Click again
   - [ ] Verify descending order (Z-A)

2. **Sort by Date**
   - [ ] Click "Created At" or "Date" column
   - [ ] Verify newest first
   - [ ] Click again
   - [ ] Verify oldest first

3. **Sort by Budget**
   - [ ] Click "Budget" column (if visible)
   - [ ] Verify high to low
   - [ ] Take screenshot (name: `13_sorting.png`)

### Expected Results:
- ✅ Sorting indicator shows active column
- ✅ Data sorts correctly
- ✅ Sort persists during session

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## TEST CASE 11: Delete Lead 🗑️

### Execution Steps:

1. **Create Test Lead**
   - [ ] Create lead: "Delete Test Lead"
   - [ ] Email: delete.test@example.com
   - [ ] Note the lead_id

2. **Delete Lead**
   - [ ] Find "Delete Test Lead"
   - [ ] Click Delete button/icon
   - [ ] Verify confirmation dialog appears
   - [ ] Take screenshot (name: `14_delete_confirmation.png`)
   - [ ] Confirm deletion

3. **Verify Deletion**
   - [ ] Check lead removed from list
   - [ ] Search for "Delete Test" - should return no results

### Database Verification:
```sql
SELECT * FROM leads WHERE email = 'delete.test@example.com';
-- Should return 0 rows
```

### Expected Results:
- ✅ Confirmation dialog appears
- ✅ Lead deleted from database
- ✅ Lead removed from list
- ✅ Success message shown

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## TEST CASE 12: Lead Analytics Dashboard 📈

### Execution Steps:

1. **Navigate to Analytics**
   - [ ] Find "Analytics" or "Dashboard" section for Leads
   - [ ] Take screenshot (name: `15_analytics_dashboard.png`)

2. **Verify Metrics**
   - [ ] Total Leads count
   - [ ] Status distribution chart
   - [ ] Source distribution chart
   - [ ] Conversion rate
   - [ ] Priority distribution

### Expected Results:
- ✅ All metrics display correctly
- ✅ Charts render without errors
- ✅ Numbers match database counts

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
Total Leads Shown: _____
Conversion Rate: _____%
```

---

## TEST CASE 13: Follow-Up Management 📅

### Execution Steps:

1. **Create Lead with Future Follow-Up**
   - [ ] Create lead: "Future Follow-Up"
   - [ ] Set Next Follow-Up: Tomorrow's date
   - [ ] Save

2. **Create Lead with Past Follow-Up**
   - [ ] Create lead: "Overdue Follow-Up"
   - [ ] Set Next Follow-Up: Yesterday's date
   - [ ] Save

3. **Check Overdue Follow-Ups**
   - [ ] Navigate to Dashboard or Follow-Ups section
   - [ ] Verify overdue lead appears with indicator
   - [ ] Take screenshot (name: `16_overdue_followups.png`)

### Database Verification:
```sql
SELECT name, next_follow_up, lead_status
FROM leads 
WHERE next_follow_up < NOW() 
AND lead_status NOT IN ('won', 'lost')
ORDER BY next_follow_up;
```

### Expected Results:
- ✅ Overdue leads highlighted
- ✅ List shows all overdue follow-ups
- ✅ Visual indicator (badge, color) present

### Actual Results:
```
[MANUAL TESTER: Fill in results here]
Status: PASS / FAIL
```

---

## POST-TEST CLEANUP

### Database Cleanup:
```sql
-- Delete all test leads
DELETE FROM leads WHERE email LIKE '%@example.com' OR email LIKE '%.test@%';

-- Delete test projects (if created during conversion test)
DELETE FROM customer_projects WHERE name LIKE '%Test%' OR name LIKE '%Kumar Residence%';

-- Delete test customer users
DELETE FROM customer_users WHERE email LIKE '%@example.com' OR email LIKE '%.test@%';

-- Verify cleanup
SELECT COUNT(*) FROM leads WHERE email LIKE '%@example.com';
```

---

## SUMMARY REPORT

### Test Results:

| Test Case | Status | Issues Found |
|-----------|--------|--------------|
| TC1: Create Lead (Happy Path) | ⬜ PASS / FAIL | |
| TC2: Missing Required Field | ⬜ PASS / FAIL | |
| TC3: Edit Lead | ⬜ PASS / FAIL | |
| TC4: Search | ⬜ PASS / FAIL | |
| TC5: Filter by Status | ⬜ PASS / FAIL | |
| TC6: Lead Assignment | ⬜ PASS / FAIL | |
| TC7: Lead Scoring | ⬜ PASS / FAIL | |
| TC8: Conversion | ⬜ PASS / FAIL | |
| TC9: Pagination | ⬜ PASS / FAIL | |
| TC10: Sorting | ⬜ PASS / FAIL | |
| TC11: Delete | ⬜ PASS / FAIL | |
| TC12: Analytics | ⬜ PASS / FAIL | |
| TC13: Follow-Ups | ⬜ PASS / FAIL | |

### Issues Log:
```
[List all bugs/issues found during testing]

Example:
1. [CRITICAL] Form validation not working for email field
2. [MEDIUM] Sorting by budget column causes error
3. [LOW] Success message disappears too quickly
```

### Browser Compatibility:
- [ ] Chrome - Tested
- [ ] Firefox - Tested
- [ ] Edge - Tested  
- [ ] Safari - Tested

### Overall Assessment:
```
Total Test Cases: 13
Passed: ___
Failed: ___
Blocked: ___

Overall Status: ✅ READY / ❌ ISSUES FOUND / ⚠️ NEEDS REVIEW

Sign-Off:
Tester Name: _________________
Date: _________________
Signature: _________________
```

---

**End of Manual Testing Guide**
