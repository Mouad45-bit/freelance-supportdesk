package com.example.supportdesk.integration.ticket;

import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TicketReadIntegrationTest extends AbstractTicketIntegrationTest {
    ////
    @Test
    public void shouldGetTicketByIdWhenRequesterIsAuthor() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "My ticket",
                "My description",
                TicketPriority.MEDIUM
        );
        //
        mockMvc.perform(get("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.title").value("My ticket"))
                .andExpect(jsonPath("$.authorId").value(user.getId()))
                .andExpect(jsonPath("$.authorUsername").value(USER_USERNAME));
    }

    @Test
    public void shouldGetTicketByIdWhenRequesterIsAdmin() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "User ticket",
                "User description",
                TicketPriority.MEDIUM
        );
        //
        mockMvc.perform(get("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.title").value("User ticket"))
                .andExpect(jsonPath("$.authorId").value(user.getId()));
    }

    ////
    @Test
    public void shouldRejectGetTicketByIdWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Protected ticket",
                "Protected desc",
                TicketPriority.MEDIUM
        );
        //
        mockMvc.perform(get("/api/tickets/{ticketId}", ticket.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId()));
    }

    @Test
    public void shouldRejectGetTicketByIdWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();
        //
        Ticket ticket = dataFactory.createTicket(
                user,
                "Private ticket",
                "Private description",
                TicketPriority.HIGH
        );

        //
        mockMvc.perform(get("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied to this ticket"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId()));
    }

    //
    @Test
    public void shouldRejectGetTicketByIdWithInvalidId() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}", "abc")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid path variable type"))
                .andExpect(jsonPath("$.path").value("/api/tickets/abc"));
    }

    //
    @Test
    public void shouldReturnNotFoundWhenGettingNonExistingTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}", 999999L)
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999"));
    }

    @Test
    public void shouldReturnNotFoundWhenGettingSoftDeletedTicket() throws Exception {
        Ticket deletedTicket = dataFactory.createDeletedTicket(
                user,
                "Deleted ticket",
                "Deleted description",
                TicketPriority.LOW,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(get("/api/tickets/{ticketId}", deletedTicket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + deletedTicket.getId()));
    }
}