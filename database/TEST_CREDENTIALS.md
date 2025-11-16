# Test Credentials for Partnership System

## Password for All Test Accounts
**Password:** `Test@123`

**BCrypt Hash:** `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

---

## Test Accounts

### 1. **Architectural Firm** (✅ Active - Can Login)
- **Phone:** `9876543210`
- **Email:** `architect@testfirm.com`
- **Name:** Rajesh Kumar
- **Designation:** Principal Architect
- **Firm:** Kumar & Associates Architects
- **Status:** ✅ Active

---

### 2. **Real Estate Agent** (⏳ Approved - First Login Pending)
- **Phone:** `9876543211`
- **Email:** `realestate@properties.com`
- **Name:** Priya Menon
- **Designation:** Senior Real Estate Consultant
- **Firm:** Kerala Premium Properties
- **Status:** ⏳ Approved

---

### 3. **Interior Designer** (✅ Active - Can Login)
- **Phone:** `9876543212`
- **Email:** `interior@designs.com`
- **Name:** Anil Thomas
- **Designation:** Lead Interior Designer
- **Firm:** Creative Interiors Studio
- **Status:** ✅ Active

---

### 4. **Financial Institution** (🕒 Pending Approval)
- **Phone:** `9876543213`
- **Email:** `loans@keralbank.com`
- **Name:** Suresh Pillai
- **Designation:** Branch Manager - Home Loans
- **Company:** Kerala Bank Ltd.
- **Status:** 🕒 Pending

---

### 5. **Material Supplier** (✅ Active - Can Login)
- **Phone:** `9876543214`
- **Email:** `sales@buildmaterials.com`
- **Name:** Mohammed Ali
- **Designation:** Sales Director
- **Company:** BuildPro Materials Pvt Ltd
- **Status:** ✅ Active

---

### 6. **Vastu Consultant** (✅ Active - Can Login)
- **Phone:** `9876543215`
- **Email:** `vastu@consultant.com`
- **Name:** Dr. Lakshmi Nair
- **Designation:** Chief Vastu Consultant
- **Business:** Vedic Vastu Consultancy
- **Status:** ✅ Active

---

### 7. **Land Consultant** (⏳ Approved - First Login Pending)
- **Phone:** `9876543216`
- **Email:** `land@consultancy.com`
- **Name:** Vineeth Krishnan
- **Designation:** Senior Land Consultant
- **Firm:** Kerala Land Experts
- **Status:** ⏳ Approved

---

### 8. **Corporate Partner** (🕒 Pending Approval)
- **Phone:** `9876543217`
- **Email:** `projects@techcorp.com`
- **Name:** Amit Sharma
- **Designation:** Head of Administration
- **Company:** Tech Solutions India Pvt Ltd
- **Status:** 🕒 Pending

---

### 9. **Suspended Account** (❌ Suspended - Cannot Login)
- **Phone:** `9876543218`
- **Email:** `suspended@architect.com`
- **Name:** Test Suspended User
- **Designation:** Architect
- **Firm:** Suspended Architecture Firm
- **Status:** ❌ Suspended

---

### 10. **Premium Real Estate** (✅ Active - Can Login)
- **Phone:** `9876543219`
- **Email:** `premium@realestate.com`
- **Name:** Sarah Joseph
- **Designation:** Managing Partner
- **Firm:** Premium Realty Group
- **Status:** ✅ Active

---

## Quick Setup Commands

### 1. Insert Sample Data
```bash
cd wd-api
psql -U your_username -d your_database -f database/sample_partnership_data.sql
```

### 2. View All Test Accounts
```sql
SELECT 
    id, 
    full_name, 
    phone, 
    email, 
    partnership_type, 
    COALESCE(firm_name, company_name, business_name) as organization,
    status 
FROM partnership_users 
ORDER BY id;
```

### 3. Check Statistics
```sql
SELECT 
    partnership_type,
    status,
    COUNT(*) as count
