-- S3 PR3 — track the last handover date we already alerted the customer about,
-- so subsequent CPM recomputes only push a new FCM when the date moves > 3
-- working days FROM the last-alerted date (cooldown).
ALTER TABLE project_schedule_config
    ADD COLUMN last_alerted_handover_date DATE NULL;
