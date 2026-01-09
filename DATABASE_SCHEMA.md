# WallDot Builders - Database Schema Documentation
**Total Tables:** 61
**Database:** PostgreSQL (wdTestDB)

## Table of Contents
1. [activity_feeds](#activity-feeds)
2. [activity_types](#activity-types)
3. [boq_items](#boq-items)
4. [boq_work_types](#boq-work-types)
5. [cctv_cameras](#cctv-cameras)
6. [customer_projects](#customer-projects)
7. [customer_refresh_tokens](#customer-refresh-tokens)
8. [customer_roles](#customer-roles)
9. [customer_users](#customer-users)
10. [design_package_payments](#design-package-payments) *(NEW)*
11. [design_steps](#design-steps)
12. [document_categories](#document-categories)
13. [feedback_forms](#feedback-forms)
14. [feedback_responses](#feedback-responses)
15. [gallery_images](#gallery-images)
16. [leads](#leads)
17. [lead_quotations](#lead-quotations) *(NEW - V1_22)*
18. [lead_quotation_items](#lead-quotation_items) *(NEW - V1_22)*
19. [lead_interactions](#lead-interactions) *(NEW - V1_23)*
20. [observations](#observations)
21. [partnership_users](#partnership-users)
22. [payment_schedule](#payment-schedule) *(NEW)*
23. [payment_transactions](#payment-transactions) *(NEW)*
24. [portal_permissions](#portal-permissions)
25. [portal_refresh_tokens](#portal-refresh-tokens)
26. [portal_role_permissions](#portal-role-permissions)
27. [portal_roles](#portal-roles)
28. [portal_users](#portal-users)
29. [project_design_steps](#project-design-steps)
30. [project_documents](#project-documents)
31. [project_members](#project-members)
32. [project_queries](#project-queries)
33. [quality_checks](#quality-checks)
34. [retention_releases](#retention-releases) *(NEW)*
35. [site_reports](#site-reports)
36. [site_visits](#site-visits)
37. [sqft_categories](#sqft-categories)
38. [staff_roles](#staff-roles)
39. [task_assignment_history](#task-assignment-history) *(NEW)*
40. [tasks](#tasks)
41. [tax_invoices](#tax-invoices) *(NEW)*
42. [view_360](#view-360)
43. [challan_sequences](#challan_sequences) *(NEW)*
44. [payment_challans](#payment_challans) *(NEW)*
45. [vendors](#vendors) *(Standardized)*
46. [materials](#materials) *(Standardized)*
47. [purchase_orders](#purchase_orders) *(Standardized)*
48. [purchase_order_items](#purchase_order-items) *(Standardized)*
49. [goods_received_notes](#goods-received-notes) *(Standardized)*
50. [inventory_stock](#inventory-stock) *(Standardized)*
51. [stock_adjustments](#stock-adjustments) *(Standardized)*
52. [subcontract_work_orders](#subcontract-work-orders) *(Standardized)*
53. [vendor_payments](#vendor-payments) *(Standardized)*
54. [material_budgets](#material-budgets) *(Standardized)*
55. [project_phases](#project-phases) *(NEW)*
56. [project_variations](#project-variations) *(Standardized)*
57. [project_warranties](#project-warranties) *(Standardized)*
58. [labour](#labour) *(Standardized)*
59. [labour_attendance](#labour-attendance) *(Standardized)*
60. [labour_payments](#labour-payments) *(Standardized)*
61. [measurement_book](#measurement-book) *(Standardized)*



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
| `material_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `materials.id` *(V1_36)* Optional link for cost tracking |
| `total_amount` | `numeric(10,2)` | ✓ | `-` | - |
| `unit_rate` | `numeric(10,2)` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `created_by_id` → `customer_users.id`
- `work_type_id` → `boq_work_types.id`
- `project_id` → `customer_projects.id`
- `material_id` → `materials.id` *(V1_36)* ON DELETE SET NULL


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
- `project_manager_id` → `portal_users.id` *(V1_35)*
- `created_by_user_id` → `portal_users.id` *(V1_35)*
- `updated_by_user_id` → `portal_users.id` *(V1_35)*
- `deleted_by_user_id` → `portal_users.id` *(V1_35)*

### Unique Constraints

- `project_uuid`

### Check Constraints *(V1_35)*

- `chk_project_status` → `project_status` IN ('ACTIVE', 'COMPLETED', 'SUSPENDED', 'CANCELLED', 'ON_HOLD')

### Indexes *(V1_35)*

- `idx_projects_deleted_at` → Partial index on `deleted_at IS NULL` for active projects
- `idx_projects_active_phase` → Composite index on `(id, project_phase, project_status)` WHERE `deleted_at IS NULL`
- `idx_projects_manager` → Index on `project_manager_id` WHERE `deleted_at IS NULL`
- `idx_projects_customer_active` → Index on `customer_id` WHERE `deleted_at IS NULL`
- `idx_projects_status` → Index on `project_status`
- `idx_projects_version` → Composite index on `(id, version)` for optimistic locking

### Notes

**Enum Fields**: `project_phase`, `contract_type`, `permit_status`, and `project_status` are stored as VARCHAR but validated as enums in application layer (JPA @Enumerated).

**Audit Trail**: Full audit trail implemented in V1_35 with user references for create/update/delete operations.

**Soft Delete**: Projects use soft delete pattern via `deleted_at` timestamp. Queries should filter WHERE `deleted_at IS NULL` for active records.

**Optimistic Locking**: The `version` column prevents lost updates in concurrent scenarios using JPA @Version annotation.

---

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
| `phone` | `character varying(20)` | ✓ | `-` | - |
| `whatsapp_number` | `character varying(20)` | ✓ | `-` | - |
| `address` | `text` | ✓ | `-` | - |
| `company_name` | `character varying(255)` | ✓ | `-` | - |
| `gst_number` | `character varying(50)` | ✓ | `-` | - |
| `lead_source` | `character varying(50)` | ✓ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |

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

**Construction Lead Management**  
Standardized on the `Lead` entity within the unified security model. Capture initial customer inquiries and tracks them through the conversion pipeline.

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
| `assigned_to_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` (V1_18) |
| `plot_area` | `numeric(10,2)` | ✓ | `-` | (V1_32) |
| `floors` | `integer` | ✓ | `-` | (V1_32) |

### Foreign Keys

- `assigned_to_id` → `portal_users.id`

### Related Tables

- **tasks**: Pre-sales tasks can be linked directly to a lead via `tasks.lead_id`.
- **customer_projects**: Successfully converted leads are linked to projects via `customer_projects.lead_id`.

---

---


---

## lead_interactions

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval` | 🔑 PK |
| `lead_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `leads.lead_id` |
| `interaction_type` | `character varying(50)` | ✗ | `-` | 'CALL', 'EMAIL', 'MEETING', etc. |
| `interaction_date` | `timestamp without time zone` | ✗ | `now()` | - |
| `duration_minutes` | `integer` | ✓ | `-` | - |
| `subject` | `character varying(255)` | ✓ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |
| `outcome` | `character varying(100)` | ✓ | `-` | - |
| `next_action` | `character varying(255)` | ✓ | `-` | - |
| `next_action_date` | `timestamp without time zone` | ✓ | `-` | - |
| `location` | `character varying(255)` | ✓ | `-` | - |
| `metadata` | `text` | ✓ | `-` | - |
| `created_by_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `portal_users.id` |
| `created_at` | `timestamp without time zone` | ✗ | `now()` | - |

### Primary Key

- `id`

### Foreign Keys

- `lead_id` → `leads.lead_id`
- `created_by_id` → `portal_users.id`

---

## lead_quotations

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval` | 🔑 PK |
| `lead_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `leads.lead_id` |
| `quotation_number` | `character varying(50)` | ✗ | `-` | 🔒 UNIQUE |
| `version` | `integer` | ✗ | `1` | - |
| `title` | `character varying(255)` | ✗ | `-` | - |
| `description` | `text` | ✓ | `-` | - |
| `total_amount` | `numeric(12,2)` | ✗ | `-` | - |
| `tax_amount` | `numeric(12,2)` | ✓ | `-` | - |
| `discount_amount` | `numeric(12,2)` | ✓ | `-` | - |
| `final_amount` | `numeric(12,2)` | ✗ | `-` | - |
| `validity_days` | `integer` | ✓ | `30` | - |
| `status` | `character varying(50)` | ✗ | `'DRAFT'` | 'DRAFT', 'SENT', 'ACCEPTED', etc. |
| `sent_at` | `timestamp without time zone` | ✓ | `-` | - |
| `viewed_at` | `timestamp without time zone` | ✓ | `-` | - |
| `responded_at` | `timestamp without time zone` | ✓ | `-` | - |
| `created_by_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `created_at` | `timestamp without time zone` | ✗ | `now()` | - |
| `updated_at` | `timestamp without time zone` | ✗ | `now()` | - |
| `notes` | `text` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `lead_id` → `leads.lead_id`
- `created_by_id` → `portal_users.id`

### Unique Constraints

- `quotation_number`

---

## lead_quotation_items

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint(64,0)` | ✗ | `nextval` | 🔑 PK |
| `quotation_id` | `bigint(64,0)` | ✗ | `-` | 🔗 FK → `lead_quotations.id` |
| `item_number` | `integer` | ✗ | `-` | - |
| `description` | `text` | ✗ | `-` | - |
| `quantity` | `numeric(10,2)` | ✗ | `1` | - |
| `unit_price` | `numeric(12,2)` | ✗ | `-` | - |
| `total_price` | `numeric(12,2)` | ✗ | `-` | - |
| `notes` | `text` | ✓ | `-` | - |

### Primary Key

- `id`

### Foreign Keys

- `quotation_id` → `lead_quotations.id`

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

Unified polymorphic document table for all system attachments.

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
| `reference_id` | `bigint(64,0)` | ✗ | `-` | Polymorphic ID (Lead ID, Project ID, etc.) |
| `reference_type` | `character varying(50)` | ✗ | `-` | Polymorphic Type ('LEAD', 'PROJECT') |
| `is_active` | `boolean` | ✗ | `true` | Soft delete flag |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✓ | `-` | - |
| `created_by_user_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `integer(32,0)` | ✗ | `1` | Optimistic locking |

### Primary Key

- `id`

### Foreign Keys

- `category_id` → `document_categories.id`
- `created_by_user_id` → `portal_users.id`
- `updated_by_user_id` → `portal_users.id`
- `deleted_by_user_id` → `portal_users.id`

### Indexes

- `idx_project_documents_ref` on `(reference_id, reference_type)`
- `idx_project_documents_category` on `category_id`

---

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

**Lead Estimation Logic**  
Used for calculating initial estimates based on square footage. Standardized on the `SqftCategories` entity in the unified model package.

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
| `lead_id` | `bigint(64,0)` | ✓ | `-` | 🔗 FK → `leads.lead_id` (V1_19) - For pre-sales tasks |
| `due_date` | `date` | ✗ | `-` | **⚠️ MANDATORY** - Required for task accountability and timeline tracking. Must be >= created_at date. |
| `created_at` | `timestamp without time zone` | ✗ | `CURRENT_TIMESTAMP` | - |
| `updated_at` | `timestamp without time zone` | ✓ | `CURRENT_TIMESTAMP` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `lead_id` → `leads.lead_id`
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

Masters for material suppliers and labor contractors. Standardized with `BaseEntity` audit trail (V1_47).

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
| `vendor_type` | `varchar(50)` | ✗ | `-` | Enum: MATERIAL, LABOUR, BOTH, CONSULTANT, SERVICE_PROVIDER |
| `bank_name` | `varchar(255)` | ✓ | `-` | - |
| `account_number` | `varchar(50)` | ✓ | `-` | - |
| `ifsc_code` | `varchar(20)` | ✓ | `-` | - |
| `is_active` | `boolean` | ✗ | `true` | - |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | Soft delete support |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | Optimistic locking |

---

## purchase_orders

Project-specific material or labor purchase orders. Standardized with `BaseEntity` audit trail (V1_47).

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
| `status` | `varchar(20)` | ✗ | `'DRAFT'` | Enum: DRAFT, PENDING_APPROVAL, APPROVED, SENT_TO_VENDOR, PARTIALLY_RECEIVED, RECEIVED, CANCELLED, CLOSED |
| `notes` | `text` | ✓ | `-` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` (Renamed from created_by_id) |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

---

## purchase_order_items

Line items within a Purchase Order. Standardized with `BaseEntity` audit trail (V1_47).

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `po_id` | `bigint` | ✗ | `-` | 🔗 FK → `purchase_orders.id` ON DELETE CASCADE |
| `description` | `varchar(255)` | ✗ | `-` | Material/Work name |
| `quantity` | `numeric(15,2)` | ✗ | `-` | - |
| `unit` | `varchar(50)` | ✗ | `-` | Enum: BAG, KG, MT, CFT, SQFT, NOS, CUM, LTR, etc. |
| `rate` | `numeric(15,2)` | ✗ | `-` | - |
| `gst_percentage` | `numeric(5,2)` | ✗ | `18.00` | - |
| `amount` | `numeric(15,2)` | ✗ | `-` | (Qty * Rate) |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

---

## goods_received_notes (GRN)

Records of material actually received at site against a PO. Standardized with `BaseEntity` (V1_47).

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
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

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

## project_variations *(Standardized)*

Change orders and additional work requests from clients. Standardized with `BaseEntity` (V1_48).

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
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `1` | Optimistic locking |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `approved_by_id` → `portal_users.id`
- `created_by_user_id` → `portal_users.id`
- `updated_by_user_id` → `portal_users.id`
- `deleted_by_user_id` → `portal_users.id`

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `approved_by_id` → `portal_users.id`
- `created_by_user_id` → `portal_users.id`
- `updated_by_user_id` → `portal_users.id`
- `deleted_by_user_id` → `portal_users.id`

---

## stock_adjustments

Records of material wastage, theft, damage, and inventory corrections. Standardized with `BaseEntity` (V1_47).

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `material_id` | `bigint` | ✗ | `-` | 🔗 FK → `materials.id` |
| `adjustment_type` | `varchar(30)` | ✗ | `-` | 'WASTAGE', 'THEFT', 'DAMAGE', 'CORRECTION', 'TRANSFER_OUT' |
| `quantity` | `numeric(15,2)` | ✗ | `-` | - |
| `reason` | `text` | ✓ | `-` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` (Renamed from adjusted_by_id) |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `material_id` → `materials.id`
- `adjusted_by_id` → `portal_users.id`

---

## subcontract_work_orders

Tracks piece-rate and lump-sum subcontractor agreements. Standardized with `BaseEntity` (V1_47).

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `work_order_number` | `varchar(50)` | ✗ | `-` | 🔒 UNIQUE (WAL/SC/YY/NNN) |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `vendor_id` | `bigint` | ✗ | `-` | 🔗 FK → `vendors.id` (vendor_type = 'LABOUR') |
| `scope_description` | `text` | ✗ | `-` | Work scope |
| `measurement_basis` | `varchar(20)` | ✗ | `'UNIT_RATE'` | Enum: LUMPSUM, UNIT_RATE |
| `negotiated_amount` | `numeric(15,2)` | ✗ | `-` | Total contract value |
| `status` | `varchar(20)` | ✗ | `'DRAFT'` | Enum: DRAFT, ISSUED, IN_PROGRESS, COMPLETED, TERMINATED |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` (Renamed from created_by_id) |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

---

## materials

Master record for all construction materials. Standardized with `BaseEntity` (V1_47).

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `name` | `varchar(255)` | ✗ | `-` | 🔒 UNIQUE |
| `description` | `text` | ✓ | `-` | - |
| `unit` | `varchar(20)` | ✗ | `-` | Enum: BAG, KG, MT, CFT, SQFT, NOS, CUM, LTR |
| `category` | `varchar(50)` | ✗ | `-` | Enum: CEMENT, STEEL, AGGREGATE, BRICK, ELECTRICAL, PLUMBING, PAINTING, FINISHING, OTHER |
| `is_active` | `boolean` | ✗ | `true` | - |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

---

## inventory_stock

Real-time stock levels of materials across different projects/sites. Standardized with `BaseEntity` (V1_47).

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `material_id` | `bigint` | ✗ | `-` | 🔗 FK → `materials.id` |
| `current_stock` | `numeric(15,2)` | ✗ | `0` | - |
| `min_stock_level` | `numeric(15,2)` | ✓ | `0` | Reorder trigger level |
| `updated_at` | `timestamp` | ✗ | `now()` | Renamed from last_updated |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

---

## subcontract_measurements *(NEW)*

Progress measurements for unit-rate subcontracts.

---

## subcontract_payments *(NEW)*

Payment records for subcontractors with TDS calculation (Section 194C).

---

## vendor_payments

Tracks all payments made to vendors against purchase invoices. Standardized with `BaseEntity` (V1_47).

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `invoice_id` | `bigint` | ✗ | `-` | 🔗 FK → `purchase_invoices.id` |
| `payment_date` | `date` | ✗ | `-` | - |
| `amount_paid` | `numeric(15,2)` | ✗ | `-` | - |
| `tds_deducted` | `numeric(15,2)` | ✓ | `0` | - |
| `other_deductions` | `numeric(15,2)` | ✓ | `0` | - |
| `net_paid` | `numeric(15,2)` | ✗ | `-` | - |
| `payment_mode` | `varchar(20)` | ✗ | `-` | CASH, CHEQUE, NEFT, RTGS, UPI |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` (Renamed from paid_by_id) |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

---

## material_budgets

Standardized on the `MaterialBudget` entity. Tracks budgeted vs actual material consumption per project.

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `material_id` | `bigint` | ✗ | `-` | 🔗 FK → `materials.id` |
| `budgeted_quantity` | `numeric(15,2)` | ✗ | `-` | - |
| `estimated_rate` | `numeric(15,2)` | ✓ | `-` | - |
| `total_budget` | `numeric(15,2)` | ✓ | `-` | Auto-calculated |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `0` | - |

## project_warranties *(Standardized)*

Tracks warranties for project components provided by vendors or manufacturers. Standardized with `BaseEntity` (V1_48).

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `nextval` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `component_name` | `varchar(255)` | ✗ | `-` | e.g., 'Waterproofing', 'Structure' |
| `description` | `text` | ✓ | `-` | - |
| `provider_name` | `varchar(255)` | ✓ | `-` | - |
| `start_date` | `date` | ✓ | `-` | - |
| `end_date` | `date` | ✓ | `-` | - |
| `status` | `varchar(20)` | ✗ | `'ACTIVE'` | 'ACTIVE', 'EXPIRED', 'VOID' |
| `coverage_details` | `text` | ✓ | `-` | - |
| `created_at` | `timestamp` | ✗ | `now()` | - |
| `updated_at` | `timestamp` | ✗ | `now()` | - |
| `created_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `updated_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `deleted_at` | `timestamp` | ✓ | `-` | - |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | 🔗 FK → `portal_users.id` |
| `version` | `bigint` | ✗ | `1` | Optimistic locking |

### Primary Key

- `id`

### Foreign Keys

- `project_id` → `customer_projects.id`
- `created_by_user_id` → `portal_users.id`
- `updated_by_user_id` → `portal_users.id`
- `deleted_by_user_id` → `portal_users.id`

---

## labour

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `-` | 🔑 PK |
| `name` | `character varying(255)` | ✗ | `-` | - |
| `phone` | `character varying(20)` | ✗ | `-` | Unique |
| `trade_type` | `character varying(50)` | ✗ | `-` | Enum: `LabourTradeType` |
| `id_proof_type` | `character varying(50)` | ✓ | `-` | Enum: `IdProofType` |
| `id_proof_number` | `character varying(255)` | ✓ | `-` | - |
| `daily_wage` | `numeric(15,2)` | ✗ | `-` | - |
| `emergency_contact` | `character varying(255)` | ✓ | `-` | - |
| `is_active` | `boolean` | ✗ | `true` | - |
| `created_at` | `timestamp` | ✗ | `now()` | Audit |
| `updated_at` | `timestamp` | ✗ | `now()` | Audit |
| `created_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `updated_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `deleted_at` | `timestamp` | ✓ | `-` | Audit |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `version` | `bigint` | ✗ | `1` | Lucking |

### Constraints

- `chk_labour_trade_type`: `trade_type` IN ('CARPENTER', 'PLUMBER', 'ELECTRICIAN', 'MASON', 'HELPER', 'PAINTER', 'TILER', 'WELDER', 'OTHER')
- `chk_labour_id_proof_type`: `id_proof_type` IN ('AADHAAR', 'PAN', 'VOTER_ID', 'DRIVING_LICENSE', 'OTHER')

---

## labour_attendance

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `-` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `labour_id` | `bigint` | ✗ | `-` | 🔗 FK → `labour.id` |
| `attendance_date` | `date` | ✗ | `-` | - |
| `status` | `character varying(20)` | ✗ | `-` | Enum: `AttendanceStatus` |
| `hours_worked` | `double precision` | ✓ | `-` | - |
| `created_at` | `timestamp` | ✗ | `now()` | Audit |
| `updated_at` | `timestamp` | ✗ | `now()` | Audit |
| `created_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `updated_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `deleted_at` | `timestamp` | ✓ | `-` | Audit |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `version` | `bigint` | ✗ | `1` | Lucking |

### Constraints

- `chk_attendance_status`: `status` IN ('PRESENT', 'ABSENT', 'HALF_DAY', 'LEAVE')

---

## labour_payments

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `-` | 🔑 PK |
| `labour_id` | `bigint` | ✗ | `-` | 🔗 FK → `labour.id` |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `mb_entry_id` | `bigint` | ✓ | `-` | 🔗 FK → `measurement_book.id` |
| `amount` | `numeric(15,2)` | ✗ | `-` | - |
| `payment_date` | `date` | ✗ | `-` | - |
| `payment_method` | `character varying(50)` | ✓ | `-` | Enum: `PaymentMethod` |
| `notes` | `text` | ✓ | `-` | - |
| `created_at` | `timestamp` | ✗ | `now()` | Audit |
| `updated_at` | `timestamp` | ✗ | `now()` | Audit |
| `created_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `updated_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `deleted_at` | `timestamp` | ✓ | `-` | Audit |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `version` | `bigint` | ✗ | `1` | Lucking |

### Constraints

- `chk_labour_payment_method`: `payment_method` IN ('CASH', 'BANK_TRANSFER', 'UPI', 'CHEQUE')

---

## measurement_book

### Columns

| Column Name | Data Type | Nullable | Default | Notes |
|-------------|-----------|----------|---------|-------|
| `id` | `bigint` | ✗ | `-` | 🔑 PK |
| `project_id` | `bigint` | ✗ | `-` | 🔗 FK → `customer_projects.id` |
| `labour_id` | `bigint` | ✓ | `-` | 🔗 FK → `labour.id` |
| `boq_item_id` | `bigint` | ✓ | `-` | 🔗 FK → `boq_items.id` |
| `description` | `character varying(255)` | ✗ | `-` | - |
| `measurement_date` | `date` | ✗ | `-` | - |
| `length` | `numeric(10,2)` | ✓ | `-` | - |
| `breadth` | `numeric(10,2)` | ✓ | `-` | - |
| `depth` | `numeric(10,2)` | ✓ | `-` | - |
| `quantity` | `numeric(10,2)` | ✗ | `-` | - |
| `unit` | `character varying(50)` | ✗ | `-` | - |
| `rate` | `numeric(15,2)` | ✓ | `-` | - |
| `total_amount` | `numeric(15,2)` | ✓ | `-` | - |
| `created_at` | `timestamp` | ✗ | `now()` | Audit |
| `updated_at` | `timestamp` | ✗ | `now()` | Audit |
| `created_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `updated_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `deleted_at` | `timestamp` | ✓ | `-` | Audit |
| `deleted_by_user_id` | `bigint` | ✓ | `-` | Audit |
| `version` | `bigint` | ✗ | `1` | Lucking |
