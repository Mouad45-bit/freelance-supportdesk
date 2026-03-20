package com.example.supportdesk.user.entity;

import com.example.supportdesk.common.entity.BaseEntity;
import com.example.supportdesk.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity @Table(name = "app_users")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseEntity {
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;
    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;
    //
    public AppUser(String fullName, String username, String passwordHash, UserRole role) {
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.passwordChangedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = Instant.now();
    }
}
