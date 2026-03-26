package com.example.supportdesk.integration.config;

import com.example.supportdesk.integration.support.IntegrationDatabaseSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    @Container
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("supportdesk_it")
                    .withUsername("supportdesk")
                    .withPassword("supportdesk");
    //
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRESQL::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRESQL::getUsername);
        registry.add("DB_PASSWORD", POSTGRESQL::getPassword);
    }

    //
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected IntegrationDatabaseSupport databaseSupport;

    //
    @BeforeEach
    void cleanDatabaseBeforeEach() {
        databaseSupport.cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfterEach() {
        databaseSupport.cleanDatabase();
    }

    //
    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
