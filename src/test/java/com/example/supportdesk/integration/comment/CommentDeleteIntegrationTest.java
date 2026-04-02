package com.example.supportdesk.integration.comment;

import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommentDeleteIntegrationTest extends AbstractCommentIntegrationTest {
    ////
    @Test
    public void shouldSoftDeleteCommentAndPreserveHistoryAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createUpdatedComment(
                user,
                ticket,
                "Version 1",
                "Version 2"
        );

        //
        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNoContent());

        //
        databaseSupport.clearPersistenceContext();

        assertionSupport.assertCommentSoftDeleted(comment.getId());
        //
        assertionSupport.assertCommentCurrentVersion(comment.getId(), 2);
        //
        assertionSupport.assertCommentVersionCount(comment.getId(), 2);
        //
        assertionSupport.assertAuditLogExists(
                AuditAction.DELETE,
                AuditResourceType.COMMENT,
                comment.getId(),
                user.getId()
        );
    }

    ////
    @Test
    public void shouldRejectCommentDeletionWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Content");

        //
        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    @Test
    public void shouldRejectCommentDeletionForAdminBecauseMutationIsUserOnly() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Content");

        //
        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    @Test
    public void shouldRejectCommentDeletionWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();
        //
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Owner content");

        //
        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your own comments"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    ////
    @Test
    public void shouldReturnNotFoundWhenDeletingNonExistingComment() throws Exception {
        mockMvc.perform(delete("/api/comments/{commentId}", 999999L)
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comment not found"))
                .andExpect(jsonPath("$.path").value("/api/comments/999999"));
    }

    @Test
    public void shouldReturnNotFoundWhenDeletingAlreadyDeletedComment() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment deletedComment = dataFactory.createDeletedComment(user, ticket, "Deleted content");

        //
        mockMvc.perform(delete("/api/comments/{commentId}", deletedComment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comment not found"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + deletedComment.getId()));
    }

    @Test
    public void shouldRejectCommentDeletionWhenParentTicketIsDeleted() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Content");

        //
        ticket.markDeleted();
        ticketRepository.save(ticket);

        //
        databaseSupport.clearPersistenceContext();

        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify or delete a comment of a deleted ticket"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }
}