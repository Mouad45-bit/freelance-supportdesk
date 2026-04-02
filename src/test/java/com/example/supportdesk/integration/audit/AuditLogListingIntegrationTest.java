package com.example.supportdesk.integration.audit;

public class AuditLogListingIntegrationTest extends AbstractAuditLogIntegrationTest {
    ////
    public void shouldListAllAuditLogsForAdmin() throws Exception {}

    public void shouldListOnlyOwnAuditLogsForUser() throws Exception {}

    ////
    public void shouldFilterAuditLogsByActionForAdmin() throws Exception {}

    public void shouldFilterAuditLogsByResourceTypeForAdmin() throws Exception {}

    //
    public void shouldFilterAuditLogsByActorIdForAdmin() throws Exception {}

    public void shouldIgnoreForeignActorIdFilterForRegularUserAndStillReturnOnlyOwnLogs() throws Exception {}

    ////
    public void shouldRejectAuditLogsByInvalidAction() throws Exception {}

    public void shouldRejectAuditLogsByInvalidResourceType() throws Exception {}

    public void shouldFilterAuditLogsByInvalidActorId() throws Exception {}

    ////
    public void shouldRejectAuditLogListingWithoutJwt() throws Exception {}

    public void shouldRejectAuditLogListingWithNegativePage() throws Exception {}

    public void shouldRejectAuditLogListingWithInvalidSize() throws Exception {}

    public void shouldRejectAuditLogListingWithUnsupportedSortBy() throws Exception {}
}