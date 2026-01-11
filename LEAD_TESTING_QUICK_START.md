# Lead Module Testing - Quick Start Guide

**Date**: January 11, 2026  
**Time**: 11:44 AM IST  
**System**: http://localhost:8082

---

## Pre-Test Checklist

- ✅ API running on port 8081
- ✅ Flutter running on port 8082
- ✅ Login: admin@gmail.com / Test123$
- ✅ Validation enhanced (Backend DTO ✅, Controller ⚠️ manual edit, Frontend ⚠️ manual edit)

---

## Quick Test Sequence (15 minutes)

### Test 1: Create Lead - Happy Path (3 min)

1. **Navigate**: http://localhost:8082 → Login → Click "Leads"
2. **Click**: "Add New Lead"
3. **Fill Form**:
   ```
   Name: Rajesh Kumar
   Email: rajesh.test@example.com
   Phone: 9876543210
   WhatsApp: 9876543210
   Customer Type: Individual / Homeowner
   Lead Source: Website
   Priority: High
   State: Kerala
   District: Kottayam
   Location: Pala
   Address: 123 Main Street
   Project Type: Turnkey Project
   Budget: 5000000
   Project Sqft Area: 2500
   ```
4. **Click**: Save
5. **Expected**: ✅ Success message, lead appears in list

**✅ PASS / ❌ FAIL**: _________

---

### Test 2: Validation - Empty Name (1 min)

1. **Click**: "Add New Lead"
2. **Leave name empty**, fill other fields
3. **Click**: Save

**Expected**: ❌ "Name is required" error (shown in red under field)

**✅ PASS / ❌ FAIL**: _________

---

### Test 3: Validation - Invalid Email (1 min)

1. **Enter**: 
   - Name: Test User
   - Email: invalid-email (no @ symbol)
2. **Click**: Save

**Expected**: ❌ "Enter a valid email" error

**✅ PASS / ❌ FAIL**: _________

---

### Test 4: Validation - Short Phone (1 min)

1. **Enter**:
   - Name: Test User
   - Phone: 123 (only 3 digits)
2. **Click**: Save

**Expected**: ❌ "Phone number must be at least 10 digits" error

**✅ PASS / ❌ FAIL**: _________

---

### Test 5: Edit Lead (2 min)

1. **Find**: "Rajesh Kumar" in leads list
2. **Click**: Edit button (pencil icon)
3. **Change**: 
   - Phone: 9876543210 → 9999888877
   - Priority: High → Medium
4. **Click**: Save

**Expected**: ✅ Success message, changes reflected in list

**✅ PASS / ❌ FAIL**: _________

---

### Test 6: Search Lead (1 min)

1. **In leads list**, enter "Rajesh" in search box
2. **Press**: Enter

**Expected**: ✅ Only "Rajesh Kumar" lead shown

**✅ PASS / ❌ FAIL**: _________

---

### Test 7: Filter by Status (1 min)

1. **Click**: Status dropdown
2. **Select**: "New Inquiry"

**Expected**: ✅ Only leads with "New Inquiry" status shown

**✅ PASS / ❌ FAIL**: _________

---

### Test 8: Lead Scoring Verification (2 min)

**Check if high-budget lead gets "HOT" score**

1. **Create new lead**:
   - Name: High Value Client
   - Email: highvalue@example.com
   - Phone: 9988776655
   - Budget: 10000000 (10 million)
   - Lead Source: Referral
   - Fill other required fields

2. **Save and check lead details**

**Expected**: ✅ Score 60+ and category "HOT" (if visible in UI)

**✅ PASS / ❌ FAIL**: _________

---

### Test 9: Validation - State/District (1 min)

**Only if you applied the manual edits**

1. **Create lead**, leave State empty
2. **Click**: Save

**Expected**: ❌ "State is required" error

**✅ PASS / ❌ FAIL**: _________  
**Note**: If validation not applied, this will pass through

---

### Test 10: Delete Lead (2 min)

1. **Create test lead**: Name = "DELETE TEST"
2. **Click**: Delete button
3. **Confirm**: deletion

**Expected**: ✅ Lead removed from list

**✅ PASS / ❌ FAIL**: _________

---

## Database Verification (Optional)

After testing, verify in PostgreSQL:

```sql
-- Check created leads
SELECT lead_id, name, email, phone, lead_source, score, score_category, created_at
FROM leads 
WHERE email LIKE '%@example.com'
ORDER BY created_at DESC;

-- Expected: 2-3 test leads with correct data
```

---

## Test Results Summary

| Test | Status | Notes |
|------|--------|-------|
| 1. Create Happy Path | ⬜ | |
| 2. Empty Name Validation | ⬜ | |
| 3. Invalid Email | ⬜ | |
| 4. Short Phone | ⬜ | |
| 5. Edit Lead | ⬜ | |
| 6. Search | ⬜ | |
| 7. Filter | ⬜ | |
| 8. Lead Scoring | ⬜ | |
| 9. State/District Val | ⬜ | (manual edit dependent) |
| 10. Delete Lead | ⬜ | |

**Overall**: ___/10 Passed

---

## Issues Found

**List any bugs or issues**:
1. _______________________________________________________
2. _______________________________________________________
3. _______________________________________________________

---

## Cleanup (After Testing)

```sql
-- Delete test leads
DELETE FROM leads WHERE email LIKE '%@example.com';

-- Verify cleanup
SELECT COUNT(*) FROM leads WHERE email LIKE '%@example.com';
-- Should return: 0
```

---

## Next Steps

- [ ] Apply remaining manual edits (MANUAL_EDITS_REQUIRED.md)
- [ ] Fix any issues found
- [ ] Test other modules (Customers, Projects, etc.)

---

**Tested By**: _________________  
**Date**: January 11, 2026  
**Time Taken**: _______ minutes  
**Overall Status**: ✅ PASS / ⚠️ ISSUES / ❌ FAIL

