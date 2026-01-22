-- V1_53__fix_mep_category_reference_type.sql
-- Fix any MEP-related categories that might have been missed
-- Ensure all MEP categories are set to PROJECT only

-- Update any category containing "MEP" to be PROJECT-specific
UPDATE document_categories 
SET reference_type = 'PROJECT' 
WHERE UPPER(name) LIKE '%MEP%'
AND reference_type != 'PROJECT';

-- Also ensure all project-related categories are properly set
UPDATE document_categories 
SET reference_type = 'PROJECT' 
WHERE (UPPER(name) LIKE '%FLOOR PLAN%'
    OR UPPER(name) LIKE '%STRUCTURAL%'
    OR UPPER(name) LIKE '%ELEVATION%'
    OR UPPER(name) LIKE '%SAFETY%'
    OR UPPER(name) LIKE '%QUALITY%'
    OR UPPER(name) LIKE '%HANDOVER%'
    OR UPPER(name) LIKE '%PERMIT%'
    OR UPPER(name) LIKE '%COLLABORATION%'
    OR UPPER(name) LIKE '%DRAWING%'
    OR UPPER(name) LIKE '%COSTING%')
AND reference_type != 'PROJECT';
