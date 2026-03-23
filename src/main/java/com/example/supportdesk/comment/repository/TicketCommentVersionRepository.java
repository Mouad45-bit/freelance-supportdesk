package com.example.supportdesk.comment.repository;

import com.example.supportdesk.comment.entity.TicketCommentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketCommentVersionRepository extends JpaRepository<TicketCommentVersion, Long> {
    Page<TicketCommentVersion> findByCommentGroupId(UUID commentGroupId, Pageable pageable);
}
