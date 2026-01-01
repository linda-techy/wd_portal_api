# WallDot Builders - Database Schema Documentation
**Total Tables:** 44
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
14. [design_package_payments](#design-package-payments) *(NEW)*
15. [design_steps](#design-steps)
16. [document_categories](#document-categories)
17. [feedback_forms](#feedback-forms)
18. [feedback_responses](#feedback-responses)
19. [gallery_images](#gallery-images)
20. [leads](#leads)
21. [observations](#observations)
22. [partnership_users](#partnership-users)
23. [payment_schedule](#payment-schedule) *(NEW)*
24. [payment_transactions](#payment-transactions) *(NEW)*
25. [portal_permissions](#portal-permissions)
26. [portal_project_documents](#portal-project-documents)
27. [portal_refresh_tokens](#portal-refresh-tokens)
28. [portal_role_permissions](#portal-role-permissions)
29. [portal_roles](#portal-roles)
30. [portal_users](#portal-users)
31. [project_design_steps](#project-design-steps)
32. [project_documents](#project-documents)
33. [project_members](#project-members)
34. [project_queries](#project-queries)
35. [quality_checks](#quality-checks)
36. [retention_releases](#retention-releases) *(NEW)*
37. [site_reports](#site-reports)
38. [site_visits](#site-visits)
39. [sqft_categories](#sqft-categories)
40. [staff_roles](#staff-roles)
41. [task_assignment_history](#task-assignment-history) *(NEW)*
42. [tasks](#tasks)
43. [tax_invoices](#tax-invoices) *(NEW)*
44. [view_360](#view-360)
45. [challan_sequences](#challan_sequences) *(NEW)*
46. [payment_challans](#payment_challans) *(NEW)*


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
| `project_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_projects.id` |

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
| contract_type | character varying(50) | âœ— | 'TURNKEY' | 'TURNKEY', 'LABOR_ONLY', 'ITEM_RATE', 'COST_PLUS' |
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
| `project_manager_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` Project manager with full task edit rights |

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
| `revoked` | `boolean` | ✗ | `false` | Set to `true` after rotation. If a revoked token is used, all user tokens are invalidated. |
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

### Default Categories (V1_13)
- Floor Plan Layout
- 3D Elevation
- Detailed Project Costing
- Structural Drawings
- MEP Drawings
- Collaboration Agreement
- Site Photos
- Other

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

### Primary Key

- `lead_id`

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

## portal_project_documents

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `description` | `text` | ✓ | `-` | - |
| `file_path` | `character varying(500)` | ✗ | `-` | - |
| `file_size` | `bigint(64,0)` | ✓ | `-` | - |
| `file_type` | `character varying(50)` | ✓ | `-` | - |
| `filename` | `character varying(255)` | ✗ | `-` | - |
| `is_active` | `boolean` | ✓ | `-` | - |
| `upload_date` | `timestamp without time zone` | ✗ | `-` | - |
| `version` | `integer(32,0)` | ✓ | `-` | - |
| `category_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `document_categories.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `uploaded_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `portal_users.id` |

### Primary Key

- `id`

### Foreign Keys

- `category_id` → `document_categories.id`
- `project_id` → `customer_projects.id`
- `uploaded_by_id` → `portal_users.id`

---

## portal_refresh_tokens

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('portal_refresh_tok...` | 🔑 PK |
| `token` | `character varying(4096)` | ✗ | `-` | 🔒 UNIQUE |
| `user_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `portal_users.id` |
| `expiry_date` | `timestamp without time zone` | ✗ | `-` | - |
| `revoked` | `boolean` | ✗ | `false` | Set to `true` after rotation. If a revoked token is used, all user tokens are invalidated. |

### Primary Key

- `id`

### Foreign Keys

- `user_id` → `portal_users.id`

### Unique Constraints

- `token`

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
| `id` | `bigint(64,0)` | ✗ | `-` | 🔑 PK |
| `description` | `text` | ✓ | `-` | - |
| `file_path` | `character varying(500)` | ✗ | `-` | - |
| `file_size` | `bigint(64,0)` | ✓ | `-` | - |
| `file_type` | `character varying(50)` | ✓ | `-` | - |
| `filename` | `character varying(255)` | ✗ | `-` | - |
| `is_active` | `boolean` | ✓ | `-` | - |
| `upload_date` | `timestamp without time zone` | ✗ | `-` | - |
| `version` | `integer(32,0)` | ✓ | `-` | - |
| `category_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `document_categories.id` |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `uploaded_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_users.id` |

### Primary Key

- `id`

### Foreign Keys

- `category_id` → `document_categories.id`
- `project_id` → `customer_projects.id`
- `uploaded_by_id` → `customer_users.id`

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

## task_assignment_history

Audit trail for all task assignments. Tracks who assigned what to whom and when, for full transparency and historical record.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('task_assignment_history_id_seq')` | 🔑 PK |
| `task_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `tasks.id` ON DELETE CASCADE |
| `assigned_from_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` Previous assignee (NULL if unassigned) |
| `assigned_to_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` New assignee (NULL if being unassigned) |
| `assigned_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `portal_users.id` User who made the change |
| `assigned_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | When assignment was made |
| `notes` | `text` | ✓ | `-` | Optional notes about why assignment changed |

### Primary Key

- `id`

### Foreign Keys

- `task_id` → `tasks.id` (ON DELETE CASCADE)
- `assigned_from_id` → `portal_users.id`
- `assigned_to_id` → `portal_users.id`
- `assigned_by_id` → `portal_users.id`

### Indexes

- `idx_task_assignment_history_task` on `task_id`
- `idx_task_assignment_history_assigned_at` on `assigned_at DESC`
- `idx_task_assignment_history_assigned_to` on `assigned_to_id`

---

## tasks

**Production-Grade Task Management System**  
**Business Context:** Construction task tracking with mandatory deadlines for project timeline accountability and proactive alert system.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('tasks_id_seq'::reg...` | 🔑 PK |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `status` | `character varying(255)` | ✗ | `'PENDING'::character varying` | Enum: PENDING, IN_PROGRESS, COMPLETED, CANCELLED |
| `priority` | `character varying(255)` | ✗ | `'MEDIUM'::character varying` | Enum: LOW, MEDIUM, HIGH, URGENT |
| `assigned_to` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `created_by` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `project_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `customer_projects.id` |
| `due_date` | `date` | ✗ | `-` | **⚠️ MANDATORY** - Required for task accountability and timeline tracking. Must be >= created_at date. |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `created_by` → `portal_users.id`
- `assigned_to` → `portal_users.id`

### Constraints

- **`chk_task_due_date_valid`**: Business rule constraint ensuring `due_date >= created_at::date`  
  _Rationale:_ Prevents backdating tasks which violates construction timeline integrity

### Indexes

| Index Name | Columns | Filter | Purpose |
|------------|---------|--------|---------|
| `idx_tasks_overdue` | `(due_date, status)` | `WHERE status NOT IN ('COMPLETED', 'CANCELLED')` | Overdue task queries for manager dashboards |
| `idx_tasks_project_due` | `(project_id, due_date, status)` | `WHERE status NOT IN ('COMPLETED', 'CANCELLED')` | Project timeline views, Gantt charts |
| `idx_tasks_assigned_due` | `(assigned_to, due_date, status)` | `WHERE status NOT IN ('COMPLETED', 'CANCELLED')` | "My Tasks" views, assignee workload |
| `idx_tasks_priority_due` | `(priority, due_date, status)` | `WHERE status NOT IN ('COMPLETED', 'CANCELLED')` | Priority-based alerts, escalation logic |

**Alert System Foundation:** These indexes enable efficient queries for:
- Overdue task detection
- Approaching deadline alerts (due in next 3 days)
- Manager dashboard performance
- Project timeline monitoring

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

## design_package_payments

Master record for a project's design package payment agreement.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('design_package_payments_id_seq')` | 🔑 PK |
| `project_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `customer_projects.id` 🔒 UNIQUE |
| `package_name` | `character varying(50)` | ✗ | `-` | e.g., 'Custom', 'Premium', 'Bespoke' |
| `rate_per_sqft` | `numeric(10,2)` | ✗ | `-` | - |
| `total_sqft` | `numeric(10,2)` | ✗ | `-` | - |
| `base_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `gst_percentage` | `numeric(5,2)` | ✗ | `18.00` | - |
| `gst_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `discount_percentage` | `numeric(5,2)` | ✓ | `0` | Configurable per package |
| `discount_amount` | `numeric(15,2)` | ✓ | `0` | - |
| `total_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `payment_type` | `character varying(20)` | ✗ | `-` | 'FULL' or 'INSTALLMENT' |
| `status` | `character varying(20)` | ✗ | `'PENDING'` | 'PENDING', 'PARTIAL', 'PAID' |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `created_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `retention_percentage` | `numeric(5,2)` | ✗ | `10.00` | Percentage held as retention (5-10%) |
| `retention_amount` | `numeric(15,2)` | ✗ | `0` | Calculated retention amount |
| `retention_released_amount` | `numeric(15,2)` | ✗ | `0` | Total released so far |
| `defect_liability_end_date` | `date` | ✓ | `-` | When retention can be released |
| `retention_status` | `character varying(20)` | ✗ | `'ACTIVE'` | 'ACTIVE', 'PARTIALLY_RELEASED', 'RELEASED' |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `created_by_id` → `portal_users.id`

### Unique Constraints

- `project_id`

---

## payment_schedule

Individual installment records for a design package payment.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('payment_schedule_id_seq')` | 🔑 PK |
| `design_payment_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `design_package_payments.id` |
| `installment_number` | `integer(32,0)` | ✗ | `-` | 1, 2, 3, etc. |
| `description` | `character varying(100)` | ✗ | `-` | e.g., 'Advance', 'Design Phase', 'Post-Design' |
| `amount` | `numeric(15,2)` | ✗ | `-` | - |
| `due_date` | `date` | ✓ | `-` | Milestone-based, optional |
| `status` | `character varying(20)` | ✗ | `'PENDING'` | 'PENDING', 'PAID', 'OVERDUE' |
| `paid_amount` | `numeric(15,2)` | ✓ | `0` | - |
| `paid_date` | `timestamp without time zone` | ✓ | `-` | - |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `design_payment_id` → `design_package_payments.id`

---

## payment_transactions

Actual payment records when money is received.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval('payment_transactions_id_seq')` | 🔑 PK |
| `schedule_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `payment_schedule.id` |
| `amount` | `numeric(15,2)` | ✗ | `-` | - |
| `payment_method` | `character varying(50)` | ✓ | `-` | 'BANK_TRANSFER', 'UPI', 'CHEQUE', 'CASH' |
| `reference_number` | `character varying(100)` | ✓ | `-` | Transaction/cheque number |
| `payment_date` | `timestamp without time zone` | ✗ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |
| `recorded_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `portal_users.id` |
| `receipt_number` | `character varying(50)` | ✓ | `-` | 🔒 UNIQUE (Format: WAL/PAY/YYYY/NNN) |
| `status` | `character varying(20)` | ✗ | `'COMPLETED'` | 'COMPLETED', 'FAILED', 'CANCELLED' |
| `tds_percentage` | `numeric(5,2)` | ✗ | `0` | TDS rate (0-100). Common: 2% for Section 194C |
| `tds_amount` | `numeric(15,2)` | ✗ | `0` | Calculated TDS deduction |
| `net_amount` | `numeric(15,2)` | ✗ | `-` | Amount received after TDS (amount - tds_amount) |
| `tds_deducted_by` | `character varying(50)` | ✗ | `'CUSTOMER'` | 'CUSTOMER', 'SELF', 'NONE' |
| `payment_category` | `character varying(50)` | ✗ | `'PROGRESS'` | 'ADVANCE', 'PROGRESS', 'FINAL', 'RETENTION_RELEASE' |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `schedule_id` → `payment_schedule.id`
- `recorded_by_id` → `portal_users.id`

---

## challan_sequences

Tracks the last used sequence number for each financial year to ensure gapless, sequential numbering.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `fy` | `varchar(10)` | ✗ | `-` | 🔒 UNIQUE (e.g., '2024-25') |
| `last_sequence` | `integer` | ✗ | `0` | Last number issued in this FY |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |

### Primary Key

- `id`

### Unique Constraints

- `fy`

---

## payment_challans

Formal challans generated for financial transactions. Each transaction is eligible for exactly one challan.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `transaction_id` | `bigint` | ✗ | `-` | 🔒 UNIQUE 🔗 FK → `payment_transactions.id` |
| `challan_number` | `varchar(50)` | ✗ | `-` | 🔒 UNIQUE (Format: WAL/CH/FY/NNN) |
| `fy` | `varchar(10)` | ✗ | `-` | Financial Year of issuance |
| `sequence_number` | `integer` | ✗ | `-` | Sequential number within the FY |
| `transaction_date` | `timestamp` | ✗ | `-` | Denormalized for efficient range queries |
| `generated_at` | `timestamp` | ✗ | `now()` | - |
| `generated_by_id` | `bigint` | ✗ | `-` | 🔗 FK → `portal_users.id` |
| `status` | `varchar(20)` | ✗ | `'ISSUED'` | 'ISSUED', 'CANCELLED' |

### Primary Key

- `id`

### Foreign Keys

- `transaction_id` → `payment_transactions.id`
- `generated_by_id` → `portal_users.id`

### Unique Constraints

- `transaction_id`
- `challan_number`

### Indexes

- `idx_payment_challans_fy` on `fy`
- `idx_payment_challans_date` on `transaction_date`

---

## vendors

Masters for material suppliers and labor contractors.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `name` | `varchar(255)` | ✗ | `-` | - |
| `contact_person` | `varchar(255)` | ✓ | `-` | - |
| `phone` | `varchar(20)` | ✗ | `-` | 🔒 UNIQUE |
| `email` | `varchar(255)` | ✓ | `-` | 🔒 UNIQUE |
| `gstin` | `varchar(15)` | ✓ | `-` | 🔒 UNIQUE |
| `address` | `text` | ✓ | `-` | - |
| `vendor_type` | `varchar(50)` | ✗ | `-` | 'MATERIAL', 'LABOUR', 'SERVICES' |
| `bank_name` | `varchar(255)` | ✓ | `-` | - |
| `account_number` | `varchar(50)` | ✓ | `-` | - |
| `ifsc_code` | `varchar(20)` | ✓ | `-` | - |
| `is_active` | `boolean` | ✗ | `true` | - |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |

---

## purchase_orders

Project-specific material or labor purchase orders.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `po_number` | `varchar(50)` | ✗ | `-` | 🔒 UNIQUE (WAL/PO/YY/NNN) |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `vendor_id` | `bigint` | ✗ | `-` | 🔗 FK → `vendors.id` |
| `po_date` | `date` | ✗ | `-` | - |
| `expected_delivery_date` | `date` | ✓ | `-` | - |
| `total_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `gst_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `net_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `status` | `varchar(20)` | ✗ | `'DRAFT'` | 'DRAFT', 'ISSUED', 'RECEIVED', 'CANCELLED' |
| `notes` | `text` | ✓ | `-` | - |
| `created_by_id` | `bigint` | ✗ | `-` | 🔗 FK → `portal_users.id` |
| `created_at` | `timestamp` | ✗ | `now()` | - |

---

## purchase_order_items

Line items within a Purchase Order.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `po_id` | `bigint` | ✗ | `-` | 🔗 FK → `purchase_orders.id` ON DELETE CASCADE |
| `description` | `varchar(255)` | ✗ | `-` | Material/Work name |
| `quantity` | `numeric(15,2)` | ✗ | `-` | - |
| `unit` | `varchar(50)` | ✗ | `-` | - |
| `rate` | `numeric(15,2)` | ✗ | `-` | - |
| `gst_percentage` | `numeric(5,2)` | ✗ | `18.00` | - |
| `amount` | `numeric(15,2)` | ✗ | `-` | (Qty * Rate) |

---

## goods_received_notes (GRN)

Records of material actually received at site against a PO.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `grn_number` | `varchar(50)` | ✗ | `-` | 🔒 UNIQUE (WAL/GRN/YY/NNN) |
| `po_id` | `bigint` | ✗ | `-` | 🔗 FK → `purchase_orders.id` |
| `received_date` | `timestamp` | ✗ | `now()` | - |
| `received_by_id` | `bigint` | ✗ | `-` | 🔗 FK → `portal_users.id` |
| `invoice_number` | `varchar(100)` | ✓ | `-` | Vendor's Invoice Refernece |
| `invoice_date` | `date` | ✓ | `-` | - |
| `challan_number` | `varchar(100)` | ✓ | `-` | Delivery Challan Reference |
| `notes` | `text` | ✓ | `-` | - |

---

## project_phases *(NEW)*

Formal tracking of construction phases with planned vs actual timelines.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `phase_name` | `varchar(100)` | ✗ | `-` | e.g., 'Foundation', 'Shuttering', 'Plaster' |
| `planned_start` | `date` | ✓ | `-` | - |
| `planned_end` | `date` | ✓ | `-` | - |
| `actual_start` | `date` | ✓ | `-` | - |
| `actual_end` | `date` | ✓ | `-` | - |
| `status` | `varchar(20)` | ✗ | `'NOT_STARTED'` | 'NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'DELAYED' |
| `display_order` | `integer` | ✓ | `-` | - |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`

---

## delay_logs *(NEW)*

Records of project delays with categorized reasons for EOT (Extension of Time) documentation.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `phase_id` | `bigint` | ✓ | `-` | 🔗 FK → `project_phases.id` |
| `delay_type` | `varchar(50)` | ✗ | `-` | 'WEATHER', 'LABOUR_STRIKE', 'MATERIAL_DELAY', 'CLIENT_APPROVAL', 'OTHER' |
| `from_date` | `date` | ✗ | `-` | - |
| `to_date` | `date` | ✓ | `-` | - |
| `reason_text` | `text` | ✓ | `-` | - |
| `logged_by_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `created_at` | `timestamp` | ✗ | `now()` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `phase_id` → `project_phases.id`
- `logged_by_id` → `portal_users.id`

---

## project_variations *(NEW)*

Change orders and additional work requests from clients.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `description` | `text` | ✗ | `-` | - |
| `estimated_amount` | `numeric(15,2)` | ✗ | `-` | - |
| `client_approved` | `boolean` | ✓ | `false` | - |
| `approved_by_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `approved_at` | `timestamp` | ✓ | `-` | - |
| `status` | `varchar(20)` | ✗ | `'DRAFT'` | 'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED' |
| `notes` | `text` | ✓ | `-` | - |
| `created_by_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `approved_by_id` → `portal_users.id`
- `created_by_id` → `portal_users.id`

---

## stock_adjustments *(NEW)*

Records of material wastage, theft, damage, and inventory corrections.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `material_id` | `bigint` | ✗ | `-` | 🔗 FK → `materials.id` |
| `adjustment_type` | `varchar(30)` | ✗ | `-` | 'WASTAGE', 'THEFT', 'DAMAGE', 'CORRECTION', 'TRANSFER_OUT' |
| `quantity` | `numeric(15,2)` | ✗ | `-` | - |
| `reason` | `text` | ✓ | `-` | - |
| `adjusted_by_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `adjusted_at` | `timestamp` | ✗ | `now()` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `material_id` → `materials.id`
- `adjusted_by_id` → `portal_users.id`

---

 
 # #   s u b c o n t r a c t _ w o r k _ o r d e r s   * ( N E W ) * 
 
 T r a c k s   p i e c e - r a t e   a n d   l u m p - s u m   s u b c o n t r a c t o r   a g r e e m e n t s .   E n a b l e s   m a n a g e m e n t   o f   6 0 - 7 0 % %   o f   c o n s t r u c t i o n   w o r k . 
 
 # # #   C o l u m n s 
 
 |   C o l u m n   N a m e   |   D a t a   T y p e   |   N u l l a b l e   |   D e f a u l t   |   N o t e s   | 
 | - - - - - - - - - - - - - | - - - - - - - - - - - | - - - - - - - - - - | - - - - - - - - - | - - - - - - - | 
 |   ` i d `   |   ` b i g i n t `   |   '  |   ` n e x t v a l `   |   =��  P K   | 
 |   ` w o r k _ o r d e r _ n u m b e r `   |   ` v a r c h a r ( 5 0 ) `   |   '  |   ` - `   |   =��  U N I Q U E   ( W A L / S C / Y Y / N N N )   | 
 |   ` p r o j e c t _ i d `   |   ` b i g i n t `   |   '  |   ` - `   |   =��  F K   �!  ` c u s t o m e r _ p r o j e c t s . i d `   | 
 |   ` v e n d o r _ i d `   |   ` b i g i n t `   |   '  |   ` - `   |   =��  F K   �!  ` v e n d o r s . i d `   ( v e n d o r _ t y p e = ' L A B O U R ' )   | 
 |   ` s c o p e _ d e s c r i p t i o n `   |   ` t e x t `   |   '  |   ` - `   |   W o r k   s c o p e   | 
 |   ` m e a s u r e m e n t _ b a s i s `   |   ` v a r c h a r ( 2 0 ) `   |   '  |   ` ' U N I T _ R A T E ' `   |   ' L U M P S U M ' ,   ' U N I T _ R A T E '   | 
 |   ` n e g o t i a t e d _ a m o u n t `   |   ` n u m e r i c ( 1 5 , 2 ) `   |   '  |   ` - `   |   T o t a l   c o n t r a c t   v a l u e   | 
 |   ` s t a t u s `   |   ` v a r c h a r ( 2 0 ) `   |   '  |   ` ' D R A F T ' `   |   ' D R A F T ' ,   ' I S S U E D ' ,   ' I N _ P R O G R E S S ' ,   ' C O M P L E T E D ' ,   ' T E R M I N A T E D '   | 
 |   ` c r e a t e d _ a t `   |   ` t i m e s t a m p `   |   '  |   ` n o w ( ) `   |   -   | 
 
 - - - 
 
 # #   s u b c o n t r a c t _ m e a s u r e m e n t s   * ( N E W ) * 
 
 P r o g r e s s   m e a s u r e m e n t s   f o r   u n i t - r a t e   s u b c o n t r a c t s . 
 
 - - - 
 
 # #   s u b c o n t r a c t _ p a y m e n t s   * ( N E W ) * 
 
 P a y m e n t   r e c o r d s   f o r   s u b c o n t r a c t o r s   w i t h   T D S   c a l c u l a t i o n   ( S e c t i o n   1 9 4 C ) . 
 
 - - - 
 
 
