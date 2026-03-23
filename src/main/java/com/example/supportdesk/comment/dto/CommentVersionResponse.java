package com.example.supportdesk.comment.dto;

import com.example.supportdesk.comment.entity.TicketCommentVersion;

import java.time.Instant;
import java.util.UUID;

public record CommentVersionResponse(
        UUID commentGroupId,
        Integer version,
        String content,
        Long authorId,
        String authorUsername,
        Long ticketId,
        Instant snapshotAt
) {
    public static CommentVersionResponse from(TicketCommentVersion version) {
        return new CommentVersionResponse(
                version.getCommentGroupId(),
                version.getVersion(),
                version.getContent(),
                version.getAuthor().getId(),
                version.getAuthor().getUsername(),
                version.getTicket().getId(),
                version.getSnapshotAt()
        );
    }
}
