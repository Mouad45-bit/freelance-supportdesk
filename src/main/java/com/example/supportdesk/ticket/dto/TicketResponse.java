package com.example.supportdesk.ticket.dto;

import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.ticket.entity.Ticket;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        Long authorId,
        String authorUsername,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAuthor().getId(),
                ticket.getAuthor().getUsername(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
