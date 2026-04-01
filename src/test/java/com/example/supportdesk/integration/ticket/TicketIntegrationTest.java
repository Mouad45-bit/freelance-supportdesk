package com.example.supportdesk.integration.ticket;

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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    ////////
    private AppUser createOtherUser() {
        return dataFactory.createUser(
                "Other User",
                "other-user-" + System.nanoTime(),
                "other123456",
                UserRole.USER
        );
    }
}