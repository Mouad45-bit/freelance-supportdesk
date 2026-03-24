package com.example.supportdesk.audit.service;

import com.example.supportdesk.audit.repository.AuditLogRepository;
import com.example.supportdesk.user.repository.AppUserRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private AppUserRepository appUserRepository;
    private JsonMapper jsonMapper;
    //
}
