# Lead Module - Comprehensive Test Plan & Manual Testing Guide

## Test Environment
- **API**: http://localhost:8081
- **Frontend**: http://localhost:8082
- **Login**: admin@gmail.com / Test123$

---

## Test Case 1: Create New Lead (Happy Path)

### Steps:
1. Navigate to **Leads** module
2. Click **"Add New Lead"**
3. Fill in the form:
   - **Full Name**: Rajesh Kumar
   - **Email**: rajesh.kumar@example.com
   - **Phone**: 9876543210
   - **WhatsApp**: 9876543210
   - **Customer Type**: Individual / Homeowner
   - **Lead Source**: Website
   - **Priority**: High
   - **State**: Kerala  
   - **District**: Kottayam
   - **Location**: Pala
   - **Address**: 123 Main Street, Pala, Kerala
   - **Project Type**: Turnkey Project
   - **Budget**: 5000000
   - **Project Sqft Area**: 2500
   - **Date of Enquiry**: Today's date

### Expected Results:
- ✅ Form validates all required fields
- ✅ Success message appears after save
- ✅ Lead appears in the leads list
- ✅ Lead score is automatically calculated
- ✅ Activity feed logs "LEAD_CREATED" event
- ✅ Welcome email is sent (check console logs)

### Database Verification:
```sql
SELECT * FROM leads WHERE email = 'rajesh.kumar@example.com';
-- Verify: name, phone, customerType, projectType, budget, score fields
```

---

## Test Case 2: Create Lead (Missing Required Fields)

### Steps:
1. Click "Add New Lead"
2. Leave **Name** field empty
3. Fill other fields
4. Click Save

### Expected Results:
- ❌ Validation error: "Name is required"
- ❌ Form does not submit
- ✅ User stays on the form screen

---

## Test Case 3: Edit Existing Lead

### Steps:
1. From Leads list, find "Rajesh Kumar"
2. Click **Edit** button
3. Modify:
   - Phone: 9999888877
   - Priority: Medium → High
   - Status: New Inquiry → Contacted
4. Click **Save**

### Expected Results:
- ✅ Success message appears
- ✅ Changes are reflected in the list
- ✅ Activity feed logs "LEAD_UPDATED" and "LEAD_STATUS_CHANGED"
- ✅ Status change email is sent (check logs)

### Database Verification:
```sql
SELECT phone, priority, lead_status, updated_at 
FROM leads 
WHERE email = 'rajesh.kumar@example.com';
```

---

## Test Case 4: Search Functionality

### Steps:
1. Go to Leads list
2. Enter "Rajesh" in search box
3. Observe results

### Expected Results:
- ✅ Only matching leads are shown
- ✅ Search is case-insensitive
- ✅ Searches across name, email, and phone fields

---

## Test Case 5: Lead Assignment

### Steps:
1. Edit a lead
2. Assign to a Portal User (select from dropdown)
3. Save

### Expected Results:
- ✅ Lead shows assigned user in list
- ✅ "Assigned Team" field is updated
- ✅ Activity log shows "LEAD_ASSIGNED" event

---

## Test Case 6: Lead Scoring

### Test Data:
Create leads with different characteristics:

| Name | Budget | Source | Expected Score Category |
|------|--------|--------|------------------------|
| High Value Lead | 10000000 | Referral | HOT |
| Medium Lead | 3000000 | Website | WARM |
| Low Lead | 500000 | Cold Call | COLD |

### Expected Results:
- ✅ Score is calculated automatically
- ✅ Score category (COLD/WARM/HOT) is assigned
- ✅ Hot leads trigger admin alert email

### Database Verification:
```sql
SELECT name, budget, lead_source, score, score_category 
FROM leads 
ORDER BY score DESC;
```

---

## Test Case 7: Filter by Status

### Steps:
1. Create leads with different statuses:
   - new_inquiry
   - contacted
   - qualified
   - proposal_sent
   - won
   - lost
2. Use status filter dropdown

### Expected Results:
- ✅ Only leads with selected status are shown
- ✅ Filter persists during session

---

## Test Case 8: Lead Conversion to Project

### Steps:
1. Select a qualified lead
2. Click "Convert to Project"
3. Fill conversion form:
   - Project Name: "Kumar Residence"
   - Project Manager: Select from dropdown
   - Start Date: Select date
4. Submit

### Expected Results:
- ✅ New project is created
- ✅ Lead status changes to "WON"
- ✅ Customer user account is created
- ✅ Welcome email sent to customer
- ✅ Activity logs show conversion
- ✅ Documents migrate from lead to project

### Database Verification:
```sql
-- Check project creation
SELECT * FROM customer_projects WHERE lead_id = (
    SELECT lead_id FROM leads WHERE email = 'rajesh.kumar@example.com'
);

-- Check customer user creation
SELECT * FROM customer_users WHERE email = 'rajesh.kumar@example.com';

-- Verify lead status
SELECT lead_status, converted_at, converted_by_id 
FROM leads 
WHERE email = 'rajesh.kumar@example.com';
```

---

## Test Case 9: Overdue Follow-Ups

