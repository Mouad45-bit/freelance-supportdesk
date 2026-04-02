package com.example.supportdesk.integration.comment;

public class CommentCreateIntegrationTest extends AbstractCommentIntegrationTest {
    ////
    public void shouldCreateCommentAndPersistVersionOneAndCreateAuditLog() throws Exception {}

    ////
    public void shouldRejectCommentCreationWithoutJwt() throws Exception {}

    public void shouldRejectCommentCreationWhenRequesterIsAdmin() throws Exception {}

    public void shouldRejectCommentCreationWhenRequesterIsAnotherUser() throws Exception {}

    ////
    public void shouldReturnNotFoundWhenCreatingCommentOnNonExistingTicket() throws Exception {}

    public void shouldReturnNotFoundWhenCreatingCommentOnDeletedTicket() throws Exception {}

    ////
    public void shouldRejectCommentCreationWithBlankContent() throws Exception {}

    public void shouldRejectCommentCreationWithTooLongContent() throws Exception {}
}