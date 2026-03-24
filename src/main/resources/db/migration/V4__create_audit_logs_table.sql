CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(20) NOT NULL,
    resource_type VARCHAR(20) NOT NULL,
    resource_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    details TEXT,

    CONSTRAINT fk_audit_logs_actor
                        FOREIGN KEY (actor_id) REFERENCES app_users(id),
    CONSTRAINT chk_audit_logs_actions
                        CHECK ( action IN ('CREATE', 'UPDATE', 'DELETE') ),
    CONSTRAINT chk_audit_logs_resource_type
                        CHECK ( resource_type IN ('TICKET', 'COMMENT') )
);

CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_resource_type ON audit_logs(resource_type);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);