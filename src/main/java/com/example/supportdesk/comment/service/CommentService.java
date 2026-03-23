package com.example.supportdesk.comment.service;

import com.example.supportdesk.comment.dto.CommentCreateRequest;
import com.example.supportdesk.comment.dto.CommentResponse;
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

    //
    private AppUser findUserOrThrow(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Ticket findTicketOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
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
}
