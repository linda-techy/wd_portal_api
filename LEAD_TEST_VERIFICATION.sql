-- Lead Module Test - SQL Verification Scripts
-- Run these queries after each test case to verify results

-- ========================================
-- TEST CASE 1: Verify Lead Creation
-- ========================================

-- Check if lead was created
SELECT 
    lead_id,
    name,
    email,
    phone,
    customer_type,
    lead_source,
    lead_status,
    priority,
    budget,
    project_sqft_area,
    score,
    score_category,
    created_at
FROM leads 
WHERE email = 'rajesh.test001@example.com';

-- Expected: 1 row returned with all fields populated

-- ========================================
-- TEST CASE 3: Verify Lead Update
-- ========================================

-- Check updated fields
SELECT 
    name,
    phone,
    priority,
    lead_status,
    notes,
    updated_at
FROM leads 
WHERE email = 'rajesh.test001@example.com';

-- Expected: phone = '9999888877', priority and status should be updated

-- ========================================
-- TEST CASE 6: Verify Lead Assignment
-- ========================================

-- Check assignment
SELECT 
    l.name,
    l.assigned_team,
    l.assigned_to_id,
    pu.first_name || ' ' || pu.last_name as assigned_user_name
FROM leads l
LEFT JOIN portal_users pu ON l.assigned_to_id = pu.user_id
WHERE l.email = 'rajesh.test001@example.com';

-- Expected: assigned_to_id should be populated

-- ========================================
-- TEST CASE 7: Verify Lead Scoring
-- ========================================

-- Check all test leads with their scores
SELECT 
    name,
    email,
    budget,
    lead_source,
    score,
    score_category,
    CASE 
        WHEN score >= 61 THEN 'Should be HOT'
        WHEN score >= 31 THEN 'Should be WARM'
        ELSE 'Should be COLD'
    END as expected_category
FROM leads 
WHERE email IN (
    'high.value@example.com',
    'medium.value@example.com',
    'low.budget@example.com'
)
ORDER BY score DESC;

-- Expected: 
-- High value (10M, Referral): Score 60+, HOT
-- Medium (3M, Website): Score 31-60, WARM
-- Low (500K, Cold Call): Score 0-30, COLD

-- ========================================
-- TEST CASE 8: Verify Lead Conversion
-- ========================================

-- Check if project was created
SELECT 
    cp.id as project_id,
    cp.code as project_code,
    cp.name as project_name,
    cp.lead_id,
    l.name as lead_name,
    cp.converted_at,
    cp.converted_by_id
FROM customer_projects cp
JOIN leads l ON cp.lead_id = l.lead_id
WHERE l.email = 'rajesh.test001@example.com';

-- Check if customer user was created
SELECT 
    user_id,
    email,
    first_name,
    last_name,
    phone,
    created_at
FROM customer_users 
WHERE email = 'rajesh.test001@example.com';

-- Check lead status updated to WON
SELECT 
    name,
    lead_status,
    converted_at,
    converted_by_id
FROM leads 
WHERE email = 'rajesh.test001@example.com';

-- Expected: 
-- 1. Project exists with code like PRJ-2026-XXXX
-- 2. Customer user created
-- 3. Lead status = 'WON'

-- ========================================
-- TEST CASE 11: Verify Lead Deletion
-- ========================================

-- This should return 0 rows if deletion was successful
SELECT * FROM leads 
WHERE email = 'delete.test@example.com';

-- Expected: 0 rows

-- ========================================
-- TEST CASE 13: Overdue Follow-Ups
-- ========================================

-- Find all overdue follow-ups
SELECT 
    name,
    email,
    next_follow_up,
    lead_status,
    NOW() - next_follow_up as days_overdue
FROM leads 
WHERE next_follow_up < NOW() 
AND lead_status NOT IN ('won', 'lost')
ORDER BY next_follow_up;

-- Expected: Shows leads with past follow-up dates

-- ========================================
-- ANALYTICS QUERIES
-- ========================================

-- Total leads by status
SELECT 
    lead_status,
    COUNT(*) as count
FROM leads 
GROUP BY lead_status
ORDER BY count DESC;

-- Leads by source
SELECT 
    lead_source,
    COUNT(*) as count
FROM leads 
GROUP BY lead_source
ORDER BY count DESC;

-- Leads by priority
SELECT 
    priority,
    COUNT(*) as count
FROM leads 
GROUP BY priority
ORDER BY count DESC;

-- Score distribution
SELECT 
    score_category,
    COUNT(*) as count,
    ROUND(AVG(score), 2) as avg_score
FROM leads 
GROUP BY score_category
ORDER BY avg_score DESC;

-- Conversion rate
SELECT 
    COUNT(*) as total_leads,
    COUNT(*) FILTER (WHERE lead_status = 'won') as converted_leads,
    ROUND(
        (COUNT(*) FILTER (WHERE lead_status = 'won')::NUMERIC / COUNT(*)) * 100,
        2
    ) as conversion_rate_percent
FROM leads;

-- ========================================
-- CLEANUP - Run after all tests
-- ========================================

-- Delete all test leads
DELETE FROM leads 
WHERE email LIKE '%@example.com' 
OR email LIKE '%.test%@%';

-- Delete test projects
DELETE FROM customer_projects 
WHERE name LIKE '%Test%' 
OR name LIKE '%Kumar Residence%'
OR lead_id IN (SELECT lead_id FROM leads WHERE email LIKE '%@example.com');

-- Delete test customer users
DELETE FROM customer_users 
WHERE email LIKE '%@example.com' 
OR email LIKE '%.test%@%';

-- Verify cleanup
SELECT 
    (SELECT COUNT(*) FROM leads WHERE email LIKE '%@example.com') as test_leads_remaining,
    (SELECT COUNT(*) FROM customer_projects WHERE name LIKE '%Test%') as test_projects_remaining,
    (SELECT COUNT(*) FROM customer_users WHERE email LIKE '%@example.com') as test_users_remaining;

-- Expected: All counts should be 0
