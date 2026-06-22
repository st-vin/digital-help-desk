package com.mku.helpdesk.repository;

import com.mku.helpdesk.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Powers the conversation thread on the ticket detail page —
    // oldest first, so it reads top-to-bottom like a chat.
    List<Comment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
