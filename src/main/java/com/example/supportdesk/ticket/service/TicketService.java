package com.example.supportdesk.ticket.service;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import com.example.supportdesk.ticket.dto.TicketCreateRequest;
import com.example.supportdesk.ticket.dto.TicketResponse;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
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

    //
    private void requireUser(AppUserPrincipal principal) {
        if (principal.getRole() != UserRole.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only USER can perform this action");
        }
    }
}
