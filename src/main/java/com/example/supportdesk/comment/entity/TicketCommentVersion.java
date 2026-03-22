package com.example.supportdesk.comment.entity;

import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ticket_comment_versions")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketCommentVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "comment_group_id",  nullable = false, updatable = false)
    private UUID commentGroupId;
    @Column(name = "version", nullable = false)
    private Integer version;
    @Column(name = "content",  nullable = false, columnDefinition = "TEXT")
    private String content;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    @Column(name = "snapshot_at",  nullable = false, updatable = false)
    private Instant snapshotAt;
    //
    public TicketCommentVersion(
            UUID commentGroupId,
            Integer version,
            String content,
            AppUser author,
            Ticket ticket,
            Instant snapshotAt
    ) {
        this.commentGroupId = commentGroupId;
        this.version = version;
        this.content = content;
        this.author = author;
        this.ticket = ticket;
        this.snapshotAt = snapshotAt;
    }

    public static TicketCommentVersion fromCurrent(TicketComment comment) {
        return new TicketCommentVersion(
                comment.getCommentGroupId(),
                comment.getCurrentVersion(),
                comment.getContent(),
                comment.getAuthor(),
                comment.getTicket(),
                Instant.now()
        );
    }
}
