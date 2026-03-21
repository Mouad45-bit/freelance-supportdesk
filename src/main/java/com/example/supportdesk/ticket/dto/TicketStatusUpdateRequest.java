package com.example.supportdesk.ticket.dto;

import com.example.supportdesk.common.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TicketStatusUpdateRequest(
        @NotNull TicketStatus status
) {
}
