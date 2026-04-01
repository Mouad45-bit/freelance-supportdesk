package com.example.supportdesk.integration.ticket;

public class TicketListIntegrationTest extends AbstractTicketIntegrationTest {
    ////
    public void shouldListOnlyOwnTicketsForUser() throws Exception {}

    public void shouldListAllTicketsForAdmin() throws Exception {}

    ////
    public void shouldExcludeSoftDeletedTicketsFromListing() throws Exception {}

    public void shouldRejectTicketListingWithoutJwt() throws Exception {}

    ////
    public void shouldFilterTicketsByStatus() throws Exception {}

    public void shouldFilterTicketsByPriority() throws Exception {}

    public void shouldFilterTicketsByKeyword() throws Exception {}

    //
    public void shouldFilterTicketsByAuthorIdForAdmin() throws Exception {}

    public void shouldIgnoreAuthorIdFilterForUserAndStillReturnOnlyOwnTickets() throws Exception {}

    //
    public void shouldRejectFilterTicketsWithInvalidParam() throws Exception {}

    ////
    public void shouldReturnPagedTicketsWithValidPagination() throws Exception {}

    //
    public void shouldRejectTicketListingWithNegativePage() throws Exception {}

    public void shouldRejectTicketListingWithInvalidSize() throws Exception {}

    public void shouldRejectTicketListingWithUnsupportedSortBy() throws Exception {}
}