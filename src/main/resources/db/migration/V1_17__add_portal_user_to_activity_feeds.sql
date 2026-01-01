ALTER TABLE activity_feeds ADD COLUMN portal_user_id BIGINT;
ALTER TABLE activity_feeds ALTER COLUMN created_by_id DROP NOT NULL;

ALTER TABLE activity_feeds ADD CONSTRAINT fk_activity_portal_user FOREIGN KEY (portal_user_id) REFERENCES portal_users(id);
