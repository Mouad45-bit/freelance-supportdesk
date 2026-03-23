package com.example.supportdesk.comment.controller;

import com.example.supportdesk.comment.dto.CommentCreateRequest;
import com.example.supportdesk.comment.dto.CommentResponse;
import com.example.supportdesk.comment.dto.CommentUpdateRequest;
import com.example.supportdesk.comment.dto.CommentVersionResponse;
import com.example.supportdesk.comment.service.CommentService;
import com.example.supportdesk.security.principal.AppUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    //
    @PostMapping("/tickets/{ticketId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long ticketId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        CommentResponse response = commentService.createComment(principal, ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tickets/{ticketId}/comments")
    public Page<CommentResponse> listComments(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long ticketId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return commentService.listComments(principal, ticketId, page, size, sortBy, sortDir);
    }

    @PatchMapping("/comments/{commentId}")
    public CommentResponse updateComment(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return commentService.updateComment(principal, commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(principal, commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/comments/{commentId}/versions")
    public Page<CommentVersionResponse> listVersions(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "version") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        return commentService.listVersions(principal, commentId, page, size, sortBy, sortDir);
    }
}
