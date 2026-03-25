package com.example.supportdesk.audit.service;

import com.example.supportdesk.audit.dto.AuditLogResponse;
import com.example.supportdesk.audit.entity.AuditLog;
import com.example.supportdesk.audit.repository.AuditLogRepository;
import com.example.supportdesk.audit.specification.AuditLogSpecifications;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;
    private final JsonMapper jsonMapper;
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

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listLogs(
            AppUserPrincipal principal,
            AuditAction action,
            AuditResourceType resourceType,
            Long actorId,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);

        Long effectiveActorId = principal.getRole() == UserRole.USER
                ? principal.getId()
                : actorId;
        //
        Specification<AuditLog> spec = Specification
                .where(AuditLogSpecifications.hasAction(action))
                .and(AuditLogSpecifications.hasResourceType(resourceType))
                .and(AuditLogSpecifications.hasActorId(effectiveActorId));

        return auditLogRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    //
    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0");
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }
        //
        String effectiveSortBy = (sortBy == null || sortBy.isBlank()) ? "timestamp" : sortBy;
        if (!effectiveSortBy.equals("timestamp")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only timestamp sorting is allowed");
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        //
        return PageRequest.of(page, size, Sort.by(direction, effectiveSortBy));
    }

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

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getActor().getId(),
                log.getActor().getUsername(),
                log.getTimestamp(),
                parseDetails(log.getDetails())
        );
    }

    private Object parseDetails(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }
        //
        try {
            return jsonMapper.readValue(details, Object.class);
        } catch (Exception ex) {
            return details;
        }
    }
}
