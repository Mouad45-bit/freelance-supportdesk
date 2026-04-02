package com.example.supportdesk.integration.ticket;

import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.ticket.dto.TicketStatusUpdateRequest;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TicketStatusUpdateIntegrationTest extends AbstractTicketIntegrationTest {
    ////
    @Test
    public void shouldUpdateTicketStatusFromOpenToInProgressForAuthorAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.HIGH,
                TicketStatus.OPEN
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        //
        databaseSupport.clearPersistenceContext();

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        //
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );
    }

    @Test
    public void shouldUpdateTicketStatusFromInProgressToResolvedForAuthorAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.HIGH,
                TicketStatus.IN_PROGRESS
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.RESOLVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        //
        databaseSupport.clearPersistenceContext();

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        //
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.RESOLVED);

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );
    }

    //
    @Test
    public void shouldAllowAdminToUpdateAnotherUsersTicketStatusAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "User ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.OPEN
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        //
        databaseSupport.clearPersistenceContext();

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        //
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                admin.getId()
        );
    }

    @Test
    public void shouldAllowAdminToApplyTransitionForbiddenToRegularUserAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Resolved ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.RESOLVED
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.OPEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.status").value("OPEN"));

        //
        databaseSupport.clearPersistenceContext();

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        //
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.OPEN);

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                admin.getId()
        );
    }

    ////
    @Test
    public void shouldRejectTicketStatusUpdateWithInvalidUserTransition() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.HIGH,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.RESOLVED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition for USER: OPEN -> RESOLVED"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    @Test
    public void shouldRejectTicketStatusUpdateWhenStatusDoesNotChange() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.HIGH,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.OPEN))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ticket is already in status OPEN"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    @Test
    public void shouldRejectTicketStatusUpdateWithoutStatus() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.status").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    @Test
    public void shouldRejectTicketStatusUpdateWithInvalidStatus() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.IN_PROGRESS
        );

        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "status": "DONE"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for status"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    //
    @Test
    public void shouldRejectTicketStatusUpdateWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();
        //
        Ticket ticket = dataFactory.createTicket(
                user,
                "Private status ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.OPEN
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update the status of your own tickets"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    @Test
    public void shouldRejectTicketStatusUpdateWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    //
    @Test
    public void shouldReturnNotFoundWhenUpdatingStatusOfNonExistingTicket() throws Exception {
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", 999999L)
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999/status"));
    }

    @Test
    public void shouldReturnNotFoundWhenUpdatingStatusOfDeletedTicket() throws Exception {
        Ticket deletedTicket = dataFactory.createDeletedTicket(
                user,
                "Deleted ticket",
                "Deleted desc",
                TicketPriority.MEDIUM,
                TicketStatus.OPEN
        );

        mockMvc.perform(patch("/api/tickets/{ticketId}/status", deletedTicket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + deletedTicket.getId() + "/status"));
    }
}