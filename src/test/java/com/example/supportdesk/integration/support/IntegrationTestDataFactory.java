package com.example.supportdesk.integration.support;

import com.example.supportdesk.audit.entity.AuditLog;
import com.example.supportdesk.audit.repository.AuditLogRepository;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.comment.entity.TicketCommentVersion;
import com.example.supportdesk.comment.repository.TicketCommentRepository;
import com.example.supportdesk.comment.repository.TicketCommentVersionRepository;
import com.example.supportdesk.common.enums.*;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationTestDataFactory {
    private final AppUserRepository appUserRepository;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketCommentVersionRepository ticketCommentVersionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JsonMapper jsonMapper;

    //
    public AppUser createUser(
            String fullName,
            String username,
            String rawPassword,
            UserRole role
    ) {
        AppUser user = new AppUser(
                fullName,
                username,
                passwordEncoder.encode(rawPassword),
                role
        );
        //
        return appUserRepository.save(user);
    }

    //
    public Ticket createTicket(
            AppUser author,
            String title,
            String description,
            TicketPriority priority
    ) {
        Ticket ticket = new Ticket(title, description, priority, author);
        //
        return ticketRepository.save(ticket);
    }

    public Ticket createTicket(
            AppUser author,
            String title,
            String description,
            TicketPriority priority,
            TicketStatus status
    ) {
        Ticket ticket = new Ticket(title, description, priority, author);
        if (status != TicketStatus.OPEN) {
            ticket.changeStatus(status);
        }
        //
        return ticketRepository.save(ticket);
    }

    public Ticket createDeletedTicket(
            AppUser author,
            String title,
            String description,
            TicketPriority priority,
            TicketStatus status
    ) {
        Ticket ticket = createTicket(author, title, description, priority, status);
        ticket.markDeleted();
        //
        return ticketRepository.save(ticket);
    }

    //
    public TicketComment createComment(
            AppUser author,
            Ticket ticket,
            String content
    ) {
        TicketComment comment = new TicketComment(content, author, ticket);
        //
        TicketComment saved = ticketCommentRepository.save(comment);
        ticketCommentVersionRepository.save(TicketCommentVersion.fromCurrent(saved));
        //
        return saved;
    }

    public TicketComment createUpdatedComment(
            AppUser author,
            Ticket ticket,
            String initialContent,
            String updatedContent
    ) {
        TicketComment comment = createComment(author, ticket, initialContent);
        comment.updateContent(updatedContent);
        //
        TicketComment saved = ticketCommentRepository.save(comment);
        ticketCommentVersionRepository.save(TicketCommentVersion.fromCurrent(saved));
        //
        return saved;
    }

    public TicketComment createDeletedComment(
            AppUser author,
            Ticket ticket,
            String content
    ) {
        TicketComment comment = createComment(author, ticket, content);
        comment.markDeleted();
        //
        return ticketCommentRepository.save(comment);
    }

    //
    public AuditLog createAuditLog(
            AuditAction action,
            AuditResourceType resourceType,
            Long resourceId,
            AppUser actor,
            Object details
    ) {
        AuditLog auditLog = new AuditLog(
                action,
                resourceType,
                resourceId,
                actor,
                toJson(details)
        );
        //
        return auditLogRepository.save(auditLog);
    }

    ////
    private String toJson(Object details) {
        if (details == null) {
            return null;
        }
        //
        try {
            return jsonMapper.writeValueAsString(details);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize test audit details", ex);
        }
    }
}
