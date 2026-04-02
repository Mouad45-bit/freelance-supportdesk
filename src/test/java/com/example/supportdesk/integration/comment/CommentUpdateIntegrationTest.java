package com.example.supportdesk.integration.comment;

import com.example.supportdesk.comment.dto.CommentUpdateRequest;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommentUpdateIntegrationTest extends AbstractCommentIntegrationTest {
    ////
    @Test
    public void shouldUpdateCommentAndIncreaseVersionAndPersistHistoryAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Initial content");

        //
        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Updated content"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(comment.getId()))
                .andExpect(jsonPath("$.currentVersion").value(2))
                .andExpect(jsonPath("$.content").value("Updated content"));

        //
        databaseSupport.clearPersistenceContext();

        TicketComment updated = ticketCommentRepository.findById(comment.getId()).orElseThrow();
        //
        assertThat(updated.getContent()).isEqualTo("Updated content");
        assertThat(updated.getCurrentVersion()).isEqualTo(2);

        //
        assertionSupport.assertCommentVersionCount(comment.getId(), 2);
        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.COMMENT,
                comment.getId(),
                user.getId()
        );
    }

    ////
    @Test
    public void shouldRejectCommentUpdateWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Initial content");

        //
        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Updated content"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    @Test
    public void shouldRejectCommentUpdateForAdminBecauseMutationIsUserOnly() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Initial content");

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Admin update"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    @Test
    public void shouldRejectCommentUpdateWhenRequesterIsAnotherUser() throws Exception {
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
        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Unauthorized update"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update your own comments"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    ////
    @Test
    public void shouldReturnNotFoundWhenUpdatingNonExistingComment() throws Exception {
        mockMvc.perform(patch("/api/comments/{commentId}", 999999L)
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Updated content"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comment not found"))
                .andExpect(jsonPath("$.path").value("/api/comments/999999"));
    }

    @Test
    public void shouldReturnNotFoundWhenUpdatingDeletedComment() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment deletedComment = dataFactory.createDeletedComment(user, ticket, "Deleted content");

        //
        mockMvc.perform(patch("/api/comments/{commentId}", deletedComment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Updated content"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comment not found"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + deletedComment.getId()));
    }

    @Test
    public void shouldRejectCommentUpdateWhenParentTicketIsDeleted() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Initial content");
        //
        ticket.markDeleted();
        ticketRepository.save(ticket);

        //
        databaseSupport.clearPersistenceContext();

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Updated content"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify or delete a comment of a deleted ticket"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    ////
    @Test
    public void shouldRejectCommentUpdateWhenContentDoesNotChange() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Same content");

        //
        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Same content"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Comment content is unchanged"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    //
    @Test
    public void shouldRejectCommentUpdateWithBlankContent() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Initial content");

        //
        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.content").exists())
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    @Test
    public void shouldRejectCommentUpdateWithTooLongContent() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Initial content");

        //
        String tooLongContent = "a".repeat(3001);

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest(tooLongContent))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.content").exists())
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }
}