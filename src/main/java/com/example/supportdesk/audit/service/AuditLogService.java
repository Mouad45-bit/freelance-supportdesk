package com.example.supportdesk.audit.service;

import com.example.supportdesk.audit.entity.AuditLog;
import com.example.supportdesk.audit.repository.AuditLogRepository;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private AppUserRepository appUserRepository;
    private JsonMapper jsonMapper;
    //
    @Transactional
    public void log(
            AuditAction action,
            AuditResourceType resourceType,
            Long resourceId,
            Long actorId,
            Map<String, Object> details
    ) {
        AppUser actor = appUserRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Actor not found"));

        String detailsJson = toJson(details);
        //
        AuditLog auditLog = new AuditLog(
                action,
                resourceType,
                resourceId,
                actor,
                detailsJson
        );

        auditLogRepository.save(auditLog);
    }

    //
    private String toJson(Map<String, Object> details) {
        try {
            return details == null ? null
                    : jsonMapper.writeValueAsString(details);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize audit details"
            );
        }
    }
}
