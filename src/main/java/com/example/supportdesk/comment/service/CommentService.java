package com.example.supportdesk.comment.service;

import com.example.supportdesk.comment.repository.TicketCommentRepository;
import com.example.supportdesk.comment.repository.TicketCommentVersionRepository;
import com.example.supportdesk.ticket.repository.TicketRepository;
import com.example.supportdesk.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketCommentVersionRepository ticketCommentVersionRepository;
    //
    private final TicketRepository ticketRepository;
    private final AppUserRepository appUserRepository;

    //
}
