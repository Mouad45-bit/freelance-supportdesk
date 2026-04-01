package com.example.supportdesk.integration.ticket;

public class TicketStatusUpdateIntegrationTest extends AbstractTicketIntegrationTest {
    ////
    public void shouldUpdateTicketStatusFromOpenToInProgressForAuthorAndCreateAuditLog() throws Exception {}

    public void shouldUpdateTicketStatusFromInProgressToResolvedForAuthorAndCreateAuditLog() throws Exception {}

    //
    public void shouldAllowAdminToUpdateAnotherUsersTicketStatusAndCreateAuditLog() throws Exception {}

    public void shouldAllowAdminToApplyTransitionForbiddenToRegularUserAndCreateAuditLog() throws Exception {}

    ////
    public void shouldRejectTicketStatusUpdateWithInvalidUserTransition() throws Exception {}

    public void shouldRejectTicketStatusUpdateWhenStatusDoesNotChange() throws Exception {}

    public void shouldRejectTicketStatusUpdateWithoutStatus() throws Exception {}

    public void shouldRejectTicketStatusUpdateWithInvalidStatus() throws Exception {}

    //
    public void shouldRejectTicketStatusUpdateWhenRequesterIsAnotherUser() throws Exception {}

    public void shouldRejectTicketStatusUpdateWithoutJwt() throws Exception {}

    //
    public void shouldReturnNotFoundWhenUpdatingStatusOfNonExistingTicket() throws Exception {}

    public void shouldReturnNotFoundWhenUpdatingStatusOfDeletedTicket() throws Exception {}
}