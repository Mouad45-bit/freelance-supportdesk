package com.example.supportdesk.comment.repository;

import com.example.supportdesk.comment.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
}
