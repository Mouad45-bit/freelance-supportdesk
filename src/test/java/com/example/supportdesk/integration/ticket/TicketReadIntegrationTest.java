package com.example.supportdesk.integration.ticket;

public class TicketReadIntegrationTest extends AbstractTicketIntegrationTest {
    ////
    public void shouldGetTicketByIdWhenRequesterIsAuthor() throws Exception {}

    public void shouldGetTicketByIdWhenRequesterIsAdmin() throws Exception {}

    ////
    public void shouldRejectGetTicketByIdWithoutJwt() throws Exception {}

    public void shouldRejectGetTicketByIdWhenRequesterIsAnotherUser() throws Exception {}

    //
    public void shouldRejectGetTicketByIdWithoutId() throws Exception {}

    public void shouldRejectGetTicketByIdWithInvalidId() throws Exception {}

    //
    public void shouldReturnNotFoundWhenGettingNonExistingTicket() throws Exception {}

    public void shouldReturnNotFoundWhenGettingSoftDeletedTicket() throws Exception {}
}