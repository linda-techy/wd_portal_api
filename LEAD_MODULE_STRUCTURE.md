# Lead Module Structure: Activities View Placement

## API Structure (Backend - Java Spring Boot)

### Controller Layer
**Location**: `src/main/java/com/wd/api/controller/LeadController.java`

**Endpoint**:
```java
GET /leads/{leadId}/activities
@GetMapping("/{leadId}/activities")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public ResponseEntity<ApiResponse<List<ActivityFeedDTO>>> getLeadActivities(@PathVariable String leadId)
```

**API URL**: `http://localhost:8081/leads/{leadId}/activities`

**Response Format**:
```json
{
  "success": true,
  "message": "Lead activities retrieved successfully",
  "data": [
    {
      "id": 1,
      "title": "Lead Created",
      "description": "Lead lead 1 was created",
      "activityType": "LEAD_CREATED",
      "createdAt": "2026-01-18T10:00:00",
      "createdByName": "admin@example.com"
    }
  ]
}
```

### Service Layer
**Location**: `src/main/java/com/wd/api/service/ActivityFeedService.java`

**Method**:
```java
@Transactional(readOnly = true)
public List<ActivityFeedDTO> getActivitiesForLead(Long leadId)
```

**Responsibilities**:
1. Fetches from `activity_feeds` table (system events)
2. Fetches from `lead_interactions` table (sales interactions)
3. Converts both to `ActivityFeedDTO` format
4. Combines and sorts by date (most recent first)

**Delegation**: `LeadService.getLeadActivities()` → `ActivityFeedService.getActivitiesForLead()`

---

## Flutter UI Structure (Frontend)

### Screen Layer
**Location**: `lib/features/leads/presentation/screens/lead_activity_screen.dart`

**Screen Class**: `LeadActivityScreen`
```dart
class LeadActivityScreen extends StatelessWidget {
  final Lead lead;
  // ...
  body: LeadActivityTimeline(leadId: lead.leadId),
}
```

**Navigation Path**: Accessed from `LeadsScreen` (lead list)

### Component Layer
**Location**: `lib/features/leads/presentation/screens/components/lead_activity_timeline.dart`

**Component Class**: `LeadActivityTimeline`
```dart
class LeadActivityTimeline extends StatefulWidget {
  final String leadId;
  // Displays timeline of activities
}
```

**Responsibilities**:
- Fetches activities via `LeadService.getLeadActivities()`
- Displays timeline UI
- Handles loading states and errors

### Service Layer (Flutter)
**Location**: `lib/features/leads/data/services/lead_service.dart`

**Method**:
```dart
Future<List<ActivityFeed>> getLeadActivities(String leadId) async {
  final response = await _apiService.get('/leads/$leadId/activities');
  return _apiService.unwrapList<ActivityFeed>(response, (json) => ActivityFeed.fromJson(json));
}
```

**API Call**: `GET /leads/{leadId}/activities`

### Model Layer
**Location**: `lib/features/leads/data/models/activity_feed.dart`

**Model Class**: `ActivityFeed`
```dart
class ActivityFeed {
  final int id;
  final String title;
  final String description;
  final String activityType;
  final DateTime createdAt;
  final String? createdByName;
  
  factory ActivityFeed.fromJson(Map<String, dynamic> json);
}
```

---

## Module Organization

### Flutter Lead Module Structure
```
lib/features/leads/
├── data/
│   ├── models/
│   │   ├── activity_feed.dart          ← Activity model
│   │   ├── lead_interaction.dart       ← Interaction model
│   │   └── lead.dart                   ← Lead model
│   └── services/
│       └── lead_service.dart           ← API calls (getLeadActivities)
├── presentation/
│   ├── screens/
│   │   ├── leads_screen.dart           ← Lead list (navigates to activity screen)
│   │   └── lead_activity_screen.dart   ← Activity screen (wrapper)
│   └── screens/components/
│       └── lead_activity_timeline.dart ← Activity timeline component (UI)
```

### API Lead Module Structure
```
src/main/java/com/wd/api/
├── controller/
│   └── LeadController.java             ← GET /leads/{leadId}/activities
├── service/
│   ├── LeadService.java                ← Delegates to ActivityFeedService
│   └── ActivityFeedService.java        ← Combines activity_feeds + lead_interactions
├── model/
│   ├── ActivityFeed.java               ← activity_feeds table entity
│   └── LeadInteraction.java            ← lead_interactions table entity
└── dto/
    └── ActivityFeedDTO.java            ← Response DTO (prevents lazy-loading issues)
```

---

## Navigation Flow

### User Journey
1. **Lead List** → `LeadsScreen` (`lib/features/leads/presentation/screens/leads_screen.dart`)
2. **User clicks "View Activity"** on a lead row
3. **Navigation** → `Navigator.push(context, MaterialPageRoute(builder: (context) => LeadActivityScreen(lead: lead)))`
4. **Activity Screen** → `LeadActivityScreen` (`lib/features/leads/presentation/screens/lead_activity_screen.dart`)
5. **Activity Component** → `LeadActivityTimeline` (`lib/features/leads/presentation/screens/components/lead_activity_timeline.dart`)
6. **API Call** → `LeadService.getLeadActivities()` → `GET /leads/{leadId}/activities`
7. **API Response** → `ActivityFeedDTO` list
8. **Display** → Timeline widget showing all activities

---

## Key Files Summary

| Layer | File Path | Purpose |
|-------|-----------|---------|
| **API Controller** | `controller/LeadController.java` | `GET /leads/{leadId}/activities` endpoint |
| **API Service** | `service/ActivityFeedService.java` | Combines `activity_feeds` + `lead_interactions` |
| **API DTO** | `dto/ActivityFeedDTO.java` | Response format (prevents lazy-loading issues) |
| **Flutter Screen** | `screens/lead_activity_screen.dart` | Activity screen wrapper |
| **Flutter Component** | `components/lead_activity_timeline.dart` | Activity timeline UI component |
| **Flutter Service** | `services/lead_service.dart` | API calls (`getLeadActivities`) |
| **Flutter Model** | `models/activity_feed.dart` | Activity data model |

---

## Summary

✅ **Properly placed** in the lead module structure:
- **API**: Under `LeadController` with endpoint `/leads/{leadId}/activities`
- **Service**: `ActivityFeedService` handles data combination
- **Flutter Screen**: Dedicated `LeadActivityScreen` 
- **Flutter Component**: Reusable `LeadActivityTimeline` component
- **Navigation**: Accessible from lead list screen

The activities view is **well-organized** and follows Flutter feature-based architecture with proper separation of concerns.
