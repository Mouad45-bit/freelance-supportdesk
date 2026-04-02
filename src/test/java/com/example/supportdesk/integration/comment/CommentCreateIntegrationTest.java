package com.example.supportdesk.integration.comment;

import com.example.supportdesk.comment.dto.CommentCreateRequest;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommentCreateIntegrationTest extends AbstractCommentIntegrationTest {
    ////
    @Test
    public void shouldCreateCommentAndPersistVersionOneAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );

        //
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

        //
        Long commentId = extractId(result);

        databaseSupport.clearPersistenceContext();

        //
        TicketComment saved = ticketCommentRepository.findById(commentId).orElseThrow();
        //
        assertThat(saved.getContent()).isEqualTo("Initial troubleshooting comment");
        assertThat(saved.getAuthor().getId()).isEqualTo(user.getId());
        assertThat(saved.getTicket().getId()).isEqualTo(ticket.getId());
        assertThat(saved.getCurrentVersion()).isEqualTo(1);
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getDeletedAt()).isNull();

        //
        assertionSupport.assertCommentCurrentVersion(commentId, 1);
        //
        assertionSupport.assertCommentVersionCount(commentId, 1);
        //
        assertionSupport.assertAuditLogExists(
                AuditAction.CREATE,
                AuditResourceType.COMMENT,
                commentId,
                user.getId()
        );
    }

    ////
    @Test
    public void shouldRejectCommentCreationWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );
        //
        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("No auth comment"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentCreationWhenRequesterIsAdmin() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );
        //
        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Admin comment"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    @Test
    public void shouldRejectCommentCreationWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();
        //
        Ticket ticket = dataFactory.createTicket(
                user,
                "Private ticket",
                "Private description",
                TicketPriority.MEDIUM
        );

        //
        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Unauthorized comment"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied for this ticket"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }

    ////
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
        //
        mockMvc.perform(post("/api/tickets/{ticketId}/comments", deletedTicket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Comment on deleted ticket"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + deletedTicket.getId() + "/comments"));
    }

    ////
    @Test
    public void shouldRejectCommentCreationWithBlankContent() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Printer issue",
                "Printer is not working",
                TicketPriority.HIGH
        );
        //
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
        //
        String tooLongContent = "a".repeat(3001);

        //
        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest(tooLongContent))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.content").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/comments"));
    }
}