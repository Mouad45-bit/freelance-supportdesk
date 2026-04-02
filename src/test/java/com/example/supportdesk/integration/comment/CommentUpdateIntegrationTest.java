package com.example.supportdesk.integration.comment;

public class CommentUpdateIntegrationTest extends AbstractCommentIntegrationTest {
    ////
    public void shouldUpdateCommentAndIncreaseVersionAndPersistHistoryAndCreateAuditLog() throws Exception {}

    ////
    public void shouldRejectCommentUpdateWithoutJwt() throws Exception {}

    public void shouldRejectCommentUpdateForAdminBecauseMutationIsUserOnly() throws Exception {}

    public void shouldRejectCommentUpdateWhenRequesterIsAnotherUser() throws Exception {}

    ////
    public void shouldReturnNotFoundWhenUpdatingNonExistingComment() throws Exception {}

    public void shouldReturnNotFoundWhenUpdatingDeletedComment() throws Exception {}

    public void shouldRejectCommentUpdateWhenParentTicketIsDeleted() throws Exception {}

    ////
    public void shouldRejectCommentUpdateWhenContentDoesNotChange() throws Exception {}

    //
    public void shouldRejectCommentUpdateWithBlankContent() throws Exception {}

    public void shouldRejectCommentUpdateWithTooLongContent() throws Exception {}
}