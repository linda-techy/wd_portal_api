# Quick Manual Edit Instructions

## Edit 1: LeadController.java ✅ Import Added, ⚠️ Annotation Needed

**File**: `n:\Projects\wd projects git\wd_portal_api\src\main\java\com\wd\api\controller\LeadController.java`

**Status**: Import ✅ Added | Annotation ⚠️ Needs manual edit

### Step: Add @Valid annotation (Line 146)

**Find** (line 146):
```java
public ResponseEntity<ApiResponse<Lead>> createLead(@RequestBody LeadCreateRequest request) {
```

**Replace with**:
```java
public ResponseEntity<ApiResponse<Lead>> createLead(@Valid @RequestBody LeadCreateRequest request) {
```

**Location**: Line 146  
**Change**: Add `@Valid` before `@RequestBody`  
**Time**: 5 seconds

---

## Edit 2: form_sections.dart - State Validator

**File**: `n:\Projects\wd projects git\wd_portal_app_flutter\lib\features\leads\presentation\screens\components\form_sections.dart`

### Step 1: State Validator (Line 235-238)

**Find**:
```dart
validator: (value) {
  // State is optional during edit; allow empty to avoid blocking save
  return null;
},
```

**Replace with**:
```dart
validator: (value) {
  if (value == null || value.isEmpty) {
    return 'State is required';
  }
  return null;
},
```

**Location**: Lines 235-238  
**Time**: 20 seconds

### Step 2: District Validator (Line 269-272)

**Find**:
```dart
validator: (value) {
  // District is optional during edit; allow empty to avoid blocking save
  return null;
},
```

**Replace with**:
```dart
validator: (value) {
  if (value == null || value.isEmpty) {
    return 'District is required';
  }
  return null;
},
```

**Location**: Lines 269-272  
**Time**: 20 seconds

---

## After Edits:

1. **Save all files**
2. **Restart API** (Ctrl+C in API terminal, then run `./mvnw.cmd spring-boot:run`)
3. **Hot reload Flutter** (press `r` in Flutter terminal)
4. **Test validation** - Try creating a lead without State/District

---

**Total Time**: ~1 minute  
**Status**: 1 of 3 edits applied, 2 remaining
