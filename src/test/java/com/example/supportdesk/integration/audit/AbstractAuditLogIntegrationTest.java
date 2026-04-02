package com.example.supportdesk.integration.audit;

import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.integration.config.AbstractAuthenticatedIntegrationTest;
import com.example.supportdesk.integration.support.IntegrationAssertionSupport;
import com.example.supportdesk.user.entity.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

public abstract class AbstractAuditLogIntegrationTest extends AbstractAuthenticatedIntegrationTest {
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

    protected Long extractId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }
}