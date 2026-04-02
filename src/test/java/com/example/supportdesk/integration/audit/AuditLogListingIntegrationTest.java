package com.example.supportdesk.integration.audit;

import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuditLogListingIntegrationTest extends AbstractAuditLogIntegrationTest {
    ////
    @Test
    public void shouldListAllAuditLogsForAdmin() throws Exception {
        AppUser otherUser = createOtherUser();
        //
        dataFactory.createAuditLog(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                101L,
                user,
                Map.of("title", "Ticket A")
        );
        dataFactory.createAuditLog(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                202L,
                admin,
                Map.of(
                        "field", "status",
                        "oldValue", TicketStatus.RESOLVED,
                        "newValue", TicketStatus.IN_PROGRESS
                )
        );
        dataFactory.createAuditLog(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                303L,
                otherUser,
                Map.of("softDelete", true)
        );

        //
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].id").isNumber())
                .andExpect(jsonPath("$.content[0].timestamp").isString());
    }

    @Test
    public void shouldListOnlyOwnAuditLogsForUser() throws Exception {
        AppUser otherUser = createOtherUser();
        //
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

        //
        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].actorId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].actorUsername").value(USER_USERNAME))
                .andExpect(jsonPath("$.content[0].resourceId").value(101L));
    }

    ////
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
        //
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
        //
        mockMvc.perform(get("/api/audit-logs")
                        .param("resourceType", "COMMENT")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].resourceType").value("COMMENT"))
                .andExpect(jsonPath("$.content[0].resourceId").value(202L));
    }

    //
    @Test
    public void shouldFilterAuditLogsByActorIdForAdmin() throws Exception {
        AppUser otherUser = createOtherUser();
        //
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

        //
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
    public void shouldIgnoreForeignActorIdFilterForRegularUserAndStillReturnOnlyOwnLogs() throws Exception {
        AppUser otherUser = createOtherUser();
        //
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

        //
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

    ////
    @Test
    public void shouldRejectAuditLogsByInvalidAction() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("action", "INVALID_ACTION")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for action"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    @Test
    public void shouldRejectAuditLogsByInvalidResourceType() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("resourceType", "INVALID_RESOURCE_TYPE")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for resource type"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    @Test
    public void shouldFilterAuditLogsByInvalidActorId() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("actorId", "abc")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for actor id"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    ////
    @Test
    public void shouldRejectAuditLogListingWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/audit-logs"));
    }

    @Test
    public void shouldRejectAuditLogListingWithNegativePage() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("page", "-1")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must be positive"))
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
        //
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
}