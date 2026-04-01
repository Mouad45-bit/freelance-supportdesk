package com.example.supportdesk.integration.ticket;

import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TicketDeleteIntegrationTest extends AbstractTicketIntegrationTest {
    ////
    @Test
    public void shouldSoftDeleteTicketAndHideItFromReadsAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Delete me",
                "Delete desc",
                TicketPriority.HIGH
        );

        //
        mockMvc.perform(delete("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNoContent());

        //
        databaseSupport.clearPersistenceContext();

        //
        assertionSupport.assertTicketSoftDeleted(ticket.getId());
        //
        assertionSupport.assertAuditLogExists(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );

        //
        mockMvc.perform(get("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound());

        //
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    ////
    @Test
    public void shouldRejectTicketDeletionWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Protected delete ticket",
                "Protected delete desc",
                TicketPriority.HIGH
        );
        //
        mockMvc.perform(delete("/api/tickets/{ticketId}", ticket.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId()));
    }

    @Test
    public void shouldRejectTicketDeletionWhenRequesterIsAdmin() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "User ticket",
                "Admin should not be allowed to delete tickets",
                TicketPriority.MEDIUM
        );
        //
        mockMvc.perform(delete("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId()));
    }

    @Test
    public void shouldRejectTicketDeletionWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();
        //
        Ticket ticket = dataFactory.createTicket(
                user,
                "Delete protected ticket",
                "Delete desc",
                TicketPriority.HIGH
        );

        //
        mockMvc.perform(delete("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your own tickets"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId()));
    }

    //
    @Test
    public void shouldReturnNotFoundWhenDeletingAlreadyDeletedTicket() throws Exception {
        Ticket deletedTicket = dataFactory.createDeletedTicket(
                user,
                "Already deleted",
                "Deleted desc",
                TicketPriority.LOW,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(delete("/api/tickets/{ticketId}", deletedTicket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + deletedTicket.getId()));
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingNonExistingTicket() throws Exception {
        mockMvc.perform(delete("/api/tickets/{ticketId}", 999999L)
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999"));
    }
}