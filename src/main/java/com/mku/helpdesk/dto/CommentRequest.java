package com.mku.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * What the client sends to POST /api/v1/tickets/{id}/comments.
 * Same DTO works for both a student's reply and a staff member's
 * response — the service layer figures out who the author is from the
 * JWT, not from anything in this body. Never trust an authorId field
 * sent by the client; always derive identity from the validated token.
 */
@Data
public class CommentRequest {

    @NotBlank(message = "Comment cannot be empty")
    private String content;
}
