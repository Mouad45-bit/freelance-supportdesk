package com.example.supportdesk.integration.comment;

import com.example.supportdesk.comment.dto.CommentCreateRequest;
import com.example.supportdesk.comment.dto.CommentUpdateRequest;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.comment.repository.TicketCommentRepository;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.integration.config.AbstractAuthenticatedIntegrationTest;
import com.example.supportdesk.integration.support.IntegrationAssertionSupport;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CommentIntegrationTest extends AbstractAuthenticatedIntegrationTest {
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private TicketCommentRepository ticketCommentRepository;
    @Autowired
    private IntegrationAssertionSupport assertionSupport;

    ////
    @Test
    public void shouldCreateCommentAndPersistVersionOneAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );

        MvcResult result = mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Initial troubleshooting comment"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.commentGroupId").isString())
                .andExpect(jsonPath("$.currentVersion").value(1))
                .andExpect(jsonPath("$.content").value("Initial troubleshooting comment"))
                .andExpect(jsonPath("$.authorId").value(user.getId()))
                .andExpect(jsonPath("$.authorUsername").value(USER_USERNAME))
                .andExpect(jsonPath("$.ticketId").value(ticket.getId()))
                .andExpect(jsonPath("$.deleted").value(false))
                .andReturn();

        Long commentId = extractId(result);

        databaseSupport.clearPersistenceContext();

        TicketComment saved = ticketCommentRepository.findById(commentId).orElseThrow();

        assertThat(saved.getContent()).isEqualTo("Initial troubleshooting comment");
        assertThat(saved.getAuthor().getId()).isEqualTo(user.getId());
        assertThat(saved.getTicket().getId()).isEqualTo(ticket.getId());
        assertThat(saved.getCurrentVersion()).isEqualTo(1);
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getDeletedAt()).isNull();

        assertionSupport.assertCommentCurrentVersion(commentId, 1);
        assertionSupport.assertCommentVersionCount(commentId, 1);
        assertionSupport.assertAuditLogExists(
                AuditAction.CREATE,
                AuditResourceType.COMMENT,
                commentId,
                user.getId()
        );
    }

    @Test
    public void shouldRejectCommentCreationWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();

        Ticket ticket = dataFactory.createTicket(
                user,
                "Private ticket",
                "Private description",
                TicketPriority.MEDIUM
        );

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Unauthorized comment"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied for this ticket"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldReturnNotFoundWhenCreatingCommentOnNonExistingTicket() throws Exception {
        mockMvc.perform(post("/api/tickets/{ticketId}/comments", 999999L)
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Comment on missing ticket"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999/comments"));
    }

    @Test
    public void shouldReturnNotFoundWhenCreatingCommentOnDeletedTicket() throws Exception {
        Ticket deletedTicket = dataFactory.createDeletedTicket(
                user,
                "Deleted ticket",
                "Deleted description",
                TicketPriority.LOW,
                TicketStatus.OPEN
        );

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", deletedTicket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Comment on deleted ticket"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + deletedTicket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentCreationWithBlankContent() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.content").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentCreationWithTooLongContent() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );

        String tooLongContent = "a".repeat(3001);

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest(tooLongContent))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.content").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentCreationWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("No auth comment"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentCreationForAdmin() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Admin comment"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

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
    public void shouldRejectCommentListingWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();

        Ticket ticket = dataFactory.createTicket(
                user,
                "Private ticket",
                "Private description",
                TicketPriority.MEDIUM
        );

        dataFactory.createComment(user, ticket, "Private comment");

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied for this ticket"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldReturnNotFoundWhenListingCommentsOfNonExistingTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", 999999L)
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999/comments"));
    }

    @Test
    public void shouldRejectCommentListingWithNegativePage() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.LOW
        );

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .param("page", "-1")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must be >= 0"))
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

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .param("size", "0")
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

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId())
                        .param("sortBy", "content")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only createdAt sorting is allowed"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldUpdateCommentAndIncreaseVersionAndPersistHistoryAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );

        TicketComment comment = dataFactory.createComment(user, ticket, "Initial content");

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Updated content"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(comment.getId()))
                .andExpect(jsonPath("$.currentVersion").value(2))
                .andExpect(jsonPath("$.content").value("Updated content"));

        databaseSupport.clearPersistenceContext();

        TicketComment updated = ticketCommentRepository.findById(comment.getId()).orElseThrow();

        assertThat(updated.getContent()).isEqualTo("Updated content");
        assertThat(updated.getCurrentVersion()).isEqualTo(2);

        assertionSupport.assertCommentCurrentVersion(comment.getId(), 2);
        assertionSupport.assertCommentVersionCount(comment.getId(), 2);
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.COMMENT,
                comment.getId(),
                user.getId()
        );
    }

    @Test
    public void shouldRejectCommentUpdateWhenContentDoesNotChange() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );

        TicketComment comment = dataFactory.createComment(user, ticket, "Same content");

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Same content"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Comment content is unchanged"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

    @Test
    public void shouldRejectCommentUpdateWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();

        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );

        TicketComment comment = dataFactory.createComment(user, ticket, "Owner content");

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Unauthorized update"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update your own comments"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

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

        TicketComment deletedComment = dataFactory.createDeletedComment(user, ticket, "Deleted content");

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

        TicketComment comment = dataFactory.createComment(user, ticket, "Initial content");

        ticket.markDeleted();
        ticketRepository.save(ticket);

        databaseSupport.clearPersistenceContext();

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Updated content"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify or delete a comment of a deleted ticket"))
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
    public void shouldSoftDeleteCommentAndPreserveHistoryAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );

        TicketComment comment = dataFactory.createUpdatedComment(
                user,
                ticket,
                "Version 1",
                "Version 2"
        );

        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNoContent());

        databaseSupport.clearPersistenceContext();

        assertionSupport.assertCommentSoftDeleted(comment.getId());
        assertionSupport.assertCommentCurrentVersion(comment.getId(), 2);
        assertionSupport.assertCommentVersionCount(comment.getId(), 2);
        assertionSupport.assertAuditLogExists(
                AuditAction.DELETE,
                AuditResourceType.COMMENT,
                comment.getId(),
                user.getId()
        );
    }

    @Test
    public void shouldRejectCommentDeletionWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();

        Ticket ticket = dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.HIGH
        );

        TicketComment comment = dataFactory.createComment(user, ticket, "Owner content");

        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your own comments"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
    }

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

        TicketComment deletedComment = dataFactory.createDeletedComment(user, ticket, "Deleted content");

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

        TicketComment comment = dataFactory.createComment(user, ticket, "Content");

        ticket.markDeleted();
        ticketRepository.save(ticket);

        databaseSupport.clearPersistenceContext();

        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot modify or delete a comment of a deleted ticket"))
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

        TicketComment comment = dataFactory.createComment(user, ticket, "Content");

        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/comments/" + comment.getId()));
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

    private Long extractId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }
}