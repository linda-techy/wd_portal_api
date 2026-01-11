# Lead Module - Frontend + Backend Validation Enhancement

## Overview
This document describes the comprehensive validation implementation for the Lead module, ensuring data integrity before API calls (frontend validation) and at API level (backend validation).

---

## Backend Validation (API Side)

### File Modified: `LeadCreateRequest.java`

**Status**: ✅ ALREADY APPLIED

Added Jakarta Bean Validation annotations:

```java
import jakarta.validation.constraints.*;

public class LeadCreateRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phone;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "WhatsApp number must be 10-15 digits")
    private String whatsappNumber;

    @NotBlank(message = "Customer type is required")
    private String customerType;

    @NotBlank(message = "Lead status is required")
    private String leadStatus;

    @NotBlank(message = "Lead source is required")
   private String leadSource;

    @NotBlank(message = "Priority is required")
    private String priority;

    @NotBlank(message = "State is required")
    private String state;
    
    @NotBlank(message = "District is required")
    private String district;
}
```

### Required Change: Enable Validation in Controller

**File**: `LeadController.java`

**Step 1**: Add import at the top:
```java
import jakarta.validation.Valid;
```

**Step 2**: Modify the `createLead` method (line 145):

**BEFORE**:
```java
public ResponseEntity<ApiResponse<Lead>> createLead(@RequestBody LeadCreateRequest request) {
```

**AFTER**:
```java
public ResponseEntity<ApiResponse<Lead>> createLead(@Valid @RequestBody LeadCreateRequest request) {
```

This enables automatic validation. Spring will now:
1. Validate the request body before the method executes
2. Return 400 Bad Request with validation errors if validation fails
3. Only call the service method if validation passes

---

## Frontend Validation (Flutter Side)

### Current Status: ✅ PARTIAL VALIDATION EXISTS

The Flutter form already has validation for:
- Name (required)
- Phone (required, min 10 digits)
- Email (format validation)
- WhatsApp (format validation if provided)
- Customer Type (required)
- Lead Source (required)
- Priority (required)
- Lead Status (required)
- Lost Reason (required when status = "lost")

### Enhancement Needed: Add State/District validation

**File**: `form_sections.dart`

**Location 1**: State dropdown validator (line 235-238)

**CHANGE FROM**:
```dart
validator: (value) {
  // State is optional during edit; allow empty to avoid blocking save
  return null;
},
```

**CHANGE TO**:
```dart
validator: (value) {
  if (value == null || value.isEmpty) {
    return 'State is required';
  }
  return null;
},
```

**Location 2**: District dropdown validator (line 269-272)

**CHANGE FROM**:
```dart
validator: (value) {
  // District is optional during edit; allow empty to avoid blocking save
  return null;
},
```

**CHANGE TO**:
```dart
validator: (value) {
  if (value == null || value.isEmpty) {
    return 'District is required';
  }
  return null;
},
```

---

## Complete Validation Matrix

| Field | Frontend Validation | Backend Validation | Required |
|-------|--------------------|--------------------|----------|
| **Name** | ✅ Required, triggers on blur | ✅ @NotBlank, @Size(2-100) | ✅ YES |
| **Email** | ✅ Format validation (regex) | ✅ @Email | ❌ NO |
| **Phone** | ✅ Required, min 10 digits | ✅ @NotBlank, @Pattern(10-15 digits) | ✅ YES |
| **WhatsApp** | ✅ Format if provided | ✅ @Pattern(10-15 digits) | ❌ NO |
| **Customer Type** | ✅ Required dropdown | ✅ @NotBlank | ✅ YES |
| **Lead Source** | ✅ Required dropdown | ✅ @NotBlank | ✅ YES |
| **Priority** | ✅ Required dropdown | ✅ @NotBlank | ✅ YES |
| **Lead Status** | ✅ Required dropdown | ✅ @NotBlank | ✅ YES |
| **State** | ⚠️ Currently optional | ✅ @NotBlank | ✅ YES |
| **District** | ⚠️ Currently optional | ✅ @NotBlank | ✅ YES |
| **Location** | ❌ Optional | ❌ Optional | ❌ NO |
| **Address** | ❌ Optional | ❌ Optional | ❌ NO |
| **Project Type** | ❌ Optional | ❌ Optional | ❌ NO |
| **Budget** | ❌ Optional | ❌ Optional | ❌ NO |
| **Sq Ft Area** | ❌ Optional | ❌ Optional | ❌ NO |
| **Lost Reason** | ✅ Required if status=lost | ❌ Not validated | ✅ YES (conditional) |

