package com.example.supportdesk.integration.auth;

import com.example.supportdesk.auth.dto.ChangePasswordRequest;
import com.example.supportdesk.auth.dto.LoginRequest;
import com.example.supportdesk.integration.config.AbstractAuthenticatedIntegrationTest;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthIntegrationTest extends AbstractAuthenticatedIntegrationTest {
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    //
    @Test
    public void shouldLoginUserAndReturnAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest(USER_USERNAME, USER_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMs").value(greaterThan(0)));
    }

    @Test
    public void shouldLoginAdminAndReturnAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInMs").value(greaterThan(0)));
    }

    //
    @Test
    public void shouldRejectLoginWithInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest(USER_USERNAME, "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    public void shouldRejectLoginWithBlankUsername() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest("", USER_PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    public void shouldRejectLoginWithBlankPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest(USER_USERNAME, ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    ////
    @Test
    public void shouldReturnCurrentUserProfileForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.fullName").value(USER_FULL_NAME))
                .andExpect(jsonPath("$.username").value(USER_USERNAME))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    public void shouldReturnCurrentUserProfileForAuthenticatedAdmin() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.fullName").value(ADMIN_FULL_NAME))
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    //
    @Test
    public void shouldRejectMeEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    @Test
    public void shouldRejectMeEndpointWithMalformedJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    @Test
    public void shouldRejectMeEndpointWithMalformedAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Token " + userAccessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    ////
    @Test
    public void shouldChangePasswordAndInvalidateOldPasswordAndAcceptNewPassword() throws Exception {
        Instant beforeChange = appUserRepository.findById(user.getId())
                .orElseThrow()
                .getPasswordChangedAt();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangePasswordRequest(
                                USER_PASSWORD,
                                "new-password-123",
                                "new-password-123"
                        ))))
                .andExpect(status().isNoContent());
        //
        AppUser updatedUser = appUserRepository.findById(user.getId()).orElseThrow();

        //
        Assertions.assertThat(passwordEncoder.matches("new-password-123", updatedUser.getPasswordHash())).isTrue();
        Assertions.assertThat(passwordEncoder.matches(USER_PASSWORD, updatedUser.getPasswordHash())).isFalse();
        Assertions.assertThat(updatedUser.getPasswordChangedAt()).isAfterOrEqualTo(beforeChange);

        //
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest(USER_USERNAME, USER_PASSWORD))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest(USER_USERNAME, "new-password-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    //
    @Test
    public void shouldRejectPasswordChangeWhenConfirmationDoesNotMatch() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangePasswordRequest(
                                USER_PASSWORD,
                                "new-password-123",
                                "different-new-password-123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passwords don't match"))
                .andExpect(jsonPath("$.path").value("/api/auth/change-password"));
    }

    @Test
    public void shouldRejectPasswordChangeWhenCurrentPasswordIsIncorrect() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangePasswordRequest(
                                "wrong-current-password",
                                "new-password-123",
                                "new-password-123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"))
                .andExpect(jsonPath("$.path").value("/api/auth/change-password"));
    }

    @Test
    public void shouldRejectPasswordChangeWhenNewPasswordMatchesCurrentPassword() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangePasswordRequest(
                                USER_PASSWORD,
                                USER_PASSWORD,
                                USER_PASSWORD
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password must be different from current password"))
                .andExpect(jsonPath("$.path").value("/api/auth/change-password"));
    }

    @Test
    public void shouldRejectPasswordChangeWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new ChangePasswordRequest(
                                USER_PASSWORD,
                                "new-password-123",
                                "different-new-password-123"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/auth/change-password"));
    }
}
