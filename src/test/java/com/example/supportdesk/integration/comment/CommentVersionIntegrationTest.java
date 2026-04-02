package com.example.supportdesk.integration.comment;

public class CommentVersionIntegrationTest extends AbstractCommentIntegrationTest {
    ////
    public void shouldListCommentVersionsForAuthorOrAdmin() throws Exception {}

    public void shouldListCommentVersionsForDeletedComment() throws Exception {}

    ////
    public void shouldRejectCommentVersionListingWithoutJwt() throws Exception {}

    public void shouldRejectCommentVersionListingWhenRequesterIsAnotherUser() throws Exception {}

    ////
    public void shouldReturnNotFoundWhenListingVersionsOfNonExistingComment() throws Exception {}

    public void shouldRejectCommentVersionListingWithNegativePage() throws Exception {}

    public void shouldRejectCommentVersionListingWithInvalidSize() throws Exception {}

    public void shouldRejectCommentVersionListingWithUnsupportedSortBy() throws Exception {}
}