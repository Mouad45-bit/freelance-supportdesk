CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_tickets_author
                     FOREIGN KEY (author_id) REFERENCES app_users(id),
    CONSTRAINT chk_tickets_status
                     CHECK ( status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED') ),
    CONSTRAINT chk_tickets_priority
                     CHECK ( priority IN ('LOW', 'MEDIUM', 'HIGH') ),
    CONSTRAINT chk_tickets_title_not_blank
                 CHECK ( length(btrim(title)) > 0 ),
    CONSTRAINT chk_tickets_description_not_blank
        CHECK ( length(btrim(description)) > 0 )
);

CREATE INDEX idx_tickets_author_id ON tickets(author_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_priority ON tickets(priority);
CREATE INDEX idx_tickets_created_at ON tickets(created_at);