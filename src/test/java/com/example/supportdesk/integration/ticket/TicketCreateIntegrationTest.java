package com.example.supportdesk.integration.ticket;

public class TicketCreateIntegrationTest extends AbstractTicketIntegrationTest {
    ////
    public void shouldCreateTicketAndPersistItAndCreateAuditLog() throws Exception {}

    ////
    public void shouldRejectTicketCreationWithoutJwt() throws Exception {}

    public void shouldRejectTicketCreationWhenRequesterIsAdmin() throws Exception {}

    //
    public void shouldRejectTicketCreationWithBlankTitle() throws Exception {}

    public void shouldRejectTicketCreationWithOversizedTitle() throws Exception {}

    //
    public void shouldRejectTicketCreationWithBlankDescription() throws Exception {}

    public void shouldRejectTicketCreationWithOversizedDescription() throws Exception {}

    //
    public void shouldRejectTicketCreationWithoutPriority() throws Exception {}

    public void shouldRejectTicketCreationWithInvalidPriority() throws Exception {}
}