CREATE TABLE ticket_comments (
    id BIGSERIAL PRIMARY KEY,
    comment_group_id UUID NOT NULL,
    current_version INTEGER NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_ticket_comments_group_id UNIQUE (comment_group_id),
    CONSTRAINT fk_ticket_comments_author
                             FOREIGN KEY (author_id) REFERENCES app_users(id),
    CONSTRAINT fk_ticket_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT chk_ticket_comments_version_positive
                             CHECK ( current_version >= 1 ),
    CONSTRAINT chk_ticket_comments_content_not_blank
                             CHECK ( length(btrim(content)) > 0 )
);

CREATE INDEX idx_ticket_comments_ticket_id ON ticket_comments(ticket_id);
CREATE INDEX idx_ticket_comments_author_id ON ticket_comments(author_id);
CREATE INDEX idx_ticket_comments_created_at ON ticket_comments(created_at);


CREATE TABLE ticket_comment_versions (
    id BIGSERIAL PRIMARY KEY,
    comment_group_id UUID NOT NULL,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    snapshot_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_ticket_comment_versions_group_version
                                     UNIQUE (comment_group_id, version),
    CONSTRAINT fk_ticket_comment_versions_author
        FOREIGN KEY (author_id) REFERENCES app_users(id),
    CONSTRAINT fk_ticket_comment_versions_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT chk_ticket_comment_versions_version_positive
        CHECK ( version >= 1 ),
    CONSTRAINT chk_ticket_comment_versions_content_not_blank
        CHECK ( length(btrim(content)) > 0 )
);

CREATE INDEX idx_ticket_comment_versions_group_id ON ticket_comment_versions(comment_group_id);
CREATE INDEX idx_ticket_comment_versions_ticket_id ON ticket_comment_versions(ticket_id);
CREATE INDEX idx_ticket_comment_versions_snapshot_at ON ticket_comment_versions(snapshot_at);