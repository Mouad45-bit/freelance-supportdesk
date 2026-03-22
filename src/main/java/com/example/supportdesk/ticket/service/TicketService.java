package com.example.supportdesk.ticket.service;

import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.common.enums.UserRole;
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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    //
    public TicketResponse createTicket(AppUserPrincipal principal, TicketCreateRequest request) {
        requireUser(principal);

        AppUser author = appUserRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        //
        Ticket ticket = new Ticket(
                request.title().trim(),
                request.description().trim(),
                request.priority(),
                author
        );

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    public TicketResponse getTicketById(AppUserPrincipal principal, Long ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        enforceReadAccess(principal, ticket);
        return TicketResponse.from(ticket);
    }

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
                .where(TicketSpecifications.hasStatus(status))
                .and(TicketSpecifications.hasPriority(priority))
                .and(TicketSpecifications.hasAuthorId(effectiveAuthorId))
                .and(TicketSpecifications.titleContains(keyword));
        //
        return ticketRepository.findAll(spec, pageable).map(TicketResponse::from);
    }

    public TicketResponse updateTicketStatus(
            AppUserPrincipal principal,
            Long ticketId,
            TicketStatusUpdateRequest request)
    {
        Ticket ticket = findTicketOrThrow(ticketId);
        //
        if (principal.getRole() == UserRole.USER) {
            if (!ticket.getAuthor().getId().equals(principal.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only update the status of your own tickets"
                );
            }
        }

        validateStatusTransition(ticket.getStatus(), request.status(), principal.getRole());

        //
        ticket.changeStatus(request.status());

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    public void deleteTicket(AppUserPrincipal principal, Long ticketId) {
        requireUser(principal);
        //
        Ticket ticket = findTicketOrThrow(ticketId);

        if (!ticket.getAuthor().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own tickets");
        }

        //
        ticketRepository.delete(ticket);
    }

    //
    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
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

    private void requireUser(AppUserPrincipal principal) {
        if (principal.getRole() != UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only USER can perform this action");
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
        String effectiveSortBy = (sortBy != null || sortBy.isBlank()) ? "createdAt" : sortBy;
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
            return;
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
