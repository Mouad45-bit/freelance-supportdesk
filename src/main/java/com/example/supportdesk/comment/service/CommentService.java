package com.example.supportdesk.comment.service;

import com.example.supportdesk.comment.dto.CommentCreateRequest;
import com.example.supportdesk.comment.dto.CommentResponse;
import com.example.supportdesk.comment.dto.CommentUpdateRequest;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.comment.entity.TicketCommentVersion;
import com.example.supportdesk.comment.repository.TicketCommentRepository;
import com.example.supportdesk.comment.repository.TicketCommentVersionRepository;
import com.example.supportdesk.common.enums.UserRole;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import com.example.supportdesk.ticket.entity.Ticket;
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.user.entity.AppUser;
import com.example.supportdesk.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketCommentVersionRepository ticketCommentVersionRepository;
    //
    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;

    //
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public CommentResponse createComment(
            AppUserPrincipal principal,
            Long ticketId,
            CommentCreateRequest request
    ) {
        AppUser author = findUserOrThrow(principal.getId());
        Ticket ticket = findTicketOrThrow(ticketId);

        enforceTicketReadAccess(principal, ticket);

        //
        TicketComment comment = new TicketComment(
                request.content().trim(),
                author,
                ticket
        );

        TicketComment saved = ticketCommentRepository.save(comment);
        ticketCommentVersionRepository.save(TicketCommentVersion.fromCurrent(saved));

        return CommentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Page<CommentResponse> listComments(
            AppUserPrincipal principal,
            Long ticketId,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Ticket ticket = findTicketOrThrow(ticketId);
        enforceTicketReadAccess(principal, ticket);

        Pageable pageable = buildCommentsPageable(page, size, sortBy, sortDir);

        //
        return ticketCommentRepository.findByTicketId(ticketId, pageable)
                .map(CommentResponse::from);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public CommentResponse updateComment(
            AppUserPrincipal principal,
            Long commentId,
            CommentUpdateRequest request
    ) {
        TicketComment comment = findCommentOrThrow(commentId);

        if (!comment.getAuthor().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own comments");
        }
        //
        String newContent = request.content().trim();
        boolean changed = comment.updateContent(newContent);

        if (!changed) {
            return CommentResponse.from(comment);
        }

        //
        TicketComment saved = ticketCommentRepository.save(comment);
        ticketCommentVersionRepository.save(TicketCommentVersion.fromCurrent(saved));

        return CommentResponse.from(saved);
    }

    //
    private AppUser findUserOrThrow(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private TicketComment findCommentOrThrow(Long commentId) {
        return ticketCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }

    private void enforceTicketReadAccess(AppUserPrincipal principal, Ticket ticket) {
        if (principal.getRole() == UserRole.ADMIN) {
            return;
        }
        //
        if (!ticket.getAuthor().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied for this ticket");
        }
    }

    private Pageable buildCommentsPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0");
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }
        //
        String effectiveSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        if (!effectiveSortBy.equals("createdAt")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only createdAt sorting is allowed");
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        //
        return PageRequest.of(page, size, Sort.by(direction, effectiveSortBy));
    }
}
