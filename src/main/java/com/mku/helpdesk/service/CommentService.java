package com.mku.helpdesk.service;

import com.mku.helpdesk.dto.CommentRequest;
import com.mku.helpdesk.dto.CommentResponse;
import com.mku.helpdesk.exception.ResourceNotFoundException;
import com.mku.helpdesk.model.Comment;
import com.mku.helpdesk.model.Role;
import com.mku.helpdesk.model.Status;
import com.mku.helpdesk.model.Ticket;
import com.mku.helpdesk.model.User;
import com.mku.helpdesk.repository.CommentRepository;
import com.mku.helpdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One service backs BOTH the student-reply endpoint and the staff-response
 * endpoint — the only difference between them is which `author` gets
 * passed in, which the controller derives from the JWT via
 * @AuthenticationPrincipal. Never trust an authorId in the request body;
 * identity always comes from the validated token, never from client input.
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest request, User author) {
        Ticket ticket = getTicketOrThrow(ticketId);

        // A student may only comment on their OWN ticket. Staff/admin can
        // comment on any ticket — that's enforced structurally by which
        // URL prefix they're allowed to reach (SecurityConfig), so we only
        // need the ownership check for the STUDENT role here.
        if (author.getRole() == Role.STUDENT && !ticket.getStudent().getId().equals(author.getId())) {
            throw new AccessDeniedException("You do not have permission to comment on this ticket.");
        }

        if (ticket.getStatus() == Status.CLOSED) {
            throw new IllegalStateException("This ticket is closed and can no longer receive replies.");
        }

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);

        return toResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getThread(Long ticketId, User requester) {
        Ticket ticket = getTicketOrThrow(ticketId);

        if (requester.getRole() == Role.STUDENT && !ticket.getStudent().getId().equals(requester.getId())) {
            throw new AccessDeniedException("You do not have permission to view this conversation.");
        }

        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Ticket getTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket #" + id + " not found."));
    }

    private CommentResponse toResponse(Comment c) {
        return new CommentResponse(
                c.getId(),
                c.getContent(),
                c.getAuthor().getFullName(),
                c.getAuthor().getRole().name(),
                c.getCreatedAt()
        );
    }
}
