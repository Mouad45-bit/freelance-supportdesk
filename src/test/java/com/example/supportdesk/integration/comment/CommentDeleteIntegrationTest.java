package com.example.supportdesk.integration.comment;

public class CommentDeleteIntegrationTest extends AbstractCommentIntegrationTest {
    ////
    public void shouldSoftDeleteCommentAndPreserveHistoryAndCreateAuditLog() throws Exception {}

    ////
    public void shouldRejectCommentDeletionWithoutJwt() throws Exception {}

    public void shouldRejectCommentDeletionForAdminBecauseMutationIsUserOnly() throws Exception {}

    public void shouldRejectCommentDeletionWhenRequesterIsAnotherUser() throws Exception {}

    ////
    public void shouldReturnNotFoundWhenDeletingNonExistingComment() throws Exception {}

    public void shouldReturnNotFoundWhenDeletingAlreadyDeletedComment() throws Exception {}

    public void shouldRejectCommentDeletionWhenParentTicketIsDeleted() throws Exception {}
}