package com.example.supportdesk.integration.config;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.integration.support.IntegrationAuthSupport;
import com.example.supportdesk.integration.support.IntegrationTestDataFactory;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractAuthenticatedIntegrationTest extends AbstractIntegrationTest {
    protected static final String USER_FULL_NAME = "Integration User";
    protected static final String USER_USERNAME = "user-it";
    protected static final String USER_PASSWORD = "user123456";
    //
    protected static final String ADMIN_FULL_NAME = "Integration Admin";
    protected static final String ADMIN_USERNAME = "admin-it";
    protected static final String ADMIN_PASSWORD = "admin123456";

    //
    @Autowired
    protected IntegrationTestDataFactory dataFactory;

    @Autowired
    protected IntegrationAuthSupport authSupport;

    //
    protected AppUser user;
    protected AppUser admin;

    //
    @BeforeEach
    void setUpAuthenticatedUsers() {
        user = dataFactory.createUser(
                USER_FULL_NAME,
                USER_USERNAME,
                USER_PASSWORD,
                UserRole.USER
        );
        //
        admin = dataFactory.createUser(
                ADMIN_FULL_NAME,
                ADMIN_USERNAME,
                ADMIN_PASSWORD,
                UserRole.ADMIN
        );
    }

    //
    protected String userAccessToken() {
        return authSupport.generateAccessToken(user);
    }

    protected String adminAccessToken() {
        return authSupport.generateAccessToken(admin);
    }

    protected String bearerToken(String accessToken) {
        return authSupport.bearerToken(accessToken);
    }
}
