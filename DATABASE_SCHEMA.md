# WallDot Builders - Database Schema Documentation
**Total Tables:** 37
**Database:** PostgreSQL (wdTestDB)

## Table of Contents
1. [activity_feeds](#activity-feeds)
2. [activity_types](#activity-types)
3. [boq_items](#boq-items)
4. [boq_work_types](#boq-work-types)
5. [cctv_cameras](#cctv-cameras)
6. [customer_permissions](#customer-permissions)
7. [customer_project_members](#customer-project-members)
8. [customer_project_team_members](#customer-project-team-members)
9. [customer_projects](#customer-projects)
10. [customer_refresh_tokens](#customer-refresh-tokens)
11. [customer_role_permissions](#customer-role-permissions)
12. [customer_roles](#customer-roles)
13. [customer_users](#customer-users)
14. [design_steps](#design-steps)
15. [document_categories](#document-categories)
16. [feedback_forms](#feedback-forms)
17. [feedback_responses](#feedback-responses)
18. [gallery_images](#gallery-images)
19. [leads](#leads)
20. [observations](#observations)
21. [partnership_users](#partnership-users)
22. [portal_permissions](#portal-permissions)
23. [portal_refresh_tokens](#portal-refresh-tokens)
24. [portal_role_permissions](#portal-role-permissions)
25. [portal_roles](#portal-roles)
26. [portal_users](#portal-users)
27. [project_design_steps](#project-design-steps)
28. [project_documents](#project-documents)
29. [project_members](#project-members)
30. [project_queries](#project-queries)
31. [quality_checks](#quality-checks)
32. [site_reports](#site-reports)
33. [site_visits](#site-visits)
34. [sqft_categories](#sqft-categories)
35. [staff_roles](#staff-roles)
36. [tasks](#tasks)
37. [view_360](#view-360)

---

## Data Type Mappings (PostgreSQL → Java)

| PostgreSQL Type | Java Type | Notes |
|----------------|-----------|-------|
| `bigint` | `Long` | 64-bit integer |
| `integer` | `Integer` | 32-bit integer |
| `varchar(n)` | `String` | Variable character with max length |
| `text` | `String` | Unlimited text |
| `boolean` | `Boolean` | True/False |
| `numeric(p,s)` | `BigDecimal` | Precise decimal numbers |
| `double precision` | `Double` | Floating point |
| `date` | `LocalDate` | Date without time |
| `timestamp` | `LocalDateTime` | Date and time |
| `uuid` | `UUID` | Universally unique identifier |
| `jsonb` | `String` or custom | JSON binary format |

---

## activity_feeds

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `metadata` | `jsonb` | ✓ | `-` | - |
| `reference_id` | `bigint(64,0)` | ✓ | `-` | - |
| `reference_type` | `character varying(50)` | ✓ | `-` | - |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `activity_type_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `activity_types.id` |
| `created_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |

### Primary Key

- `id`

### Foreign Keys

- `activity_type_id` → `activity_types.id`
- `project_id` → `customer_projects.id`
- `created_by_id` → `customer_users.id`

---

## activity_types

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `color` | `character varying(20)` | ✓ | `-` | - |
| `icon` | `character varying(50)` | ✓ | `-` | - |
| `name` | `character varying(100)` | ✗ | `-` | 🔒 UNIQUE |
| `description` | `text` | ✓ | `-` | - |

### Primary Key

- `id`

### Unique Constraints

- `name`

---

## boq_items

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `amount` | `numeric(15,2)` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `description` | `character varying(255)` | ✗ | `-` | - |
| `is_active` | `boolean` | ✓ | `-` | - |
| `item_code` | `character varying(50)` | ✓ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |
| `quantity` | `numeric(10,2)` | ✗ | `-` | - |
| `rate` | `numeric(15,2)` | ✗ | `-` | - |
| `specifications` | `text` | ✓ | `-` | - |
| `unit` | `character varying(50)` | ✗ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✗ | `-` | - |
| `created_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `work_type_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `boq_work_types.id` |
| `total_amount` | `numeric(10,2)` | ✓ | `-` | - |
| `unit_rate` | `numeric(10,2)` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `created_by_id` → `customer_users.id`
- `work_type_id` → `boq_work_types.id`
- `project_id` → `customer_projects.id`

---

## boq_work_types

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `description` | `character varying(255)` | ✓ | `-` | - |
| `display_order` | `integer(32,0)` | ✓ | `-` | - |
| `name` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |

### Primary Key

- `id`

### Unique Constraints

- `name`

---

## cctv_cameras

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `camera_name` | `character varying(100)` | ✗ | `-` | - |
| `camera_type` | `character varying(50)` | ✓ | `-` | - |
| `installation_date` | `date` | ✓ | `-` | - |
| `is_active` | `boolean` | ✓ | `-` | - |
| `is_installed` | `boolean` | ✓ | `-` | - |
| `last_active` | `timestamp without time zone` | ✓ | `-` | - |
| `location` | `character varying(255)` | ✓ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |
| `resolution` | `character varying(20)` | ✓ | `-` | - |
| `snapshot_url` | `character varying(500)` | ✓ | `-` | - |
| `stream_url` | `character varying(500)` | ✓ | `-` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `name` | `character varying(255)` | ✗ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`

---

## customer_permissions

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `description` | `character varying(255)` | ✓ | `-` | - |
| `name` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |

### Primary Key

- `id`

### Unique Constraints

- `name`

---

## customer_project_members

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `customer_id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK 🔗 FK → `customer_users.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK 🔗 FK → `customer_projects.id` |

### Primary Key

- Composite: `project_id, customer_id`

### Foreign Keys

- `customer_id` → `customer_users.id`
- `project_id` → `customer_projects.id`

---

## customer_project_team_members

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK 🔗 FK → `customer_projects.id` |
| `user_id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK 🔗 FK → `portal_users.id` |

### Primary Key

- Composite: `user_id, project_id`

### Foreign Keys

- `user_id` → `portal_users.id`
- `project_id` → `customer_projects.id`

---

## customer_projects

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('customer_projects_...` | 🔑 PK |
| `name` | `character varying(255)` | ✗ | `-` | - |
| `location` | `character varying(255)` | ✓ | `-` | - |
| `start_date` | `date` | ✓ | `-` | - |
| `end_date` | `date` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `created_by` | `character varying(255)` | ✓ | `-` | - |
| `project_phase` | `character varying(100)` | ✗ | `'design'::character varying` | - |
| `state` | `character varying(50)` | ✗ | `-` | - |
| `district` | `character varying(50)` | ✗ | `-` | - |
| `sqfeet` | `numeric(10,2)` | ✓ | `-` | - |
| `lead_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `leads.lead_id` |
| `code` | `character varying(255)` | ✓ | `-` | - |
| `design_package` | `character varying(255)` | ✓ | `-` | - |
| `is_design_agreement_signed` | `boolean` | ✗ | `false` | - |
| `project_uuid` | `uuid` | ✗ | `-` | 🔒 UNIQUE |
| `sq_feet` | `double precision(53)` | ✓ | `-` | - |
| `customer_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_users.id` |
| `project_type` | `character varying(255)` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `lead_id` → `leads.lead_id`
- `customer_id` → `customer_users.id`

### Unique Constraints

- `project_uuid`
- `project_uuid`

---

## customer_refresh_tokens

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `expiry_date` | `timestamp without time zone` | ✗ | `-` | - |
| `revoked` | `boolean` | ✗ | `-` | - |
| `token` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |
| `user_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |

### Primary Key

- `id`

### Foreign Keys

- `user_id` → `customer_users.id`

### Unique Constraints

- `token`

---

## customer_role_permissions

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `role_id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK 🔗 FK → `customer_roles.id` |
| `permission_id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK 🔗 FK → `customer_permissions.id` |

### Primary Key

- Composite: `role_id, permission_id`

### Foreign Keys

- `permission_id` → `customer_permissions.id`
- `role_id` → `customer_roles.id`

---

## customer_roles

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `description` | `character varying(255)` | ✓ | `-` | - |
| `name` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |

### Primary Key

- `id`

### Unique Constraints

- `name`

---

## customer_users

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `email` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |
| `enabled` | `boolean` | ✗ | `-` | - |
| `first_name` | `character varying(255)` | ✓ | `-` | - |
| `last_name` | `character varying(255)` | ✓ | `-` | - |
| `password` | `character varying(255)` | ✗ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |
| `role_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_roles.id` |

### Primary Key

- `id`

### Foreign Keys

- `role_id` → `customer_roles.id`

### Unique Constraints

- `email`

---

## design_steps

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('design_steps_id_se...` | 🔑 PK |
| `step_name` | `character varying(255)` | ✗ | `-` | - |
| `weight_percentage` | `double precision(53)` | ✗ | `-` | - |
| `category` | `character varying(50)` | ✓ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `display_order` | `integer(32,0)` | ✓ | `-` | - |

### Primary Key

- `id`

---

## document_categories

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `description` | `character varying(255)` | ✓ | `-` | - |
| `display_order` | `integer(32,0)` | ✓ | `-` | - |
| `name` | `character varying(100)` | ✗ | `-` | 🔒 UNIQUE |

### Primary Key

- `id`

### Unique Constraints

- `name`

---

## feedback_forms

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `form_type` | `character varying(50)` | ✓ | `-` | - |
| `is_active` | `boolean` | ✓ | `-` | - |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `created_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `form_schema` | `jsonb` | ✓ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `created_by_id` → `customer_users.id`
- `created_by_id` → `portal_users.id`
- `project_id` → `customer_projects.id`

---

## feedback_responses

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `comments` | `text` | ✓ | `-` | - |
| `is_completed` | `boolean` | ✓ | `-` | - |
| `rating` | `integer(32,0)` | ✓ | `-` | - |
| `response_data` | `jsonb` | ✓ | `-` | - |
| `submitted_at` | `timestamp without time zone` | ✗ | `-` | - |
| `customer_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `form_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `feedback_forms.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |

### Primary Key

- `id`

### Foreign Keys

- `form_id` → `feedback_forms.id`
- `project_id` → `customer_projects.id`
- `customer_id` → `customer_users.id`

---

## gallery_images

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `caption` | `character varying(255)` | ✓ | `-` | - |
| `image_path` | `character varying(500)` | ✗ | `-` | - |
| `location_tag` | `character varying(255)` | ✓ | `-` | - |
| `tags` | `ARRAY` | ✓ | `-` | - |
| `taken_date` | `date` | ✗ | `-` | - |
| `thumbnail_path` | `character varying(500)` | ✓ | `-` | - |
| `uploaded_at` | `timestamp without time zone` | ✗ | `-` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `site_report_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `site_reports.id` |
| `uploaded_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `image_url` | `character varying(500)` | ✗ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `uploaded_by_id` → `customer_users.id`
- `site_report_id` → `site_reports.id`
- `project_id` → `customer_projects.id`
- `uploaded_by_id` → `portal_users.id`

---

## leads

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `name` | `character varying(255)` | ✗ | `-` | - |
| `email` | `character varying(255)` | ✓ | `-` | - |
| `phone` | `character varying(255)` | ✓ | `-` | - |
| `whatsapp_number` | `character varying(255)` | ✓ | `-` | - |
| `lead_source` | `character varying(255)` | ✗ | `'website'::character varying` | - |
| `lead_status` | `character varying(255)` | ✗ | `'New Inquiry'::character va...` | - |
| `priority` | `character varying(255)` | ✗ | `'low'::character varying` | - |
| `customer_type` | `character varying(255)` | ✓ | `-` | - |
| `address` | `character varying(255)` | ✓ | `-` | - |
| `project_type` | `character varying(255)` | ✓ | `-` | - |
| `project_description` | `text` | ✓ | `-` | - |
| `requirements` | `character varying(255)` | ✓ | `-` | - |
| `budget` | `numeric(38,2)` | ✓ | `-` | - |
| `next_follow_up` | `timestamp without time zone` | ✓ | `-` | - |
| `last_contact_date` | `timestamp without time zone` | ✓ | `-` | - |
| `assigned_team` | `character varying(255)` | ✓ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |
| `client_rating` | `integer(32,0)` | ✓ | `-` | - |
| `probability_to_win` | `integer(32,0)` | ✓ | `-` | - |
| `lost_reason` | `character varying(255)` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |
| `lead_id` | `bigint(64,0)` | ✗ | `nextval('leads_lead_id_seq'...` | 🔑 PK |
| `date_of_enquiry` | `date` | ✓ | `-` | - |
| `state` | `character varying(255)` | ✓ | `-` | - |
| `district` | `character varying(255)` | ✓ | `-` | - |
| `location` | `character varying(255)` | ✓ | `-` | - |
| `project_sqft_area` | `numeric(38,2)` | ✓ | `-` | - |
| `score` | `integer(32,0)` | ✓ | `0` | - |
| `score_category` | `character varying(20)` | ✓ | `'COLD'::character varying` | - |
| `last_scored_at` | `timestamp without time zone` | ✓ | `-` | - |
| `score_factors` | `jsonb` | ✓ | `-` | - |
| `plot_area` | `numeric(10,2)` | ✓ | `-` | - |
| `floors` | `integer(32,0)` | ✓ | `-` | - |
| `converted_by_id` | `bigint(64,0)` | ✓ | `-` | - |
| `converted_at` | `timestamp without time zone` | ✓ | `-` | - |
| `assigned_to_id` | `bigint(64,0)` | ✓ | `-` | - |

### Primary Key

- `lead_id`

---

---

## material_indent_items

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `indent_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `material_indents.id` |
| `material_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `materials.id` |
| `item_name` | `character varying(255)` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `unit` | `character varying(50)` | ✗ | `-` | - |
| `quantity_requested` | `numeric(10,2)` | ✗ | `-` | - |
| `quantity_approved` | `numeric(10,2)` | ✓ | `-` | - |
| `po_quantity` | `numeric(10,2)` | ✓ | `0` | - |
| `estimated_rate` | `numeric(15,2)` | ✓ | `-` | - |
| `estimated_amount` | `numeric(15,2)` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✗ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `indent_id` → `material_indents.id`
- `material_id` → `materials.id`

---

## material_indents

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `indent_number` | `character varying(50)` | ✗ | `-` | 🔒 UNIQUE |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `request_date` | `date` | ✗ | `-` | - |
| `required_date` | `date` | ✗ | `-` | - |
| `status` | `character varying(50)` | ✗ | `'DRAFT'` | - |
| `priority` | `character varying(20)` | ✓ | `'MEDIUM'` | - |
| `notes` | `text` | ✓ | `-` | - |
| `requested_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `approved_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `approved_at` | `timestamp without time zone` | ✓ | `-` | - |
| `rejection_reason` | `text` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✗ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `requested_by_id` → `portal_users.id`
- `approved_by_id` → `portal_users.id`

### Unique Constraints

- `indent_number`

---

## observations

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `description` | `text` | ✗ | `-` | - |
| `image_path` | `character varying(500)` | ✓ | `-` | - |
| `location` | `character varying(255)` | ✓ | `-` | - |
| `priority` | `character varying(20)` | ✓ | `-` | - |
| `reported_date` | `timestamp without time zone` | ✗ | `-` | - |
| `resolution_notes` | `text` | ✓ | `-` | - |
| `resolved_date` | `timestamp without time zone` | ✓ | `-` | - |
| `status` | `character varying(50)` | ✓ | `-` | - |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `reported_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `reported_by_role_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `staff_roles.id` |
| `resolved_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_users.id` |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `severity` | `character varying(50)` | ✓ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `reported_by_role_id` → `staff_roles.id`
- `reported_by_id` → `customer_users.id`
- `resolved_by_id` → `customer_users.id`
- `reported_by_id` → `portal_users.id`
- `project_id` → `customer_projects.id`

---

## partnership_users

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('partnership_users_...` | 🔑 PK |
| `phone` | `character varying(15)` | ✗ | `-` | 🔒 UNIQUE |
| `email` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |
| `password_hash` | `character varying(255)` | ✗ | `-` | - |
| `full_name` | `character varying(255)` | ✗ | `-` | - |
| `designation` | `character varying(255)` | ✓ | `-` | - |
| `partnership_type` | `character varying(50)` | ✗ | `-` | - |
| `firm_name` | `character varying(255)` | ✓ | `-` | - |
| `company_name` | `character varying(255)` | ✓ | `-` | - |
| `gst_number` | `character varying(20)` | ✓ | `-` | - |
| `license_number` | `character varying(100)` | ✓ | `-` | - |
| `rera_number` | `character varying(100)` | ✓ | `-` | - |
| `cin_number` | `character varying(50)` | ✓ | `-` | - |
| `ifsc_code` | `character varying(20)` | ✓ | `-` | - |
| `employee_id` | `character varying(100)` | ✓ | `-` | - |
| `experience` | `integer(32,0)` | ✓ | `-` | - |
| `specialization` | `character varying(255)` | ✓ | `-` | - |
| `portfolio_link` | `character varying(500)` | ✓ | `-` | - |
| `certifications` | `text` | ✓ | `-` | - |
| `area_of_operation` | `character varying(255)` | ✓ | `-` | - |
| `areas_covered` | `character varying(255)` | ✓ | `-` | - |
| `land_types` | `character varying(255)` | ✓ | `-` | - |
| `materials_supplied` | `character varying(500)` | ✓ | `-` | - |
| `business_size` | `character varying(50)` | ✓ | `-` | - |
| `location` | `character varying(255)` | ✓ | `-` | - |
| `industry` | `character varying(255)` | ✓ | `-` | - |
| `project_type` | `character varying(100)` | ✓ | `-` | - |
| `project_scale` | `character varying(50)` | ✓ | `-` | - |
| `timeline` | `character varying(255)` | ✓ | `-` | - |
| `years_of_practice` | `integer(32,0)` | ✓ | `-` | - |
| `area_served` | `character varying(255)` | ✓ | `-` | - |
| `business_name` | `character varying(255)` | ✓ | `-` | - |
| `additional_contact` | `character varying(255)` | ✓ | `-` | - |
| `message` | `text` | ✓ | `-` | - |
| `status` | `character varying(20)` | ✓ | `'pending'::character varying` | - |
| `created_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |
| `approved_at` | `timestamp without time zone` | ✓ | `-` | - |
| `last_login` | `timestamp without time zone` | ✓ | `-` | - |
| `created_by` | `character varying(100)` | ✓ | `-` | - |
| `updated_by` | `character varying(100)` | ✓ | `-` | - |

### Primary Key

- `id`

### Unique Constraints

- `phone`
- `email`

---

## portal_permissions

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('portal_permissions...` | 🔑 PK |
| `name` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |
| `description` | `character varying(255)` | ✓ | `-` | - |

### Primary Key

- `id`

### Unique Constraints

- `name`

---

## portal_refresh_tokens

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('portal_refresh_tok...` | 🔑 PK |
| `token` | `character varying(4096)` | ✗ | `-` | 🔒 UNIQUE |
| `user_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `portal_users.id` |
| `expiry_date` | `timestamp without time zone` | ✗ | `-` | - |
| `revoked` | `boolean` | ✗ | `false` | - |

### Primary Key

- `id`

### Foreign Keys

- `user_id` → `portal_users.id`

### Unique Constraints

- `token`

---

---

## labour_advances

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `advance_date` | `date` | ✗ | `-` | - |
| `amount` | `numeric(15,2)` | ✗ | `-` | - |
| `labour_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `labour.id` |
| `notes` | `character varying(255)` | ✓ | `-` | - |
| `recovered_amount` | `numeric(15,2)` | ✗ | `0` | - |

### Primary Key

- `id`

### Foreign Keys

- `labour_id` → `labour.id`

---

## portal_role_permissions

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `role_id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK 🔗 FK → `portal_roles.id` |
| `permission_id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK 🔗 FK → `portal_permissions.id` |

### Primary Key

- Composite: `role_id, permission_id`

### Foreign Keys

- `permission_id` → `portal_permissions.id`
- `role_id` → `portal_roles.id`

---

## portal_roles

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('portal_roles_id_se...` | 🔑 PK |
| `name` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |
| `description` | `character varying(255)` | ✓ | `-` | - |
| `code` | `text` | ✓ | `-` | - |

### Primary Key

- `id`

### Unique Constraints

- `name`

---

## portal_users

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('portal_users_id_se...` | 🔑 PK |
| `email` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |
| `password` | `character varying(255)` | ✗ | `-` | - |
| `first_name` | `character varying(255)` | ✓ | `-` | - |
| `last_name` | `character varying(255)` | ✓ | `-` | - |
| `role_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_roles.id` |
| `enabled` | `boolean` | ✗ | `true` | - |
| `created_at` | `timestamp without time zone` | ✓ | `now()` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `now()` | - |

### Primary Key

- `id`

### Foreign Keys

- `role_id` → `portal_roles.id`

### Unique Constraints

- `email`

---

## project_design_steps

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('project_design_ste...` | 🔑 PK |
| `project_uuid` | `uuid` | ✓ | `-` | 🔗 FK → `customer_projects.project_uuid` 🔒 UNIQUE |
| `step_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `design_steps.id` 🔒 UNIQUE |
| `status` | `character varying(50)` | ✓ | `'not_started'::design_step_...` | - |
| `progress_percentage` | `double precision(53)` | ✓ | `0` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` 🔒 UNIQUE |
| `completed_at` | `timestamp without time zone` | ✓ | `-` | - |
| `completion_percentage` | `integer(32,0)` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |
| `started_at` | `timestamp without time zone` | ✓ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |
| `design_step_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `design_steps.id` |

### Primary Key

- `id`

### Foreign Keys

- `design_step_id` → `design_steps.id`
- `project_id` → `customer_projects.id`
- `project_uuid` → `customer_projects.project_uuid`
- `step_id` → `design_steps.id`

### Unique Constraints

- `project_uuid`
- `step_id`
- `project_id`
- `step_id`

---

## project_documents

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval` | 🔑 PK |
| `filename` | `character varying(255)` | ✗ | `-` | - |
| `file_path` | `character varying(500)` | ✗ | `-` | - |
| `file_type` | `character varying(50)` | ✓ | `-` | - |
| `file_size` | `bigint(64,0)` | ✓ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `category_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `document_categories.id` |
| `reference_id` | `bigint(64,0)` | ✗ | `-` | - |
| `reference_type` | `character varying(50)` | ✗ | `-` | - |
| `is_active` | `boolean` | ✗ | `true` | - |
| `created_at` | `timestamp without time zone` | ✗ | `now()` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |
| `created_by_user_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint(64,0)` | ✓ | `-` | - |
| `deleted_at` | `timestamp without time zone` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint(64,0)` | ✓ | `-` | - |
| `version` | `integer(32,0)` | ✗ | `1` | - |

### Primary Key

- `id`

### Foreign Keys

- `category_id` → `document_categories.id`
- `created_by_user_id` → `portal_users.id`

---

## project_members

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `role_in_project` | `character varying(50)` | ✓ | `-` | - |
| `portal_user_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `id` | `bigint(64,0)` | ✗ | `-` | - |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `customer_user_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_users.id` |

### Foreign Keys

- `project_id` → `customer_projects.id`
- `portal_user_id` → `portal_users.id`
- `customer_user_id` → `customer_users.id`

---

## project_queries

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `category` | `character varying(50)` | ✓ | `-` | - |
| `description` | `text` | ✗ | `-` | - |
| `priority` | `character varying(20)` | ✓ | `-` | - |
| `raised_date` | `timestamp without time zone` | ✗ | `-` | - |
| `resolution` | `text` | ✓ | `-` | - |
| `resolved_date` | `timestamp without time zone` | ✓ | `-` | - |
| `status` | `character varying(50)` | ✓ | `-` | - |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `assigned_to_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_users.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `raised_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `raised_by_role_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `staff_roles.id` |
| `resolved_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_users.id` |
| `answer` | `text` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `question` | `text` | ✓ | `-` | - |
| `responded_at` | `timestamp without time zone` | ✓ | `-` | - |
| `subject` | `character varying(255)` | ✗ | `-` | - |
| `asked_by` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_users.id` |
| `responded_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |

### Primary Key

- `id`

### Foreign Keys

- `raised_by_id` → `customer_users.id`
- `resolved_by_id` → `customer_users.id`
- `raised_by_role_id` → `staff_roles.id`
- `project_id` → `customer_projects.id`
- `assigned_to_id` → `customer_users.id`
- `responded_by_id` → `portal_users.id`
- `asked_by` → `customer_users.id`

---

## quality_checks

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `priority` | `character varying(20)` | ✓ | `-` | - |
| `resolution_notes` | `text` | ✓ | `-` | - |
| `resolved_at` | `timestamp without time zone` | ✓ | `-` | - |
| `sop_reference` | `character varying(100)` | ✓ | `-` | - |
| `status` | `character varying(50)` | ✓ | `-` | - |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `assigned_to_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_users.id` |
| `created_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `resolved_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_users.id` |
| `check_date` | `timestamp without time zone` | ✓ | `-` | - |
| `remarks` | `text` | ✓ | `-` | - |
| `result` | `character varying(50)` | ✓ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |
| `conducted_by` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |

### Primary Key

- `id`

### Foreign Keys

- `created_by_id` → `customer_users.id`
- `project_id` → `customer_projects.id`
- `resolved_by_id` → `customer_users.id`
- `assigned_to_id` → `customer_users.id`
- `conducted_by` → `portal_users.id`

---

## site_reports

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `equipment_used` | `text` | ✓ | `-` | - |
| `manpower_deployed` | `integer(32,0)` | ✓ | `-` | - |
| `report_date` | `timestamp without time zone` | ✗ | `-` | - |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `weather` | `character varying(100)` | ✓ | `-` | - |
| `work_progress` | `text` | ✓ | `-` | - |
| `created_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `status` | `character varying(50)` | ✓ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |
| `submitted_by` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `submitted_by` → `portal_users.id`
- `created_by_id` → `customer_users.id`

---

## site_visits

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `attendees` | `ARRAY` | ✓ | `-` | - |
| `check_in_time` | `timestamp without time zone` | ✗ | `-` | - |
| `check_out_time` | `timestamp without time zone` | ✓ | `-` | - |
| `findings` | `text` | ✓ | `-` | - |
| `location` | `character varying(255)` | ✓ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |
| `purpose` | `character varying(255)` | ✓ | `-` | - |
| `weather_conditions` | `character varying(100)` | ✓ | `-` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `visitor_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `visitor_role_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `staff_roles.id` |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `visit_date` | `timestamp without time zone` | ✓ | `-` | - |
| `visited_by` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `visitor_role_id` → `staff_roles.id`
- `visitor_id` → `customer_users.id`
- `visited_by` → `portal_users.id`

---

## sqft_categories

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `uuid` | ✗ | `gen_random_uuid()` | 🔑 PK |
| `category` | `character varying(50)` | ✗ | `-` | - |
| `lowest_sqft` | `integer(32,0)` | ✗ | `-` | - |
| `highest_sqft` | `integer(32,0)` | ✗ | `-` | - |
| `modified_by` | `character varying(100)` | ✓ | `-` | - |
| `update_date` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |
| `description` | `text` | ✓ | `-` | - |
| `max_sqft` | `integer(32,0)` | ✓ | `-` | - |
| `min_sqft` | `integer(32,0)` | ✓ | `-` | - |
| `name` | `character varying(100)` | ✓ | `-` | - |

### Primary Key

- `id`

---

## staff_roles

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `name` | `character varying(100)` | ✗ | `-` | 🔒 UNIQUE |
| `description` | `text` | ✓ | `-` | - |
| `display_order` | `integer(32,0)` | ✓ | `-` | - |

### Primary Key

- `id`

### Unique Constraints

- `name`

---

## retention_releases

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `amount_released` | `numeric(15,2)` | ✗ | `-` | - |
| `notes` | `character varying(255)` | ✓ | `-` | - |
| `release_date` | `date` | ✗ | `-` | - |
| `status` | `character varying(50)` | ✗ | `'PENDING'` | - |
| `work_order_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `subcontract_work_orders.id` |

### Primary Key

- `id`

### Foreign Keys

- `work_order_id` → `subcontract_work_orders.id`

---

---

---

## subcontract_measurements

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `amount` | `numeric(15,2)` | ✗ | `-` | - |
| `bill_number` | `character varying(255)` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `description` | `character varying(255)` | ✗ | `-` | - |
| `measurement_date` | `date` | ✗ | `-` | - |
| `quantity` | `numeric(15,2)` | ✗ | `-` | - |
| `rate` | `numeric(15,2)` | ✗ | `-` | - |
| `status` | `character varying(50)` | ✗ | `'PENDING'` | - |
| `unit` | `character varying(50)` | ✗ | `-` | - |
| `work_order_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `subcontract_work_orders.id` |

### Primary Key

- `id`

### Foreign Keys

- `work_order_id` → `subcontract_work_orders.id`

---

## subcontract_payments

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `created_at` | `timestamp without time zone` | ✗ | `-` | - |
| `gross_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `net_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `payment_date` | `date` | ✗ | `-` | - |
| `payment_mode` | `character varying(255)` | ✗ | `-` | - |
| `retention_amount` | `numeric(15,2)` | ✗ | `0` | - |
| `tds_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `tds_percentage` | `numeric(5,2)` | ✗ | `1.00` | - |
| `work_order_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `subcontract_work_orders.id` |

### Primary Key

- `id`

### Foreign Keys

- `work_order_id` → `subcontract_work_orders.id`

---

## subcontract_work_orders

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `-` | - |
| `actual_completion_date` | `date` | ✓ | `-` | - |
| `measurement_basis` | `character varying(50)` | ✗ | `'UNIT_RATE'` | - |
| `negotiated_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `payment_terms` | `text` | ✓ | `-` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `rate` | `numeric(15,2)` | ✓ | `-` | - |
| `retention_percentage` | `numeric(5,2)` | ✓ | `5.00` | - |
| `scope_description` | `text` | ✗ | `-` | - |
| `start_date` | `date` | ✓ | `-` | - |
| `status` | `character varying(50)` | ✗ | `'DRAFT'` | - |
| `target_completion_date` | `date` | ✓ | `-` | - |
| `total_retention_accumulated` | `numeric(15,2)` | ✓ | `0` | - |
| `unit` | `character varying(50)` | ✓ | `-` | - |
| `vendor_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `vendors.id` |
| `work_order_number` | `character varying(255)` | ✗ | `-` | 🔒 UNIQUE |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `vendor_id` → `vendors.id`

### Unique Constraints

- `work_order_number`

---

## tasks

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('tasks_id_seq'::reg...` | 🔑 PK |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `status` | `character varying(255)` | ✗ | `'PENDING'::character varying` | - |
| `priority` | `character varying(255)` | ✗ | `'MEDIUM'::character varying` | - |
| `assigned_to` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `created_by` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `project_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_projects.id` |
| `due_date` | `date` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `created_by` → `portal_users.id`
- `assigned_to` → `portal_users.id`

---

## view_360

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `capture_date` | `date` | ✓ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `is_active` | `boolean` | ✓ | `-` | - |
| `location` | `character varying(255)` | ✓ | `-` | - |
| `thumbnail_url` | `character varying(500)` | ✓ | `-` | - |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `uploaded_at` | `timestamp without time zone` | ✗ | `-` | - |
| `view_count` | `integer(32,0)` | ✓ | `-` | - |
| `view_url` | `character varying(500)` | ✗ | `-` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `uploaded_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |
| `created_at` | `timestamp without time zone` | ✓ | `-` | - |
| `panorama_url` | `character varying(500)` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `uploaded_by_id` → `customer_users.id`

---


---

## project_milestones

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `name` | `character varying(255)` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `milestone_percentage` | `numeric(5,2)` | ✓ | `-` | - |
| `amount` | `numeric(15,2)` | ✗ | `-` | - |
| `status` | `character varying(50)` | ✗ | `'PENDING'` | - |
| `due_date` | `date` | ✓ | `-` | - |
| `completed_date` | `date` | ✓ | `-` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `invoice_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `project_invoices.id` |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `invoice_id` → `project_invoices.id`

---

## receipts

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `receipt_number` | `character varying(50)` | ✗ | `-` | 🔒 UNIQUE |
| `amount` | `numeric(15,2)` | ✗ | `-` | - |
| `payment_date` | `date` | ✗ | `-` | - |
| `payment_method` | `character varying(50)` | ✓ | `-` | - |
| `transaction_reference` | `character varying(100)` | ✓ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `invoice_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `project_invoices.id` |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `invoice_id` → `project_invoices.id`

### Unique Constraints

- `receipt_number`

---

## wage_sheets

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `sheet_number` | `character varying(50)` | ✗ | `-` | 🔒 UNIQUE |
| `start_date` | `date` | ✗ | `-` | - |
| `end_date` | `date` | ✗ | `-` | - |
| `total_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `status` | `character varying(50)` | ✗ | `'DRAFT'` | - |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `generated_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `portal_users.id` |
| `approved_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `generated_by_id` → `portal_users.id`
- `approved_by_id` → `portal_users.id`

### Unique Constraints

- `sheet_number`

---

## wage_sheet_entries

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `wage_sheet_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `wage_sheets.id` |
| `labour_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `labour.id` |
| `days_worked` | `numeric(5,2)` | ✗ | `-` | - |
| `daily_wage` | `numeric(10,2)` | ✗ | `-` | - |
| `overtime_hours` | `numeric(5,2)` | ✓ | `0` | - |
| `overtime_amount` | `numeric(10,2)` | ✓ | `0` | - |
| `additions` | `numeric(10,2)` | ✓ | `0` | - |
| `deductions` | `numeric(10,2)` | ✓ | `0` | - |
| `advances_deducted` | `numeric(10,2)` | ✓ | `0` | - |
| `net_payable` | `numeric(15,2)` | ✗ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `wage_sheet_id` → `wage_sheets.id`
- `labour_id` → `labour.id`

---

## labour_advances

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `labour_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `labour.id` |
| `amount` | `numeric(15,2)` | ✗ | `-` | - |
| `advance_date` | `date` | ✗ | `-` | - |
| `reason` | `character varying(255)` | ✓ | `-` | - |
| `is_recovered` | `boolean` | ✗ | `false` | - |
| `recovered_amount` | `numeric(15,2)` | ✓ | `0` | - |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `labour_id` → `labour.id`
