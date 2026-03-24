package com.example.supportdesk.ticket.entity;

import com.example.supportdesk.common.entity.BaseEntity;
import com.example.supportdesk.common.enums.TicketPriority;
import com.example.supportdesk.common.enums.TicketStatus;
import com.example.supportdesk.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity @Table(name = "tickets")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseEntity {
    @Column(name = "title", nullable = false, length = 150)
    private String title;
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TicketStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TicketPriority priority;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id",  nullable = false)
    private AppUser author;
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
    @Column(name = "deleted_at")
    private Instant deletedAt;
    //
    public Ticket(String title, String description, TicketPriority priority, AppUser author) {
        this.title = title;
        this.description = description;
        this.status = TicketStatus.OPEN;
        this.priority = priority;
        this.author = author;
        this.deleted = false;
    }

    public void changeStatus(TicketStatus newStatus) {
        this.status = newStatus;
    }

    public void markDeleted() {
        this.deleted = true;
        this.deletedAt = Instant.now();
    }
}
