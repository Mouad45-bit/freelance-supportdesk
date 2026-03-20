CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    password_changed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_app_users_username UNIQUE (username),
    CONSTRAINT chk_app_users_role CHECK ( role IN ('USER', 'ADMIN') )
);

CREATE INDEX idx_app_users_role ON app_users(role);