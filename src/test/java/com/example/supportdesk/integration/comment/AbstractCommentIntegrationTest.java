package com.example.supportdesk.integration.comment;

import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.comment.repository.TicketCommentRepository;
import com.example.supportdesk.integration.config.AbstractAuthenticatedIntegrationTest;
import com.example.supportdesk.integration.support.IntegrationAssertionSupport;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.user.entity.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

public abstract class AbstractCommentIntegrationTest extends AbstractAuthenticatedIntegrationTest {
    @Autowired
    protected TicketRepository ticketRepository;
    @Autowired
    protected TicketCommentRepository ticketCommentRepository;
    @Autowired
    protected IntegrationAssertionSupport assertionSupport;

    //
    protected AppUser createOtherUser() {
        return dataFactory.createUser(
                "Other User",
                "other-user-" + System.nanoTime(),
                "other123456",
                UserRole.USER
        );
    }

    protected Ticket createOwnedTicket() {
        return dataFactory.createTicket(
                user,
                "Ticket",
                "Description",
                TicketPriority.MEDIUM
        );
    }

    protected Long extractId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }
}