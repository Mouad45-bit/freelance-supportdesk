package com.example.supportdesk.comment.entity;

import com.example.supportdesk.common.entity.BaseEntity;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity @Table(name = "ticket_comments")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketComment extends BaseEntity {
    @Column(name = "comment_group_id",  nullable = false, updatable = false, unique = true)
    private UUID commentGroupId;
    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;
    @Column(name = "content",  nullable = false, columnDefinition = "TEXT")
    private String content;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;
    //
    public TicketComment(String content, AppUser author, Ticket ticket) {
        this.commentGroupId = UUID.randomUUID();
        this.currentVersion = 1;
        this.content = content;
        this.author = author;
        this.ticket = ticket;
    }

    public boolean updateContent(String newContent) {
        if (this.content.equals(newContent)) {
            return false;
        }
        //
        this.content = newContent;
        this.currentVersion = this.currentVersion + 1;

        return true;
    }
}
