package com.example.supportdesk.integration.support;

import com.example.supportdesk.audit.entity.AuditLog;
import com.example.supportdesk.audit.repository.AuditLogRepository;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.comment.repository.TicketCommentRepository;
import com.example.supportdesk.comment.repository.TicketCommentVersionRepository;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationAssertionSupport {
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketCommentVersionRepository ticketCommentVersionRepository;
    private final AuditLogRepository auditLogRepository;

    //
    public void assertTicketSoftDeleted(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AssertionError("Ticket not found: " + ticketId));
        //
        Assertions.assertThat(ticket.isDeleted()).isTrue();
        Assertions.assertThat(ticket.getDeletedAt()).isNotNull();
    }

    public void assertCommentSoftDeleted(Long commentId) {
        TicketComment comment = ticketCommentRepository.findById(commentId)
                .orElseThrow(() -> new AssertionError("Comment not found: " + commentId));
        //
        Assertions.assertThat(comment.isDeleted()).isTrue();
        Assertions.assertThat(comment.getDeletedAt()).isNotNull();
    }

    //
    public void assertCommentVersionCount(Long commentId, int expectedCount) {
        TicketComment comment = ticketCommentRepository.findById(commentId)
                .orElseThrow(() -> new AssertionError("Comment not found: " + commentId));
        //
        long count = ticketCommentVersionRepository
                .findByCommentGroupId(comment.getCommentGroupId(), Pageable.unpaged())
                .getTotalElements();
        //
        Assertions.assertThat(count).isEqualTo(expectedCount);
    }

    public void assertCommentCurrentVersion(Long commentId, int expectedVersion) {
        TicketComment comment = ticketCommentRepository.findById(commentId)
                .orElseThrow(() -> new AssertionError("Comment not found: " + commentId));
        //
        Assertions.assertThat(comment.getCurrentVersion()).isEqualTo(expectedVersion);
    }

    //
    public void assertAuditLogExists(
            AuditAction action,
            AuditResourceType resourceType,
            Long resourceId,
            Long actorId
    ) {
        boolean exists = auditLogRepository.findAll().stream()
                .anyMatch(log ->
                        log.getAction() == action
                        && log.getResourceType() == resourceType
                        && log.getResourceId().equals(resourceId)
                        && log.getActor().getId().equals(actorId)
                );
        //
        Assertions.assertThat(exists).isTrue();
    }

    public void assertLatestAuditLog(
            AuditAction action,
            AuditResourceType resourceType,
            Long resourceId,
            Long actorId
    ) {
        AuditLog latest = auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("No audit log found"));
        //
        Assertions.assertThat(latest.getAction()).isEqualTo(action);
        Assertions.assertThat(latest.getResourceType()).isEqualTo(resourceType);
        Assertions.assertThat(latest.getResourceId()).isEqualTo(resourceId);
        Assertions.assertThat(latest.getActor().getId()).isEqualTo(actorId);
    }
}
