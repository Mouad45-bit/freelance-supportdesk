package com.example.supportdesk.comment.dto;

import com.example.supportdesk.comment.entity.TicketComment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        Long id,
        UUID commentGroupId,
        Integer currentVersion,
        String content,
        Long authorId,
        String authorUsername,
        Long ticketId,
        boolean deleted,
        Instant deletedAt,
        Instant createdAt,
        Instant updatedAt

) {
    public static CommentResponse from(TicketComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getCommentGroupId(),
                comment.getCurrentVersion(),
                comment.getContent(),
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getTicket().getId(),
                comment.isDeleted(),
                comment.getDeletedAt(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
