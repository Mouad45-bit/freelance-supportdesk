package com.example.supportdesk.integration.ticket;

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
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TicketIntegrationTest extends AbstractAuthenticatedIntegrationTest {
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private IntegrationAssertionSupport assertionSupport;

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

    //
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

    ////
    @Test
    public void shouldListOnlyOwnTicketsForUser() throws Exception {
        AppUser otherUser = createOtherUser();

        dataFactory.createTicket(user, "My ticket", "Mine", TicketPriority.LOW);
        dataFactory.createTicket(otherUser, "Other ticket", "Not mine", TicketPriority.HIGH);

        //
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].authorId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].authorUsername").value(USER_USERNAME));
    }

    @Test
    public void shouldListAllTicketsForAdmin() throws Exception {
        AppUser otherUser = createOtherUser();

        dataFactory.createTicket(user, "User ticket", "Mine", TicketPriority.LOW);
        dataFactory.createTicket(otherUser, "Other ticket", "Other", TicketPriority.HIGH);

        //
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    //
    @Test
    public void shouldRejectTicketListingWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    //
    @Test
    public void shouldFilterTicketsByStatus() throws Exception {
        dataFactory.createTicket(user, "Open ticket", "Open desc", TicketPriority.LOW, TicketStatus.OPEN);
        dataFactory.createTicket(user, "Resolved ticket", "Resolved desc", TicketPriority.LOW, TicketStatus.RESOLVED);
        //
        mockMvc.perform(get("/api/tickets")
                        .param("status", "RESOLVED")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$.content[0].title").value("Resolved ticket"));
    }

    @Test
    public void shouldFilterTicketsByPriority() throws Exception {
        dataFactory.createTicket(user, "Low ticket", "Low desc", TicketPriority.LOW);
        dataFactory.createTicket(user, "High ticket", "High desc", TicketPriority.HIGH);
        //
        mockMvc.perform(get("/api/tickets")
                        .param("priority", "HIGH")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.content[0].title").value("High ticket"));
    }

    @Test
    public void shouldFilterTicketsByAuthorIdForAdmin() throws Exception {
        AppUser otherUser = createOtherUser();

        dataFactory.createTicket(user, "User ticket", "Mine", TicketPriority.LOW);
        dataFactory.createTicket(otherUser, "Other ticket", "Other", TicketPriority.HIGH);

        //
        mockMvc.perform(get("/api/tickets")
                        .param("authorId", otherUser.getId().toString())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].authorId").value(otherUser.getId()))
                .andExpect(jsonPath("$.content[0].title").value("Other ticket"));
    }

    @Test
    public void shouldIgnoreAuthorIdFilterForUserAndStillReturnOnlyOwnTickets() throws Exception {
        AppUser otherUser = createOtherUser();

        dataFactory.createTicket(user, "My ticket", "Mine", TicketPriority.LOW);
        dataFactory.createTicket(otherUser, "Other ticket", "Other", TicketPriority.HIGH);

        //
        mockMvc.perform(get("/api/tickets")
                        .param("authorId", otherUser.getId().toString())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].authorId").value(user.getId()))
                .andExpect(jsonPath("$.content[0].title").value("My ticket"));
    }

    @Test
    public void shouldFilterTicketsByKeyword() throws Exception {
        dataFactory.createTicket(user, "Printer issue", "Printer broken", TicketPriority.MEDIUM);
        dataFactory.createTicket(user, "Laptop issue", "Laptop broken", TicketPriority.MEDIUM);
        //
        mockMvc.perform(get("/api/tickets")
                        .param("keyword", "printer")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Printer issue"));
    }

    //
    @Test
    public void shouldExcludeSoftDeletedTicketsFromListing() throws Exception {
        dataFactory.createTicket(user, "Active ticket", "Active desc", TicketPriority.LOW);
        dataFactory.createDeletedTicket(user, "Deleted ticket", "Deleted desc", TicketPriority.HIGH, TicketStatus.OPEN);
        //
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Active ticket"));
    }

    //
    @Test
    public void shouldReturnPagedTicketsWithValidPagination() throws Exception {
        dataFactory.createTicket(user, "Ticket 1", "Desc 1", TicketPriority.LOW);
        dataFactory.createTicket(user, "Ticket 2", "Desc 2", TicketPriority.MEDIUM);
        dataFactory.createTicket(user, "Ticket 3", "Desc 3", TicketPriority.HIGH);
        //
        mockMvc.perform(get("/api/tickets")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    //
    @Test
    public void shouldRejectTicketListingWithNegativePage() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .param("page", "-1")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page index must be >= 0"))
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    public void shouldRejectTicketListingWithInvalidSize() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .param("size", "0")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    public void shouldRejectTicketListingWithSizeGreaterThan100() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .param("size", "101")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    public void shouldRejectTicketListingWithUnsupportedSortBy() throws Exception {
        mockMvc.perform(get("/api/tickets")
                        .param("sortBy", "title")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only createdAt sorting is allowed"))
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    ////
    @Test
    public void shouldUpdateTicketStatusFromOpenToInProgressForAuthorAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.HIGH,
                TicketStatus.OPEN
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        //
        databaseSupport.clearPersistenceContext();

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        //
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );
    }

    @Test
    public void shouldUpdateTicketStatusFromInProgressToResolvedForAuthorAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.HIGH,
                TicketStatus.IN_PROGRESS
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.RESOLVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        //
        databaseSupport.clearPersistenceContext();

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        //
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.RESOLVED);

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );
    }

    //
    @Test
    public void shouldRejectTicketStatusUpdateWithInvalidUserTransition() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.HIGH,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.RESOLVED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition for USER: OPEN -> RESOLVED"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    @Test
    public void shouldRejectTicketStatusUpdateWhenStatusDoesNotChange() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.HIGH,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.OPEN))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ticket is already in status OPEN"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    @Test
    public void shouldRejectTicketStatusUpdateWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();

        Ticket ticket = dataFactory.createTicket(
                user,
                "Private status ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.OPEN
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update the status of your own tickets"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    @Test
    public void shouldRejectTicketStatusUpdateWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Status ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    @Test
    public void shouldRejectTicketStatusUpdateWithoutStatus() throws Exception {
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
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.status").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId() + "/status"));
    }

    //
    @Test
    public void shouldAllowAdminToUpdateAnotherUsersTicketStatusAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "User ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.OPEN
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        //
        databaseSupport.clearPersistenceContext();

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        //
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                admin.getId()
        );
    }

    @Test
    public void shouldAllowAdminToApplyTransitionForbiddenToRegularUserAndCreateAuditLog() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Resolved ticket",
                "Status desc",
                TicketPriority.MEDIUM,
                TicketStatus.RESOLVED
        );

        //
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.OPEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        //
        databaseSupport.clearPersistenceContext();

        Ticket updated = ticketRepository.findById(ticket.getId()).orElseThrow();
        //
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.OPEN);

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.UPDATE,
                AuditResourceType.TICKET,
                ticket.getId(),
                admin.getId()
        );
    }

    //
    @Test
    public void shouldReturnNotFoundWhenUpdatingStatusOfNonExistingTicket() throws Exception {
        mockMvc.perform(patch("/api/tickets/{ticketId}/status", 999999L)
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketStatusUpdateRequest(TicketStatus.IN_PROGRESS))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999/status"));
    }

    ////
    @Test
    public void shouldSoftDeleteTicketAndHideItFromNormalReadsAndCreateAuditLog() throws Exception {
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
        databaseSupport.clearPersistenceContext();

        assertionSupport.assertTicketSoftDeleted(ticket.getId());

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.DELETE,
                AuditResourceType.TICKET,
                ticket.getId(),
                user.getId()
        );

        //
        mockMvc.perform(get("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound());

        //
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    //
    @Test
    public void shouldRejectTicketDeletionWithoutJwt() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "Protected delete ticket",
                "Protected delete desc",
                TicketPriority.HIGH
        );

        mockMvc.perform(delete("/api/tickets/{ticketId}", ticket.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId()));
    }

    @Test
    public void shouldRejectTicketDeletionWhenRequesterIsAnotherUser() throws Exception {
        AppUser otherUser = createOtherUser();

        Ticket ticket = dataFactory.createTicket(
                user,
                "Delete protected ticket",
                "Delete desc",
                TicketPriority.HIGH
        );

        //
        mockMvc.perform(delete("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", authSupport.bearerToken(otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your own tickets"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId()));
    }

    //
    @Test
    public void shouldReturnNotFoundWhenDeletingAlreadyDeletedTicket() throws Exception {
        Ticket deletedTicket = dataFactory.createDeletedTicket(
                user,
                "Already deleted",
                "Deleted desc",
                TicketPriority.LOW,
                TicketStatus.OPEN
        );
        //
        mockMvc.perform(delete("/api/tickets/{ticketId}", deletedTicket.getId())
                        .header("Authorization", bearerToken(userAccessToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + deletedTicket.getId()));
    }

    @Test
    public void shouldRejectTicketDeletionWhenRequesterIsAdmin() throws Exception {
        Ticket ticket = dataFactory.createTicket(
                user,
                "User ticket",
                "Admin should not be allowed to delete tickets",
                TicketPriority.MEDIUM
        );
        //
        mockMvc.perform(delete("/api/tickets/{ticketId}", ticket.getId())
                        .header("Authorization", bearerToken(adminAccessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + ticket.getId()));
    }

    ////////
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