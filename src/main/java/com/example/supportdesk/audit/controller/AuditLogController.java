package com.example.supportdesk.audit.controller;

import com.example.supportdesk.audit.dto.AuditLogResponse;
import com.example.supportdesk.audit.service.AuditLogService;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;
    //
    @GetMapping
    public Page<AuditLogResponse> listLogs(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditResourceType resourceType,
            @RequestParam(required = false) Long actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return auditLogService.listLogs(
                principal, action, resourceType, actorId, page, size, sortBy, sortDir
        );
    }
}
