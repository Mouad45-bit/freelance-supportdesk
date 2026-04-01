package com.example.supportdesk.integration.ticket;

import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.ticket.dto.TicketCreateRequest;
import com.example.supportdesk.ticket.entity.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TicketCreateIntegrationTest extends AbstractTicketIntegrationTest {
    ////
    @Test
    public void shouldCreateTicketAndPersistItAndCreateAuditLog() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketCreateRequest(
                                "Printer issue",
                                "Printer is not working on floor 2",
                                TicketPriority.HIGH
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Printer issue"))
                .andExpect(jsonPath("$.description").value("Printer is not working on floor 2"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.authorId").value(user.getId()))
                .andExpect(jsonPath("$.authorUsername").value(USER_USERNAME))
                .andReturn();

        //
        Long ticketId = extractId(result);

        databaseSupport.clearPersistenceContext();

        Ticket saved = ticketRepository.findById(ticketId).orElseThrow();
        //
        assertThat(saved.getTitle()).isEqualTo("Printer issue");
        assertThat(saved.getDescription()).isEqualTo("Printer is not working on floor 2");
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(saved.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(saved.getAuthor().getId()).isEqualTo(user.getId());
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getDeletedAt()).isNull();

        //
        assertionSupport.assertAuditLogExists(
                AuditAction.CREATE,
                AuditResourceType.TICKET,
                ticketId,
                user.getId()
        );
    }

    ////
    @Test
    public void shouldRejectTicketCreationWhenRequesterIsAdmin() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketCreateRequest(
                                "Admin ticket",
                                "Admin should not be allowed to create tickets",
                                TicketPriority.HIGH
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    public void shouldRejectTicketCreationWithoutJwt() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketCreateRequest(
                                "Printer issue",
                                "Printer is not working on floor 2",
                                TicketPriority.HIGH
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    //
    @Test
    public void shouldRejectTicketCreationWithBlankTitle() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketCreateRequest(
                                "  ",
                                "Printer is not working on floor 2",
                                TicketPriority.HIGH
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    public void shouldRejectTicketCreationWithOversizedTitle() throws Exception {
        String oversizedTitle = "a".repeat(151);
        //
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketCreateRequest(
                                oversizedTitle,
                                "Printer is not working on floor 2",
                                TicketPriority.HIGH
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    //
    @Test
    public void shouldRejectTicketCreationWithBlankDescription() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketCreateRequest(
                                "Printer issue",
                                "  ",
                                TicketPriority.HIGH
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    public void shouldRejectTicketCreationWithOversizedDescription() throws Exception {
        String oversizedDescription = "a".repeat(1501);

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new TicketCreateRequest(
                                "Printer issue",
                                oversizedDescription,
                                TicketPriority.HIGH
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    //
    @Test
    public void shouldRejectTicketCreationWithoutPriority() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Printer issue",
                                  "description": "Printer is not working on floor 2"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.priority").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    public void shouldRejectTicketCreationWithInvalidPriority() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearerToken(userAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Printer issue",
                                  "description": "Printer is not working on floor 2",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for priority"))
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }
}