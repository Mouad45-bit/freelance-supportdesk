ALTER TABLE tickets
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tickets
ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_tickets_deleted ON tickets(deleted);