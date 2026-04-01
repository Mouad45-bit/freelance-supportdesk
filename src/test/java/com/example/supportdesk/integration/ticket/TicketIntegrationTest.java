package com.example.supportdesk.integration.ticket;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.integration.config.AbstractAuthenticatedIntegrationTest;
import com.example.supportdesk.user.entity.AppUser;
import org.junit.jupiter.api.Test;

public class TicketIntegrationTest extends AbstractAuthenticatedIntegrationTest {
    @Test
    public void shouldRejectTicketListingWithSizeGreaterThan100() throws Exception {
        //
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