### Steps:
1. Create a lead with Next Follow-Up date in the past
2. Navigate to Dashboard or Follow-Up section

### Expected Results:
- ✅ Lead appears in "Overdue Follow-Ups" list
- ✅ Visual indicator (red badge/highlight)

---

## Test Case 10: Delete Lead

### Steps:
1. Select a test lead
2. Click Delete
3. Confirm deletion

### Expected Results:
- ✅ Confirmation dialog appears
- ✅ Lead is removed from database
- ✅ Activity log shows deletion

### Database Verification:
```sql
SELECT * FROM leads WHERE lead_id = [deleted_lead_id];
-- Should return no rows
```

---

## Test Case 11: Lead Analytics

### Steps:
1. Navigate to Lead Analytics/Dashboard
2. View metrics

### Expected Results:
- ✅ Total leads count is accurate
- ✅ Status distribution shows correct counts
- ✅ Source distribution is correct
- ✅ Conversion rate is calculated properly
- ✅ Charts/graphs render without errors

---

## Test Case 12: Pagination

### Steps:
1. If less than 20 leads exist, create more test leads
2. Navigate through pages

### Expected Results:
- ✅ Page size limit is respected (10-20 per page)
- ✅ Page navigation works (Next, Previous, Page numbers)
- ✅ Total count is displayed correctly

---

## Test Case 13: Sort Functionality

### Steps:
1. Click column headers to sort:
   - Name (A-Z, Z-A)
   - Date Created (Newest, Oldest)
   - Budget (High to Low, Low to High)
   - Score (High to Low)

### Expected Results:
- ✅ Data sorts correctly
- ✅ Sort indicator shows active column
- ✅ Sort persists during session

---

## Common Issues to Check

### Frontend Issues:
- [ ] Form validation triggers properly
- [ ] Dropdowns populate with correct options
- [ ] Date pickers work correctly
- [ ] Field masking (phone numbers) works
- [ ] Error messages are clear and helpful
- [ ] Success messages appear and auto-dismiss
- [ ] Loading states show during API calls

### Backend Issues:
- [ ] Null pointer exceptions on missing fields
- [ ] Enum value mismatches
- [ ] Duplicate email validation
- [ ] Transaction rollback on errors
- [ ] Email sending failures don't block operations
- [ ] Activity feed logging doesn't cause failures

### Data Integrity:
- [ ] Foreign key relationships (assignedTo)
- [ ] Audit fields (createdAt, updatedAt) populate
- [ ] Score calculation handles null values
- [ ] Budget accepts decimal values
- [ ] Date fields parse correctly

---

## API Endpoint Testing (Manual with Authentication)

### Get All Leads:
```http
GET http://localhost:8081/api/leads
Authorization: Bearer [token]
```

### Create Lead:
```http
POST http://localhost:8081/api/leads
Authorization: Bearer [token]
Content-Type: application/json

{
  "name": "Test Lead",
  "email": "test@example.com",
  "phone": "1234567890",
  "customerType": "individual",
  "leadSource": "website",
  "projectType": "residential"
}
```

### Update Lead:
```http
PUT http://localhost:8081/api/leads/{id}
Authorization: Bearer [token]
Content-Type: application/json

{
  "name": "Updated Name",
  "leadStatus": "contacted"
}
```

### Get Lead by ID:
```http
GET http://localhost:8081/api/leads/{id}
Authorization: Bearer [token]
```

---

## Performance Testing

### Load Test:
1. Create 100+ leads
2. Test list performance
3. Test search performance
4. Test filtering performance

### Expected:
- ✅ List loads in < 2 seconds
- ✅ Search responds in < 500ms
- ✅ Filter updates in < 500ms

---

## Regression Testing Checklist

After any code changes to Lead module:
- [ ] Re-run all test cases above
- [ ] Verify no console errors
- [ ] Check API logs for exceptions
- [ ] Verify email functionality
- [ ] Test on different browsers (Chrome, Firefox, Edge)
- [ ] Test responsive design (mobile, tablet, desktop)

---

## Known Issues / Bugs to Fix

*(Document any issues found during testing)*

### Example:
- [ ] **Issue**: Date picker doesn't work on mobile Safari
  - **Severity**: Medium
  - **Fix**: Use native date input for mobile

---

## Test Data Cleanup

After testing:
```sql
-- Delete test leads
DELETE FROM leads WHERE email LIKE '%@example.com';

-- Delete test projects created from leads
DELETE FROM customer_projects WHERE name LIKE '%Test%';

-- Verify cleanup
SELECT COUNT(*) FROM leads WHERE email LIKE '%@example.com';
```

---

## Sign-Off

| Role | Name | Date | Status |
|------|------|------|--------|
| QA Engineer | [Name] | [Date] | ✅ PASS / ❌ FAIL |
| Developer | [Name] | [Date] | ✅ Code Review Complete |
| Product Owner | [Name] | [Date] | ✅ Acceptance Complete |

---

**Last Updated**: January 10, 2026
**Test Environment**: Development
**Status**: Ready for Testing
