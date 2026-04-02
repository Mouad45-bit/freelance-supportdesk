package com.example.supportdesk.integration.audit;

import com.example.supportdesk.comment.dto.CommentCreateRequest;
import com.example.supportdesk.comment.dto.CommentUpdateRequest;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.ticket.dto.TicketCreateRequest;
import com.example.supportdesk.ticket.dto.TicketStatusUpdateRequest;
import com.example.supportdesk.ticket.entity.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuditLogGenerationIntegrationTest extends AbstractAuditLogIntegrationTest {
    ////
    @Test
    public void shouldGenerateCreateTicketAuditLogAfterTicketCreation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketCreateRequest(
                                "Printer issue",
                                "Printer is not working on floor 2",
                                TicketPriority.HIGH
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        //
        Long ticketId = extractId(result);

        //
        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);
        //
        assertionSupport.assertLatestAuditLog(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                ticketId,
                user.getId()
        );
    }

    @Test
    public void shouldGenerateUpdateTicketAuditLogAfterTicketStatusUpdate() throws Exception {
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
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk());

        //
        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);
        //
        assertionSupport.assertLatestAuditLog(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );
    }

    @Test
    public void shouldGenerateDeleteTicketAuditLogAfterTicketDeletion() throws Exception {
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
        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);
        //
        assertionSupport.assertLatestAuditLog(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );
    }

    ////
    @Test
    public void shouldGenerateCreateCommentAuditLogAfterCommentCreation() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Comment ticket",
                "Comment desc",
                TicketPriority.HIGH
        );
        //
        MvcResult result = mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Initial comment"))))
                .andExpect(status().isCreated())
                .andReturn();

        //
        Long commentId = extractId(result);

        //
        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);
        //
        assertionSupport.assertLatestAuditLog(
                AuditAction.CREATE,
                AuditResourceType.COMMENT,
                commentId,
                user.getId()
        );
    }

    @Test
    public void shouldGenerateUpdateCommentAuditLogAfterCommentUpdate() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Comment ticket",
                "Comment desc",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Version 1");
        //
        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Version 2"))))
                .andExpect(status().isOk());

        //
        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);
        //
        assertionSupport.assertLatestAuditLog(
                AuditAction.UPDATE,
                AuditResourceType.COMMENT,
                comment.getId(),
                user.getId()
        );
    }

    @Test
    public void shouldGenerateDeleteCommentAuditLogAfterCommentDeletion() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Comment ticket",
                "Comment desc",
                TicketPriority.HIGH
        );
        //
        TicketComment comment = dataFactory.createComment(user, ticket, "Comment to delete");
        //
        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNoContent());

        //
        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);
        //
        assertionSupport.assertLatestAuditLog(
                AuditAction.DELETE,
                AuditResourceType.COMMENT,
                comment.getId(),
                user.getId()
        );
    }
}