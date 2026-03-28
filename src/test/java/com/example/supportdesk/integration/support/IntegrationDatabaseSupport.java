package com.example.supportdesk.integration.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationDatabaseSupport {
    private final JdbcTemplate jdbcTemplate;
    //
    @PersistenceContext
    private EntityManager entityManager;

    //
    public void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                ticket_comment_versions,
                ticket_comments,
                audit_logs,
                tickets,
                app_users
                RESTART IDENTITY CASCADE
                """);
    }

    public void clearPersistenceContext() {
        entityManager.clear();
    }

    public long countAppUsers() {
        return count("app_users");
    }

    public long countTickets() {
        return count("tickets");
    }

    public long countComments() {
        return count("ticket_comments");
    }

    public long countCommentVersions() {
        return count("ticket_comment_versions");
    }

    public long countAuditLogs() {
        return count("audit_logs");
    }

    ////
    private long count(String tableName) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Long.class
        );
        //
        return value == null ? 0L : value;
    }
}
