ALTER TABLE ticket_comments
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE ticket_comments
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_ticket_comments_deleted
    ON ticket_comments(deleted);