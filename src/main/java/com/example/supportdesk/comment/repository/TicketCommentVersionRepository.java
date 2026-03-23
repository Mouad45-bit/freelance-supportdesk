package com.example.supportdesk.comment.repository;

import com.example.supportdesk.comment.entity.TicketCommentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentVersionRepository extends JpaRepository<TicketCommentVersion, Long> {
}
