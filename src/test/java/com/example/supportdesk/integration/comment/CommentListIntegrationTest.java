package com.example.supportdesk.integration.comment;

import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommentListIntegrationTest extends AbstractCommentIntegrationTest {
    ////
    @Test
    public void shouldListCommentsOfActiveTicketWithoutDeletedOnesForAuthorOrAdmin() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.MEDIUM
        );
        //
        dataFactory.createComment(user, ticket, "Visible comment");
        dataFactory.createDeletedComment(user, ticket, "Deleted comment");

        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Visible comment"))
                .andExpect(jsonPath("$.content[0].deleted").value(false));

        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Visible comment"))
                .andExpect(jsonPath("$.content[0].deleted").value(false))
                .andExpect(jsonPath("$.content[0].authorId").value(user.getId()));
    }

    @Test
    public void shouldListCommentsOfDeletedTicketIncludingDeletedOnesForAuthorOrAdmin() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.MEDIUM
        );
        //
        dataFactory.createComment(user, ticket, "Visible comment");
        dataFactory.createDeletedComment(user, ticket, "Deleted comment");
        //
        ticket.markDeleted();
        ticketRepository.save(ticket);

        //
        databaseSupport.clearPersistenceContext();

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    ////
    @Test
    public void shouldRejectCommentListingWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.LOW
        );
        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentListingWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();
        //
        Ticket ticket = dataFactory.createTicket(
                user,
                "Private ticket",
                "Private description",
                TicketPriority.MEDIUM
        );
        //
        dataFactory.createComment(user, ticket, "Private comment");

        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied for this ticket"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    ////
    @Test
    public void shouldReturnNotFoundWhenListingCommentsOfNonExistingTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", 999999L)
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999/comments"));
    }

    //
    @Test
    public void shouldRejectCommentListingWithNegativePage() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.LOW
        );
        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .param("page", "-1")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must be positive"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentListingWithInvalidSize() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.LOW
        );

        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .param("size", "0")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .param("size", "101")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentListingWithUnsupportedSortBy() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.LOW
        );
        //
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .param("sortBy", "content")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only 'created at' sorting is allowed"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }
}