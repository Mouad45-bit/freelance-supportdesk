package com.example.supportdesk.integration.config;

import com.example.supportdesk.integration.support.IntegrationDatabaseSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    protected static final SingletonPostgresContainer POSTGRESQL =
            SingletonPostgresContainer.getInstance();
    //
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
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

    //
    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