---

## Implementation Steps

### 1. Backend (API) - MANUAL EDIT REQUIRED

**File to Edit**: `n:\Projects\wd projects git\wd_portal_api\src\main\java\com\wd\api\controller\LeadController.java`

1. Add import at top (around line 15):
   ```java
   import jakarta.validation.Valid;
   ```

2. Change line 145 from:
   ```java
   public ResponseEntity<ApiResponse<Lead>> createLead(@RequestBody LeadCreateRequest request) {
   ```
   
   To:
   ```java
   public ResponseEntity<ApiResponse<Lead>> createLead(@Valid @RequestBody LeadCreateRequest request) {
   ```

3. Save file
4. Restart API server

### 2. Frontend (Flutter) - MANUAL EDIT REQUIRED

**File to Edit**: `n:\Projects\wd projects git\wd_portal_app_flutter\lib\features\leads\presentation\screens\components\form_sections.dart`

1. Find State dropdown validator (line 235-238)
2. Replace with:
   ```dart
   validator: (value) {
     if (value == null || value.isEmpty) {
       return 'State is required';
     }
     return null;
   },
   ```

3. Find District dropdown validator (line 269-272)
4. Replace with:
   ```dart
   validator: (value) {
     if (value == null || value.isEmpty) {
       return 'District is required';
     }
     return null;
   },
   ```

5. Save file
6. Hot reload Flutter app (press `r` in terminal) or restart

---

## Testing the Validation

### Test Case 1: Empty Name
1. Open Add Lead form
2. Leave "Name" field empty
3. Fill other required fields
4. Click Save

**Expected**: Frontend shows "Name is required" validation error before API call

### Test Case 2: Invalid Email
1. Enter name: "Test User"
2. Enter email: "invalid-email"
3. Fill other fields
4. Click Save

**Expected**: Frontend shows "Enter a valid email" error

### Test Case 3: Short Phone Number
1. Enter name: "Test User"
2. Enter phone: "123"
3. Fill other fields
4. Click Save

**Expected**: Frontend shows "Phone number must be at least 10 digits"

### Test Case 4: Missing State
1. Enter all required fields
2. Leave State dropdown empty
3. Click Save

**Expected**: Frontend shows "State is required"

### Test Case 5: Missing District
1. Select a State
2. Leave District empty
3. Click Save

**Expected**: Frontend shows "District is required"

### Test Case 6: Backend Validation (Bypass Frontend)
If someone bypasses frontend and sends API request directly:

```bash
curl -X POST http://localhost:8081/api/leads \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "email": "test@example.com"
  }'
```

**Expected**: API returns 400 Bad Request with validation errors:
```json
{
  "timestamp": "2026-01-11T02:10:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "message": "Name is required"
    },
    {
      "field": "phone",
      "message": "Phone number is required"
    },
    ...
  ]
}
```

---

## Benefits

### 1. **Better User Experience**
- Errors shown immediately as user types
- Clear, field-specific error messages
- No unnecessary API calls for invalid data

### 2. **Data Integrity**
- Backend enforces validation even if frontend is bypassed
- Consistent validation rules across both layers
- Prevents invalid data from reaching database

### 3. **Security**
- Backend validation prevents malicious requests
- Input sanitization via regex patterns
- Protection against injection attacks

### 4. **Reduced Server Load**
- Frontend validation prevents unnecessary API calls
- Server only processes valid requests
- Better performance under load

---

## Manual Edit Required

**IMPORTANT**: Due to file encoding issues with automated editing, please manually apply the changes described above:

1. ✅ **Backend DTO**: Already applied (LeadCreateRequest.java has validation annotations)
2. ⚠️ **Backend Controller**: Needs manual edit (add @Valid annotation)
3. ⚠️ **Frontend Validators**: Needs manual edit (add State/District validation)

After making these changes:
- Restart API server
- Hot reload or restart Flutter app
- Test the validation with the test cases above

---

**Status**: Backend DTO validation ✅ Ready | Controller @Valid ⚠️ Manual edit needed | Frontend validators ⚠️ Manual edit needed

**Priority**: HIGH - These validations prevent data integrity issues

**Estimated Time**: 5 minutes for manual edits

