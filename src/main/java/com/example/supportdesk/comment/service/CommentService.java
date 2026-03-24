package com.example.supportdesk.comment.service;

import com.example.supportdesk.audit.service.AuditLogService;
import com.example.supportdesk.comment.dto.CommentCreateRequest;
import com.example.supportdesk.comment.dto.CommentResponse;
import com.example.supportdesk.comment.dto.CommentUpdateRequest;
import com.example.supportdesk.comment.dto.CommentVersionResponse;
import com.example.supportdesk.comment.entity.TicketComment;
import com.example.supportdesk.comment.entity.TicketCommentVersion;
import com.example.supportdesk.comment.repository.TicketCommentRepository;
import com.example.supportdesk.comment.repository.TicketCommentVersionRepository;
import com.example.supportdesk.common.enums.AuditAction;
import com.example.supportdesk.common.enums.AuditResourceType;
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

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketCommentVersionRepository ticketCommentVersionRepository;
    //
    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;
    //
    private final AuditLogService auditLogService;

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

        //
        auditLogService.log(
                AuditAction.CREATE,
                AuditResourceType.COMMENT,
                saved.getId(),
                principal.getId(),
                Map.of(
                        "ticketId", saved.getTicket().getId(),
                        "commentGroupId", saved.getCommentGroupId().toString(),
                        "version", saved.getCurrentVersion()
                )
        );

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
        return ticketCommentRepository.findByTicketIdAndDeletedFalse(ticketId, pageable)
                .map(CommentResponse::from);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public CommentResponse updateComment(
            AppUserPrincipal principal,
            Long commentId,
            CommentUpdateRequest request
    ) {
        TicketComment comment = findActiveCommentOrThrow(commentId);

        if (!comment.getAuthor().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own comments");
        }
        //
        String oldContent = comment.getContent();

        String newContent = request.content().trim();
        boolean changed = comment.updateContent(newContent);

        if (!changed) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Comment content is unchanged"
            );
        }

        //
        TicketComment saved = ticketCommentRepository.save(comment);
        ticketCommentVersionRepository.save(TicketCommentVersion.fromCurrent(saved));

        //
        auditLogService.log(
                AuditAction.UPDATE,
                AuditResourceType.COMMENT,
                saved.getId(),
                principal.getId(),
                Map.of(
                        "ticketId", saved.getTicket().getId(),
                        "commentGroupId", saved.getCommentGroupId().toString(),
                        "oldVersion", saved.getCurrentVersion() - 1,
                        "newVersion", saved.getCurrentVersion(),
                        "oldContent", oldContent,
                        "newContent", saved.getContent()
                )
        );

        return CommentResponse.from(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void deleteComment(
            AppUserPrincipal principal,
            Long commentId
    ) {
        TicketComment comment = findActiveCommentOrThrow(commentId);

        if (!comment.getAuthor().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }

        //
        auditLogService.log(
                AuditAction.DELETE,
                AuditResourceType.COMMENT,
                comment.getId(),
                principal.getId(),
                Map.of(
                        "ticketId", comment.getTicket().getId(),
                        "commentGroupId", comment.getCommentGroupId().toString(),
                        "version", comment.getCurrentVersion(),
                        "softDelete", true
                )
        );

        comment.markDeleted();
        ticketCommentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Page<CommentVersionResponse> listVersions(
            AppUserPrincipal principal,
            Long commentId,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        TicketComment comment = findCommentIncludingDeletedOrThrow(commentId);
        enforceTicketReadAccess(principal, comment.getTicket());

        Pageable pageable = buildVersionsPageable(page, size, sortBy, sortDir);

        //
        return ticketCommentVersionRepository.findByCommentGroupId(comment.getCommentGroupId(), pageable)
                .map(CommentVersionResponse::from);
    }

    //
    private AppUser findUserOrThrow(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private TicketComment findActiveCommentOrThrow(Long commentId) {
        return ticketCommentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
    }
    private TicketComment findCommentIncludingDeletedOrThrow(Long commentId) {
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

    private Pageable buildVersionsPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must be >= 0");
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }
        //
        String effectiveSortBy = (sortBy == null || sortBy.isBlank()) ? "version" : sortBy;
        if (!effectiveSortBy.equals("version")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only version sorting is allowed");
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        //
        return PageRequest.of(page, size, Sort.by(direction, effectiveSortBy));
    }
}
