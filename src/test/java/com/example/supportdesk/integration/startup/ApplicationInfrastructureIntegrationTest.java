package com.example.supportdesk.integration.startup;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.integration.config.AbstractIntegrationTest;
import com.example.supportdesk.integration.support.IntegrationDatabaseSupport;
import com.example.supportdesk.integration.support.IntegrationTestDataFactory;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import org.assertj.core.api.Assertions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class ApplicationInfrastructureIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private IntegrationDatabaseSupport databaseSupport;
    @Autowired
    private IntegrationTestDataFactory dataFactory;

    //
    @Test
    public void shouldStartApplicationWithPostgresContainer() {
        Assertions.assertThat(POSTGRESQL.isRunning()).isTrue();
        //
        Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Assertions.assertThat(value).isEqualTo(1);
    }

    @Test
    public void shouldApplyFlywayMigrationsOnStartup() {
        List<String> versions = jdbcTemplate.queryForList("""
                SELECT version
                FROM flyway_schema_history
                WHERE success = true
                ORDER BY installed_rank
                """, String.class);
        //
        Assertions.assertThat(versions)
                .containsExactly("1", "2", "3", "4", "5", "6");
    }

    @Test
    public void shouldConnectJpaAndHibernateToPostgresSuccessfully() {
        AppUser saved = dataFactory.createUser(
                "Startup User",
                "startup-user",
                "password123",
                UserRole.USER
        );

        databaseSupport.flushAndClear();

        AppUser reloaded = appUserRepository.findById(saved.getId())
                .orElseThrow();

        //
        Assertions.assertThat(reloaded.getId()).isEqualTo(saved.getId());
        Assertions.assertThat(reloaded.getUsername()).isEqualTo("startup-user");
        Assertions.assertThat(reloaded.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    public void shouldExposeExpectedTablesAfterMigrations() {
        List<String> tableNames = jdbcTemplate.queryForList("""
                SELECT tablename
                FROM pg_tables
                WHERE schemaname = 'public'
                ORDER BY tablename
                """, String.class);
        //
        Assertions.assertThat(tableNames).contains(
                "app_users",
                "tickets",
                "ticket_comments",
                "ticket_comment_versions",
                "audit_logs",
                "flyway_schema_history"
        );
    }

    @Test
    public void shouldPersistEntityWithCreatedAtAndUpdatedAt() {
        AppUser saved = dataFactory.createUser(
                "Audited User",
                "audited-user",
                "password123",
                UserRole.USER
        );

        databaseSupport.flushAndClear();

        AppUser persisted = appUserRepository.findById(saved.getId())
                .orElseThrow();

        //
        Assertions.assertThat(persisted.getCreatedAt()).isNotNull();
        Assertions.assertThat(persisted.getUpdatedAt()).isNotNull();

        Assertions.assertThat(persisted.getUpdatedAt()).isAfterOrEqualTo(persisted.getCreatedAt());
    }
}
