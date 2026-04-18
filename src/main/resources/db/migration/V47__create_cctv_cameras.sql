-- V47: Create CCTV cameras configuration table
CREATE TABLE IF NOT EXISTS cctv_cameras (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES customer_projects(id),
    camera_name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    provider VARCHAR(100),
    stream_protocol VARCHAR(20) NOT NULL DEFAULT 'HLS',
    stream_url VARCHAR(1000),
    snapshot_url VARCHAR(1000),
    username VARCHAR(255),
    password VARCHAR(255),
    port INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT true,
    resolution VARCHAR(50),
    installation_date DATE,
    display_order INTEGER DEFAULT 0,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cctv_cameras_project ON cctv_cameras(project_id);
CREATE INDEX idx_cctv_cameras_active ON cctv_cameras(project_id, is_active) WHERE deleted_at IS NULL;
