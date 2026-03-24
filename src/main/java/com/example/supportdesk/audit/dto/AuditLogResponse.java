package com.example.supportdesk.audit.dto;

import com.example.supportdesk.audit.entity.AuditLog;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        AuditAction action,
        AuditResourceType resourceType,
        Long resourceId,
        Long actorId,
        String actorUsername,
        Instant timestamp,
        String details
) {
    public static AuditLogResponse from(AuditLog log){
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getActor().getId(),
                log.getActor().getUsername(),
                log.getTimestamp(),
                log.getDetails()
        );
    }
}
