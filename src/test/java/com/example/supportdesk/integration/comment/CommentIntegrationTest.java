package com.example.supportdesk.integration.comment;

import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.integration.config.AbstractAuthenticatedIntegrationTest;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CommentIntegrationTest extends AbstractAuthenticatedIntegrationTest {
    @Autowired
    private TicketRepository ticketRepository;

    ////
    @Test
    public void shouldListCommentsOfActiveTicketWithoutDeletedOnesForAuthor() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.MEDIUM
        );

        dataFactory.createComment(user, ticket, "Visible comment");
        dataFactory.createDeletedComment(user, ticket, "Deleted comment");

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Visible comment"))
                .andExpect(jsonPath("$.content[0].deleted").value(false));
    }

    @Test
    public void shouldListCommentsOfDeletedTicketIncludingDeletedOnesForAuthor() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.MEDIUM
        );

        dataFactory.createComment(user, ticket, "Visible comment");
        dataFactory.createDeletedComment(user, ticket, "Deleted comment");

        ticket.markDeleted();
        ticketRepository.save(ticket);

        databaseSupport.clearPersistenceContext();

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    public void shouldListCommentsForAdminInReadMode() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "User ticket",
                "User description",
                TicketPriority.LOW
        );

        dataFactory.createComment(user, ticket, "Comment readable by admin");

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].authorId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].content").value("Comment readable by admin"));
    }

    @Test
    public void shouldListCommentVersionsForAuthor() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.MEDIUM
        );

        TicketComment comment = dataFactory.createUpdatedComment(
                user,
                ticket,
                "Version 1",
                "Version 2"
        );

        mockMvc.perform(get("/api/comments/{commentId}/versions", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Version 1"))
                .andExpect(jsonPath("$.content[1].version").value(2))
                .andExpect(jsonPath("$.content[1].content").value("Version 2"));
    }

    @Test
    public void shouldListCommentVersionsForDeletedComment() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.MEDIUM
        );

        TicketComment deletedComment = dataFactory.createDeletedComment(user, ticket, "Deleted but versioned");

        mockMvc.perform(get("/api/comments/{commentId}/versions", deletedComment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Deleted but versioned"));
    }

    @Test
    public void shouldListCommentVersionsForAdminInReadMode() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.MEDIUM
        );

        TicketComment comment = dataFactory.createUpdatedComment(
                user,
                ticket,
                "Version 1",
                "Version 2"
        );

        mockMvc.perform(get("/api/comments/{commentId}/versions", comment.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].authorId").value(user.getId()))
                .andExpect(jsonPath("$.content[1].authorId").value(user.getId()));
    }

    @Test
    public void shouldRejectCommentVersionListingWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();

        Ticket ticket = dataFactory.createTicket(
                user,
                "Private ticket",
                "Private description",
                TicketPriority.MEDIUM
        );

        TicketComment comment = dataFactory.createComment(user, ticket, "Private version");

        mockMvc.perform(get("/api/comments/{commentId}/versions", comment.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied for this ticket"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId() + "/versions"));
    }

    @Test
    public void shouldReturnNotFoundWhenListingVersionsOfNonExistingComment() throws Exception {
        mockMvc.perform(get("/api/comments/{commentId}/versions", 999999L)
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comment not found"))
                .andExpect(jsonPath("$.path").value("/api/comments/999999/versions"));
    }

    @Test
    public void shouldRejectCommentVersionListingWithUnsupportedSortBy() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.MEDIUM
        );

        TicketComment comment = dataFactory.createUpdatedComment(
                user,
                ticket,
                "Version 1",
                "Version 2"
        );

        mockMvc.perform(get("/api/comments/{commentId}/versions", comment.getId())
                        .param("sortBy", "createdAt")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only version sorting is allowed"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId() + "/versions"));
    }

    @Test
    public void shouldRejectCommentVersionListingWithInvalidPagination() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.MEDIUM
        );

        TicketComment comment = dataFactory.createUpdatedComment(
                user,
                ticket,
                "Version 1",
                "Version 2"
        );

        mockMvc.perform(get("/api/comments/{commentId}/versions", comment.getId())
                        .param("size", "0")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId() + "/versions"));
    }

    private AppUser createOtherUser() {
        return dataFactory.createUser(
                "Other User",
                "other-user-" + System.nanoTime(),
                "other123456",
                UserRole.USER
        );
    }
}