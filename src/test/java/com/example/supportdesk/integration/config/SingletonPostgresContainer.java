package com.example.supportdesk.integration.config;

import org.testcontainers.containers.PostgreSQLContainer;

public final class SingletonPostgresContainer
        extends PostgreSQLContainer<SingletonPostgresContainer> {
    private static final SingletonPostgresContainer INSTANCE =
            new SingletonPostgresContainer();
    //
    private SingletonPostgresContainer() {
        super("postgres:16-alpine");
        withDatabaseName("supportdesk-it");
        withUsername("supportdesk");
        withPassword("supportdesk");
    }

    //
    public static SingletonPostgresContainer getInstance() {
        return INSTANCE;
    }

    static {
        INSTANCE.start();
    }
}
