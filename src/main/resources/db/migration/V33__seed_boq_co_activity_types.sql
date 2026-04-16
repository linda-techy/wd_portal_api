INSERT INTO activity_types (name, description)
SELECT 'BOQ_SUBMITTED', 'BOQ document submitted for customer approval'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'BOQ_SUBMITTED');

INSERT INTO activity_types (name, description)
SELECT 'BOQ_APPROVED', 'BOQ document approved by customer'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'BOQ_APPROVED');

INSERT INTO activity_types (name, description)
SELECT 'BOQ_REJECTED', 'BOQ document rejected'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'BOQ_REJECTED');

INSERT INTO activity_types (name, description)
SELECT 'CO_SENT_TO_CUSTOMER', 'Change order sent to customer for review'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'CO_SENT_TO_CUSTOMER');

INSERT INTO activity_types (name, description)
SELECT 'CO_APPROVED', 'Change order approved by customer'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'CO_APPROVED');

INSERT INTO activity_types (name, description)
SELECT 'CO_REJECTED', 'Change order rejected by customer'
WHERE NOT EXISTS (SELECT 1 FROM activity_types WHERE name = 'CO_REJECTED');
