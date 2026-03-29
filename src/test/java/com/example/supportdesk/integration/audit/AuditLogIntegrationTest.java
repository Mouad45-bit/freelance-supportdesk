package com.example.supportdesk.integration.audit;

import com.example.supportdesk.comment.dto.CommentCreateRequest;
import com.example.supportdesk.comment.dto.CommentUpdateRequest;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.integration.config.AbstractAuthenticatedIntegrationTest;
import com.example.supportdesk.integration.support.IntegrationAssertionSupport;
import com.example.supportdesk.ticket.dto.TicketCreateRequest;
import com.example.supportdesk.ticket.dto.TicketStatusUpdateRequest;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuditLogIntegrationTest extends AbstractAuthenticatedIntegrationTest {
    @Autowired
    private IntegrationAssertionSupport assertionSupport;

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

        Long ticketId = extractId(result);

        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);

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

        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk());

        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);

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

        mockMvc.perform(delete("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNoContent());

        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);

        assertionSupport.assertLatestAuditLog(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );
    }

    @Test
    public void shouldGenerateCreateCommentAuditLogAfterCommentCreation() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Comment ticket",
                "Comment desc",
                TicketPriority.HIGH
        );

        MvcResult result = mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentCreateRequest("Initial comment"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long commentId = extractId(result);

        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);

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

        TicketComment comment = dataFactory.createComment(user, ticket, "Version 1");

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CommentUpdateRequest("Version 2"))))
                .andExpect(status().isOk());

        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);

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

        TicketComment comment = dataFactory.createComment(user, ticket, "Comment to delete");

        mockMvc.perform(delete("/api/comments/{commentId}", comment.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNoContent());

        assertThat(databaseSupport.countAuditLogs()).isEqualTo(1);

        assertionSupport.assertLatestAuditLog(
                AuditAction.DELETE,
                AuditResourceType.COMMENT,
                comment.getId(),
                user.getId()
        );
    }

    @Test
    public void shouldListAllAuditLogsForAdmin() throws Exception {
        AppUser otherUser = createOtherUser();

        dataFactory.createAuditLog(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                101L,
                user,
                Map.of("title", "Ticket A")
        );
        dataFactory.createAuditLog(
                AuditAction.UPDATE,
                AuditResourceType.COMMENT,
                202L,
                admin,
                Map.of("field", "content")
        );
        dataFactory.createAuditLog(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                303L,
                otherUser,
                Map.of("softDelete", true)
        );

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].timestamp").isString());
    }

    @Test
    public void shouldFilterAuditLogsByActionForAdmin() throws Exception {
        dataFactory.createAuditLog(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                101L,
                user,
                Map.of("title", "Ticket A")
        );
        dataFactory.createAuditLog(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                102L,
                user,
                Map.of("softDelete", true)
        );

        mockMvc.perform(get("/api/audit-logs")
                        .param("action", "DELETE")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("DELETE"))
                .andExpect(jsonPath("$.content[0].resourceId").value(102L))
                .andExpect(jsonPath("$.content[0].actorId").value(user.getId()));
    }

    @Test
    public void shouldFilterAuditLogsByResourceTypeForAdmin() throws Exception {
        dataFactory.createAuditLog(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                101L,
                user,
                Map.of("title", "Ticket A")
        );
        dataFactory.createAuditLog(
                AuditAction.UPDATE,
                AuditResourceType.COMMENT,
                202L,
                user,
                Map.of("field", "content")
        );

        mockMvc.perform(get("/api/audit-logs")
                        .param("resourceType", "COMMENT")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].resourceType").value("COMMENT"))
                .andExpect(jsonPath("$.content[0].resourceId").value(202L));
    }

    @Test
    public void shouldFilterAuditLogsByActorIdForAdmin() throws Exception {
        AppUser otherUser = createOtherUser();

        dataFactory.createAuditLog(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                101L,
                user,
                Map.of("title", "Ticket A")
        );
        dataFactory.createAuditLog(
                AuditAction.UPDATE,
                AuditResourceType.COMMENT,
                202L,
                otherUser,
                Map.of("field", "content")
        );

        mockMvc.perform(get("/api/audit-logs")
                        .param("actorId", otherUser.getId().toString())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].actorId").value(otherUser.getId()))
                .andExpect(jsonPath("$.content[0].actorUsername").value(otherUser.getUsername()));
    }

    @Test
    public void shouldListOnlyOwnAuditLogsForUser() throws Exception {
        AppUser otherUser = createOtherUser();

        dataFactory.createAuditLog(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                101L,
                user,
                Map.of("title", "My ticket")
        );
        dataFactory.createAuditLog(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                202L,
                otherUser,
                Map.of("softDelete", true)
        );

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].actorId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].actorUsername").value(USER_USERNAME))
                .andExpect(jsonPath("$.content[0].resourceId").value(101L));
    }

    @Test
    public void shouldIgnoreForeignActorIdFilterForRegularUserAndStillReturnOnlyOwnLogs() throws Exception {
        AppUser otherUser = createOtherUser();

        dataFactory.createAuditLog(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                101L,
                user,
                Map.of("title", "My ticket")
        );
        dataFactory.createAuditLog(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                202L,
                otherUser,
                Map.of("softDelete", true)
        );

        mockMvc.perform(get("/api/audit-logs")
                        .param("actorId", otherUser.getId().toString())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].actorId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].actorUsername").value(USER_USERNAME))
                .andExpect(jsonPath("$.content[0].resourceId").value(101L));
    }

    @Test
    public void shouldRejectAuditLogListingWithNegativePage() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("page", "-1")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must be >= 0"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    @Test
    public void shouldRejectAuditLogListingWithInvalidSize() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("size", "0")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    @Test
    public void shouldRejectAuditLogListingWithSizeGreaterThan100() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("size", "101")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    @Test
    public void shouldRejectAuditLogListingWithUnsupportedSortBy() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("sortBy", "actorId")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only timestamp sorting is allowed"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    @Test
    public void shouldRejectAuditLogListingWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
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