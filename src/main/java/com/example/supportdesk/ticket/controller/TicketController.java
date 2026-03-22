package com.example.supportdesk.ticket.controller;

import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import com.example.supportdesk.ticket.dto.TicketCreateRequest;
import com.example.supportdesk.ticket.dto.TicketResponse;
import com.example.supportdesk.ticket.dto.TicketStatusUpdateRequest;
import com.example.supportdesk.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
    //
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody TicketCreateRequest request
    ) {
        TicketResponse response = ticketService.createTicket(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tickedId}")
    public TicketResponse getTicketById(
            @PathVariable Long tickedId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ticketService.getTicketById(principal, tickedId);
    }

    @GetMapping
    public Page<TicketResponse> listTickets(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ticketService.listTickets(
                principal, status, priority, authorId, keyword, page, size, sortBy, sortDir);
    }

    @PatchMapping("/{ticketId}/status")
    public TicketResponse updateTicketStatus(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody TicketStatusUpdateRequest request
    ) {
        return ticketService.updateTicketStatus(principal, ticketId, request);
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        ticketService.deleteTicket(principal, ticketId);
        return ResponseEntity.noContent().build();
    }
}
