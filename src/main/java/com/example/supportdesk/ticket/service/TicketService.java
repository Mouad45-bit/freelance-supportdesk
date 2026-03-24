package com.example.supportdesk.ticket.service;

import com.example.supportdesk.audit.service.AuditLogService;
import com.example.supportdesk.common.enums.*;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import com.example.supportdesk.ticket.dto.TicketCreateRequest;
import com.example.supportdesk.ticket.dto.TicketResponse;
import com.example.supportdesk.ticket.dto.TicketStatusUpdateRequest;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.ticket.specification.TicketSpecifications;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    //
    private final AuditLogService auditLogService;

    //
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public TicketResponse createTicket(AppUserPrincipal principal, TicketCreateRequest request) {
        AppUser author = appUserRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        //
        Ticket ticket = new Ticket(
                request.title().trim(),
                request.description().trim(),
                request.priority(),
                author
        );

        Ticket saved = ticketRepository.save(ticket);

        //
        auditLogService.log(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                saved.getId(),
                principal.getId(),
                Map.of(
                        "title", saved.getTitle(),
                        "priority", saved.getPriority().name(),
                        "status", saved.getStatus().name()
                )
        );

        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TicketResponse getTicketById(AppUserPrincipal principal, Long ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        enforceReadAccess(principal, ticket);
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Page<TicketResponse> listTickets(
            AppUserPrincipal principal,
            TicketStatus status,
            TicketPriority priority,
            Long authorId,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        //
        Long effectiveAuthorId = principal.getRole() == UserRole.USER
                ? principal.getId()
                : authorId;

        //
        Specification<Ticket> spec = Specification
                .where(TicketSpecifications.isNotDeleted())
                .and(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.hasPriority(priority))
                .and(TicketSpecifications.hasAuthorId(effectiveAuthorId))
                .and(TicketSpecifications.titleContains(keyword));
        //
        return ticketRepository.findAll(spec, pageable).map(TicketResponse::from);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public TicketResponse updateTicketStatus(
            AppUserPrincipal principal,
            Long ticketId,
            TicketStatusUpdateRequest request)
    {
        Ticket ticket = findTicketOrThrow(ticketId);
        //
        if (principal.getRole() == UserRole.USER
                && !ticket.getAuthor().getId().equals(principal.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only update the status of your own tickets"
            );
        }

        validateStatusTransition(ticket.getStatus(), request.status(), principal.getRole());

        //
        TicketStatus oldStatus = ticket.getStatus();
        ticket.changeStatus(request.status());

        Ticket saved = ticketRepository.save(ticket);

        //
        auditLogService.log(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                saved.getId(),
                principal.getId(),
                Map.of(
                        "field", "status",
                        "oldValue", oldStatus.name(),
                        "newValue", saved.getStatus().name()
                )
        );

        return TicketResponse.from(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void deleteTicket(AppUserPrincipal principal, Long ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);

        if (!ticket.getAuthor().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own tickets");
        }

        //
        auditLogService.log(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                ticket.getId(),
                principal.getId(),
                Map.of(
                        "title", ticket.getTitle(),
                        "status", ticket.getStatus().name()
                )
        );

        ticketRepository.delete(ticket);
    }

    //
    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private void enforceReadAccess(AppUserPrincipal principal, Ticket ticket) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }

        if (!ticket.getAuthor().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this ticket");
        }
    }

    private Pageable buildPageable(int page,  int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0");
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }
        //
        String effectiveSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        if (!effectiveSortBy.equals("createdAt")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only createdAt sorting is allowed");
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        //
        return PageRequest.of(page, size, Sort.by(direction, effectiveSortBy));
    }

    private void validateStatusTransition(TicketStatus current, TicketStatus target, UserRole role) {
        if (current == target) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Ticket is already in status " + current.name()
            );
        }

        if (role == UserRole.ADMIN) {
            return;
        }

        //
        boolean allowedForUser =
                (current == TicketStatus.OPEN && target == TicketStatus.IN_PROGRESS)
                        || (current == TicketStatus.IN_PROGRESS && target == TicketStatus.RESOLVED);

        if (!allowedForUser) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition for USER: " + current + " -> " + target
            );
        }
    }
}
