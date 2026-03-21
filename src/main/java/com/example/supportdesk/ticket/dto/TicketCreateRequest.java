package com.example.supportdesk.ticket.dto;

import com.example.supportdesk.common.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketCreateRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 1500) String description,
        @NotNull TicketPriority priority
) {
}