FROM partnership_users
GROUP BY partnership_type, status
ORDER BY partnership_type, status;
```

---

## Testing Login API

### Test with cURL

#### Active Account (Should Work)
```bash
curl -X POST http://localhost:8080/api/partnerships/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "password": "Test@123"
  }'
```

#### Approved Account (Should Work - Status will change to Active)
```bash
curl -X POST http://localhost:8080/api/partnerships/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543211",
    "password": "Test@123"
  }'
```

#### Pending Account (Should Fail)
```bash
curl -X POST http://localhost:8080/api/partnerships/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543213",
    "password": "Test@123"
  }'
```

Expected Error: `"Account is not active. Status: pending"`

#### Suspended Account (Should Fail)
```bash
curl -X POST http://localhost:8080/api/partnerships/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543218",
    "password": "Test@123"
  }'
```

Expected Error: `"Account is not active. Status: suspended"`

---

## Testing Dashboard Access

### 1. Login and Save Token
```bash
# Login
RESPONSE=$(curl -s -X POST http://localhost:8080/api/partnerships/login \
  -H "Content-Type: application/json" \
  -d '{"phone": "9876543210", "password": "Test@123"}')

# Extract token (requires jq)
TOKEN=$(echo $RESPONSE | jq -r '.token')
echo "Token: $TOKEN"
```

### 2. Verify Token
```bash
curl -X GET http://localhost:8080/api/partnerships/verify \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Get Dashboard Stats
```bash
curl -X GET http://localhost:8080/api/partnerships/stats \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Get Referrals
```bash
curl -X GET http://localhost:8080/api/partnerships/referrals \
  -H "Authorization: Bearer $TOKEN"
```

---

## Testing Frontend

### Partner Login Page
```
http://localhost:3000/partnerships/login
```

**Test Logins:**
- Phone: `9876543210` | Password: `Test@123` ✅ Should work
- Phone: `9876543212` | Password: `Test@123` ✅ Should work
- Phone: `9876543213` | Password: `Test@123` ❌ Should fail (pending)
- Phone: `9876543218` | Password: `Test@123` ❌ Should fail (suspended)

---

## Admin Operations

### Approve Pending Applications
```sql
-- Approve Financial Institution
UPDATE partnership_users 
SET status = 'approved', 
    approved_at = CURRENT_TIMESTAMP,
    updated_by = 'admin'
WHERE phone = '9876543213';

-- Approve Corporate Partner
UPDATE partnership_users 
SET status = 'approved', 
    approved_at = CURRENT_TIMESTAMP,
    updated_by = 'admin'
WHERE phone = '9876543217';
```

### Activate Suspended Account
```sql
UPDATE partnership_users 
SET status = 'active',
    updated_by = 'admin'
WHERE phone = '9876543218';
```

### Suspend Active Account
```sql
UPDATE partnership_users 
SET status = 'suspended',
    updated_by = 'admin'
WHERE phone = '9876543210';
```

---

## Cleanup Test Data

```sql
-- Delete all test data
DELETE FROM partnership_users 
WHERE phone IN (
    '9876543210', '9876543211', '9876543212', '9876543213', '9876543214',
    '9876543215', '9876543216', '9876543217', '9876543218', '9876543219'
);

-- Verify deletion
SELECT COUNT(*) FROM partnership_users;
```

---

## Notes

- All test accounts use the same password: `Test@123`
- BCrypt hash is pre-computed for faster testing
- "Active" status accounts can login immediately
- "Approved" status accounts can login, and status will change to "active" on first login
- "Pending" status accounts cannot login until approved by admin
- "Suspended" status accounts cannot login
- Token expires after 24 hours (86400000 milliseconds)

---

## Security Reminder for Production

⚠️ **IMPORTANT:** 
- Never use these test credentials in production
- Always use strong, unique passwords
- Implement proper password reset flow
- Add rate limiting for login attempts
- Enable 2FA for sensitive partnerships
- Regularly audit partner accounts

