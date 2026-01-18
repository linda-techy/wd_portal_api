# Table Analysis: activity_feeds vs lead_interactions

## Purpose & Design Philosophy

### **activity_feeds** - System Audit Log / Activity Feed
**Purpose**: General-purpose activity log for **automatic system events** and audit trail

**Key Characteristics**:
- **Auto-generated**: Created by system when entities are created/updated
- **Generic**: Can track activities for LEADS, PROJECTS, or any entity via `reference_type`
- **Polymorphic**: Uses `reference_type` + `reference_id` pattern (LEAD, PROJECT, etc.)
- **Activity Types**: Categorized via `activity_types` table (LEAD_CREATED, LEAD_UPDATED, LEAD_STATUS_CHANGED, etc.)
- **Metadata**: Stores additional structured data in JSONB `metadata` field
- **Project Linking**: Can link activities to projects via `project_id` for project timelines

**When Used**:
- Lead created → logs "LEAD_CREATED"
- Lead status changed → logs "LEAD_STATUS_CHANGED"
- Lead converted to project → logs "LEAD_CONVERTED"
- System-generated events
- Audit trail for compliance

**Fields**:
- `title`, `description` (generic activity description)
- `activity_type_id` → `activity_types` (categorized)
- `reference_type`, `reference_id` (polymorphic reference)
- `metadata` (JSONB for structured data)
- `project_id` (optional link to project)

---

### **lead_interactions** - Sales Communication Log / CRM Interactions
**Purpose**: Specialized table for **manual sales interactions** and communication tracking

**Key Characteristics**:
- **Manual Entry**: Created by sales staff when logging communications
- **Lead-Specific**: Only tracks interactions with leads (no polymorphic pattern)
- **Sales-Oriented**: Designed for CRM workflows (outcomes, follow-ups, actions)
- **Communication Types**: Fixed enum (CALL, EMAIL, MEETING, SITE_VISIT, WHATSAPP, SMS, OTHER)
- **Pipeline Management**: Tracks outcomes and next actions for sales process
- **Duration Tracking**: Records call/meeting duration
- **Follow-up Tracking**: `next_action` and `next_action_date` for pipeline management

**When Used**:
- Sales call logged → "CALL" interaction
- Email sent to lead → "EMAIL" interaction  
- Site visit scheduled → "SITE_VISIT" interaction
- Meeting notes recorded → "MEETING" interaction
- Manual sales activity logging
- CRM communication history

**Fields**:
- `interaction_type` (CALL, EMAIL, MEETING, etc.)
- `subject` (interaction subject/title)
- `notes` (detailed notes from interaction)
- `outcome` (SCHEDULED_FOLLOWUP, QUOTE_SENT, NEEDS_INFO, NOT_INTERESTED, CONVERTED, HOT_LEAD, COLD_LEAD)
- `duration_minutes` (call/meeting duration)
- `next_action`, `next_action_date` (follow-up planning)
- `interaction_date` (when interaction occurred)

---

## Summary: They Are NOT Duplicates

### activity_feeds
✅ **System audit log** - What happened (automatically)  
✅ **Polymorphic** - Works for leads, projects, any entity  
✅ **Generic** - Can log any type of activity  
✅ **Audit trail** - Compliance and history tracking  

### lead_interactions  
✅ **Sales CRM log** - What sales team did (manually)  
✅ **Lead-specific** - Only for lead communications  
✅ **Sales-focused** - Designed for pipeline management  
✅ **Action-oriented** - Tracks outcomes and next steps  

---

## Why Combine Them in Activities View?

Both tables contain **relevant activity information for a lead**, but serve different purposes:

1. **activity_feeds** shows **system events** (automatic tracking)
   - "Lead was created on Jan 18"
   - "Lead status changed from NEW to QUALIFIED"

2. **lead_interactions** shows **sales activities** (manual tracking)  
   - "Phone call on Jan 18 - discussed requirements"
   - "Site visit scheduled for Jan 25"
   - "Quotation sent, waiting for response"

**Combined**, they provide a **complete activity timeline** showing:
- System-generated events (what the system did)
- Sales activities (what the team did)
- Both sorted chronologically for a unified view

---

## Recommendation: Keep Both Tables

**DO NOT merge them** - They serve distinct business purposes:
- `activity_feeds` = System audit log (automatic, generic, polymorphic)
- `lead_interactions` = Sales CRM log (manual, lead-specific, action-oriented)

**Current solution is correct**: Combine them at the **API/service layer** for unified display, but maintain separate tables for their distinct purposes.
