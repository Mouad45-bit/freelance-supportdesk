package com.example.supportdesk.audit.specification;

import com.example.supportdesk.audit.entity.AuditLog;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {
    private AuditLogSpecifications(){}
    //
    public static Specification<AuditLog> hasAction(AuditAction action){
        return (root, query, cb) ->
                action == null ? null : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasResourceType(AuditResourceType resourceType){
        return (root, query, cb) ->
                resourceType == null ? null : cb.equal(root.get("resourceType"), resourceType);
    }

    public static Specification<AuditLog> hasActorId(Long actorId){
        return (root, query, cb) ->
                actorId == null ? null : cb.equal(root.get("actor").get("id"), actorId);
    }
}
