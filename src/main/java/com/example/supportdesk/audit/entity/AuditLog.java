package com.example.supportdesk.audit.entity;

import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity @Table(name = "audit_logs")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private AuditResourceType resourceType;
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private AppUser actor;
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    //
    public AuditLog(
            AuditAction action,
            AuditResourceType resourceType,
            Long resourceId,
            AppUser actor,
            String details
    ) {
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.actor = actor;
        this.timestamp = Instant.now();
        this.details = details;
    }
}
