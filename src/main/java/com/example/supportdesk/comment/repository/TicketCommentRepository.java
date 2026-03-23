package com.example.supportdesk.comment.repository;

import com.example.supportdesk.comment.entity.TicketComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    Page<TicketComment> findByTicketId(Long ticketId, Pageable pageable);
}
