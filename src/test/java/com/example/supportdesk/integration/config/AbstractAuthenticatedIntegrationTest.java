package com.example.supportdesk.integration.config;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import com.example.supportdesk.security.service.JwtService;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    protected AppUserRepository appUserRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtService jwtService;

    //
    protected AppUser user;
    protected AppUser admin;

    //
    @BeforeEach
    void setUpAuthenticatedUsers() {
        user = createUser(USER_FULL_NAME, USER_USERNAME, USER_PASSWORD, UserRole.USER);
        admin = createUser(ADMIN_FULL_NAME, ADMIN_USERNAME, ADMIN_PASSWORD, UserRole.ADMIN);
    }

    //
    protected String userAccessToken() {
        return jwtService.generateToken(AppUserPrincipal.from(user));
    }

    protected String adminAccessToken() {
        return jwtService.generateToken(AppUserPrincipal.from(admin));
    }

    protected String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    ////
    private AppUser createUser(
            String fullName,
            String username,
            String rawPassword,
            UserRole role
    ) {
        return appUserRepository.save(
                new AppUser(
                        fullName,
                        username,
                        passwordEncoder.encode(rawPassword),
                        role
                )
        );
    }
}
