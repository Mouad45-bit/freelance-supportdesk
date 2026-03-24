package com.example.supportdesk.comment.repository;

import com.example.supportdesk.comment.entity.TicketComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    Page<TicketComment> findByTicketId(Long ticketId, Pageable pageable);
    Page<TicketComment> findByTicketIdAndDeletedFalse(Long ticketId, Pageable pageable);
    Optional<TicketComment> findByIdAndDeletedFalse(Long id);
}
